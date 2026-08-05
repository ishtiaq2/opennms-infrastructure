# OpenNMS Alarm Lifecycle — Guide & Hands-On Simulation Tutorial (Podman Edition)

**Setup:** OpenNMS, PostgreSQL, and snmpsim each as their own **Podman** containers · a separate **python-lab** container running all simulation scripts · everything wired together on a user-defined Podman network so it behaves like a real segmented network.
**Audience:** New to OpenNMS — every step explains the "why," not just the "how"

---

## How this document is organized

- **Part A — The Guide.** Concepts before touching a keyboard: what OpenNMS is, how it stores things, what "alarm lifecycle" means end to end. (Unchanged by the infrastructure choice — read this once.)
- **Part B — The Tutorial.** A from-scratch Podman build: containerized OpenNMS + PostgreSQL + snmpsim "devices," and a python-lab container whose scripts walk an alarm through every stage of its life while you watch it happen in the UI and in the database.
- **Part C — Reference & troubleshooting**, including Podman-specific gotchas (rootless networking, low ports, SELinux) that don't come up in a VM-based lab.

---

# PART A — THE GUIDE

## A1. What OpenNMS is, in one paragraph

OpenNMS (Horizon) is a network monitoring platform. It does three jobs: **discover/track infrastructure** (provisioning), **watch it** (polling, data collection, listening for traps/syslog), and **tell you when something's wrong** (events → alarms → notifications). Everything OpenNMS knows lives in a PostgreSQL database — every node, every event, every alarm is a row in a table. That's true whether OpenNMS runs on a VM or in a container; only *how you reach it* changes.

## A2. The four core concepts, and how they relate

```
Requisition  --(import)-->  Node  --(monitored by)-->  Services/Interfaces
                                         |
                                         v
                              Trap / Syslog / REST input
                                         |
                                         v
                                     Event  (raw, immutable log entry)
                                         |
                              (matches eventconf.xml rule with <alarm-data>)
                                         v
                                     Alarm  (mutable, has a lifecycle & a count)
```

**Requisition** — a declarative XML "shopping list" of nodes: hostnames, IPs, SNMP community, categories, services. You don't create nodes directly; you create a requisition and *import* it.

**Node** — the database record created after import. A node has interfaces (IPs), and each interface has services (SNMP, HTTP, ICMP...) that get polled.

**Event** — an immutable fact: "something happened." Every trap, every matched syslog line, every internal state change becomes a row in `events`. Events are a permanent audit log — never edited or merged.

**Alarm** — a *mutable, deduplicated* representation of a problem. Only events whose definition includes `<alarm-data>` produce one. OpenNMS computes a **reduction key** from the event (UEI + node/interface/service). If an alarm with that key already exists, the counter increments and last-seen time updates instead of creating a duplicate.

## A3. The alarm lifecycle, precisely

| State | Meaning | How you get there |
|---|---|---|
| **New / Uncleared** | Problem is active, nobody has acted on it | An `<alarm-data>` event fires, no matching alarm exists yet |
| **Escalated** | Same problem, severity raised | Repeated occurrences, or manual severity change |
| **Acknowledged** | A human said "I've seen this" | UI Ack button, or `PUT /rest/alarms/{id}` with `ack=true` |
| **Cleared** | Underlying problem resolved | A different event's `<alarm-data>` specifies a `<clear-key>` matching this alarm's reduction key |
| **Re-opened** | New occurrence after clearing | Fresh event re-triggers the same reduction key |
| **Purged** | Old cleared alarms garbage-collected | Alarmd's scheduled cleanup |

Two mechanisms drive this automatically: **reduction keys** (dedupe repeats into one row) and **clear keys** (let a "resolved" event auto-clear the alarm from the matching "problem" event, e.g. `linkUp` clearing `linkDown`).

## A4. Where everything lives in the database

| Table | Holds |
|---|---|
| `node` | One row per imported node |
| `ipinterface` | IP addresses per node |
| `snmpinterface` | SNMP-discovered interface details |
| `service` / `ifservices` | Which services are monitored where |
| `events` | Every event ever received — immutable |
| `alarms` | Current alarm state — `counter`, `severity_id`, `alarmacktime`, `clearkey`, `reductionkey`, `firsteventtime`, `lasteventtime` |
| `alarm_attributes` | Extra metadata on an alarm |
| `category_node` | Node-to-category mapping |

## A5. The three ways problems get in (what you'll simulate)

1. **SNMP traps** (UDP/162) — Trapd converts them to events via MIB definitions.
2. **Syslog** (UDP/514) — Syslogd matches text against regex patterns to produce events.
3. **REST-injected synthetic events** — POST event XML directly to `/rest/events`, bypassing traps/syslog. Used for app-level or hardware-agnostic conditions.

---

# PART B — THE TUTORIAL (Podman)

## B0. What you'll build

```
                     Podman user-defined network: labnet (172.28.0.0/24)

┌────────────────┐   ┌────────────────┐   ┌───────────────────────────────┐
│  postgres        │   │  opennms         │   │  snmpsim-switch1 :161          │
│  172.28.0.5      │◄──┤  172.28.0.10     │◄──┤  snmpsim-router1 :161          │
│  (data volume)   │   │  :8980 web/REST  │   │  snmpsim-appserver1 :161       │
└────────────────┘   │  :162/udp trapd  │   │  172.28.0.11 / .12 / .13       │
                       │  :514/udp syslogd│   └───────────────────────────────┘
                       └────────────────┘
                                ▲
                                │ traps / syslog / REST calls
                       ┌────────────────┐
                       │  python-lab      │
                       │  172.28.0.20     │
                       │  all sim scripts │
                       └────────────────┘
```

Every component is its own container, each with a **static IP on a shared network** — this is what makes it feel like real, separate boxes on a real subnet rather than processes sharing localhost, and it's exactly what makes SNMP source-IP and syslog-hostname matching in OpenNMS behave the way it would against real gear.

## B1. Podman host prerequisites

Install Podman and `podman-compose` on your host (a Linux VM or bare-metal box — Podman itself doesn't need to be inside a VM, but using one keeps this fully isolated from your regular machine):

```bash
# Debian/Ubuntu
sudo apt update && sudo apt install -y podman
pip install --break-system-packages podman-compose

# Fedora/RHEL
sudo dnf install -y podman podman-compose
```

Verify:

```bash
podman --version
podman-compose --version
```

### Rootless vs. rootful — read this before you start

Podman defaults to **rootless** (containers run as your user, not root). That's the safer default and works fine for almost everything here, but two things in this lab specifically care about it:

1. **Low ports (162, 514).** Rootless Podman can still *publish* container ports below 1024 to the host, because the port mapping happens through the rootless network stack (`slirp4netns`/`pasta`) in userspace rather than needing `CAP_NET_BIND_SERVICE` on the host. It generally works out of the box — but if publishing `162:162/udp` or `514:514/udp` fails for you, either:
   - Lower the host's unprivileged port floor: `sudo sysctl net.ipv4.ip_unprivileged_port_start=0`, or
   - Run this lab's containers rootful (`sudo podman-compose up`) — simplest fix, acceptable for a throwaway lab.
2. **Static IPs on a custom network** need Podman's `netavark` network backend (default on Podman 4.x+). Check with `podman info --format '{{.Host.NetworkBackend}}'` — if it says `cni` instead of `netavark`, upgrade Podman or static IP assignment in the compose file below won't take effect.

### SELinux hosts (Fedora/RHEL/CentOS)

Any bind-mounted volume needs a `:Z` (or `:z` for shared) suffix or the container won't be able to read/write it. The compose file below already includes this — keep it if you're on an SELinux-enforcing host, harmless if you're not.

## B2. Project layout

```
opennms-lab/
├── compose.yaml
├── snmpsim/
│   ├── Containerfile
│   └── data/
│       ├── switch1/switch1.snmprec
│       ├── router1/router1.snmprec
│       └── appserver1/appserver1.snmprec
├── python-lab/
│   ├── Containerfile
│   └── scripts/
│       ├── provision.py
│       ├── trap_sender.py
│       ├── syslog_sender.py
│       ├── fire_disk_full.py
│       ├── fire_disk_ok.py
│       └── run_incident_simulation.py
└── opennms-config/
    └── lab-custom-events.xml
```

```bash
mkdir -p opennms-lab/{snmpsim/data/switch1,snmpsim/data/router1,snmpsim/data/appserver1,python-lab/scripts,opennms-config}
cd opennms-lab
```

## B3. The snmpsim container (fake SNMP devices)

`snmpsim` serves canned SNMP data over UDP so OpenNMS's discovery/provisioning/polling treats these containers exactly like real hardware. We'll run **three separate containers**, one per simulated device, each on its own IP — closer to reality than one process on three ports.

**`snmpsim/Containerfile`:**

```dockerfile
FROM docker.io/library/python:3.11-slim
RUN pip install --no-cache-dir snmpsim-lextudio
WORKDIR /data
EXPOSE 161/udp
ENTRYPOINT ["snmpsim-command-responder"]
```

**`snmpsim/data/switch1/switch1.snmprec`:**

```
1.3.6.1.2.1.1.1.0|4|Simulated Core Switch - Lab Device 1
1.3.6.1.2.1.1.3.0|67|12345678
1.3.6.1.2.1.1.5.0|4|switch1-lab
1.3.6.1.2.1.2.1.0|2|4
1.3.6.1.2.1.2.2.1.7.1|2|1
1.3.6.1.2.1.2.2.1.8.1|2|1
```

Create similar `.snmprec` files for `router1` and `appserver1` (change the `sysDescr`/`sysName` lines to match).

## B4. The python-lab container (all simulation scripts)

**`python-lab/Containerfile`:**

```dockerfile
FROM docker.io/library/python:3.11-slim
RUN pip install --no-cache-dir pysnmp requests
WORKDIR /scripts
ENTRYPOINT ["sleep", "infinity"]
```

Keeping this container idle (`sleep infinity`) and running scripts inside it interactively (`podman exec`) mirrors how you'd operate a real jump-box/tooling host on a real network — you don't rebuild the image every time you want to run a different script.

## B5. The compose file — everything wired together

**`compose.yaml`:**

```yaml
version: "3.8"

networks:
  labnet:
    driver: bridge
    ipam:
      config:
        - subnet: 172.28.0.0/24

volumes:
  pgdata:
  opennms-data:

services:

  postgres:
    image: docker.io/library/postgres:15
    container_name: opennms-postgres
    environment:
      POSTGRES_USER: opennms
      POSTGRES_PASSWORD: opennms
      POSTGRES_DB: opennms
    volumes:
      - pgdata:/var/lib/postgresql/data:Z
    networks:
      labnet:
        ipv4_address: 172.28.0.5

  opennms:
    # Check docs.opennms.com's "Deploy with Container" page for the current
    # official image name/tag and required env vars before you pull —
    # these details change between releases.
    image: docker.io/opennms/horizon:latest
    container_name: opennms
    depends_on:
      - postgres
    environment:
      POSTGRES_HOST: opennms-postgres
      POSTGRES_PORT: "5432"
      POSTGRES_USER: opennms
      POSTGRES_PASSWORD: opennms
      POSTGRES_DB: opennms
    ports:
      - "8980:8980/tcp"
      - "162:162/udp"
      - "514:514/udp"
    volumes:
      - opennms-data:/opt/opennms/data:Z
      - ./opennms-config/lab-custom-events.xml:/opt/opennms/etc/events/lab-custom-events.xml:Z
    networks:
      labnet:
        ipv4_address: 172.28.0.10

  snmpsim-switch1:
    build: ./snmpsim
    container_name: snmpsim-switch1
    command: ["--data-dir=/data", "--agent-udpv4-endpoint=0.0.0.0:161"]
    volumes:
      - ./snmpsim/data/switch1:/data:Z
    networks:
      labnet:
        ipv4_address: 172.28.0.11

  snmpsim-router1:
    build: ./snmpsim
    container_name: snmpsim-router1
    command: ["--data-dir=/data", "--agent-udpv4-endpoint=0.0.0.0:161"]
    volumes:
      - ./snmpsim/data/router1:/data:Z
    networks:
      labnet:
        ipv4_address: 172.28.0.12

  snmpsim-appserver1:
    build: ./snmpsim
    container_name: snmpsim-appserver1
    command: ["--data-dir=/data", "--agent-udpv4-endpoint=0.0.0.0:161"]
    volumes:
      - ./snmpsim/data/appserver1:/data:Z
    networks:
      labnet:
        ipv4_address: 172.28.0.13

  python-lab:
    build: ./python-lab
    container_name: python-lab
    volumes:
      - ./python-lab/scripts:/scripts:Z
    networks:
      labnet:
        ipv4_address: 172.28.0.20
```

Bring it all up:

```bash
podman-compose up -d --build
podman-compose ps
```

Give OpenNMS 1–3 minutes on first boot (it initializes the schema in PostgreSQL). Watch it:

```bash
podman logs -f opennms
```

Once you see it listening, the UI is at `http://localhost:8980/opennms` (default `admin`/`admin` — **change immediately**, Settings → Users).

**Container-to-container addressing:** because everything is on the `labnet` network, containers reach each other by **service/container name** via Podman's built-in DNS — `opennms`, `opennms-postgres`, `snmpsim-switch1`, etc. all resolve automatically from inside `python-lab`. That's what the scripts below target instead of a VM IP.

## B6. Provisioning: build the requisition and import it

### B6.1 The requisition XML

Node interface IPs point at the snmpsim containers' static IPs — those are your "devices."

```xml
<?xml version="1.0" encoding="UTF-8"?>
<model-import xmlns="http://xmlns.opennms.org/xsd/config/model-import"
               foreign-source="lab-network">
  <node foreign-id="1" node-label="switch1-lab">
    <interface ip-addr="172.28.0.11" status="1" snmp-primary="P">
      <monitored-service service-name="ICMP"/>
      <monitored-service service-name="SNMP"/>
    </interface>
    <category name="Switches"/>
  </node>
  <node foreign-id="2" node-label="router1-lab">
    <interface ip-addr="172.28.0.12" status="1" snmp-primary="P">
      <monitored-service service-name="ICMP"/>
      <monitored-service service-name="SNMP"/>
    </interface>
    <category name="Routers"/>
  </node>
  <node foreign-id="3" node-label="appserver1-lab">
    <interface ip-addr="172.28.0.13" status="1" snmp-primary="P">
      <monitored-service service-name="ICMP"/>
      <monitored-service service-name="HTTP"/>
    </interface>
    <category name="Servers"/>
  </node>
</model-import>
```

Save this as `python-lab/scripts/lab-network-requisition.xml` (it's inside the volume you already mounted into `python-lab`, so the container can read it).

### B6.2 Push it via Python + REST

**`python-lab/scripts/provision.py`:**

```python
import requests
from requests.auth import HTTPBasicAuth

# Container DNS name, not an IP — Podman's network resolves this for you
OPENNMS = "http://opennms:8980/opennms/rest"
AUTH = HTTPBasicAuth("admin", "your-new-password")

requisition_xml = open("lab-network-requisition.xml").read()

r = requests.post(
    f"{OPENNMS}/requisitions",
    data=requisition_xml,
    headers={"Content-Type": "application/xml"},
    auth=AUTH,
)
print("Upload:", r.status_code)

r = requests.put(f"{OPENNMS}/requisitions/lab-network/import", auth=AUTH)
print("Import triggered:", r.status_code)
```

Run it **inside the python-lab container**:

```bash
podman exec -it python-lab python3 provision.py
```

### B6.3 Watch it land in the database

```bash
podman exec -it opennms-postgres psql -U opennms -d opennms
```

```sql
SELECT nodeid, nodelabel, foreignsource, foreignid, nodetype
FROM node WHERE foreignsource = 'lab-network';

SELECT n.nodelabel, ip.ipaddr, ip.issnmpprimary
FROM ipinterface ip JOIN node n ON ip.nodeid = n.nodeid
WHERE n.foreignsource = 'lab-network';

SELECT n.nodelabel, ip.ipaddr, s.servicename, ifs.status
FROM ifservices ifs
JOIN service s ON ifs.serviceid = s.serviceid
JOIN ipinterface ip ON ifs.ipinterfaceid = ip.id
JOIN node n ON ip.nodeid = n.nodeid
WHERE n.foreignsource = 'lab-network';
```

Three nodes, each with an interface and its services — provisioning's entire output made concrete.

## B7. Custom event → alarm mapping (up/down clear-key pair)

Same idea as a VM setup, but the file is delivered via the bind-mount you already defined in `compose.yaml` (`./opennms-config/lab-custom-events.xml`), so editing it on the host and restarting the `opennms` container is all you need — no copying files into a VM.

**`opennms-config/lab-custom-events.xml`:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<events xmlns="http://xmlns.opennms.org/xsd/eventconf">

  <event>
    <uei>uei.opennms.org/lab/diskFull</uei>
    <event-label>Lab: Disk usage critical</event-label>
    <descr>&lt;p&gt;Disk usage critical on %nodelabel%&lt;/p&gt;</descr>
    <logmsg dest="logndisplay">Disk usage critical on %nodelabel%</logmsg>
    <severity>Major</severity>
    <alarm-data reduction-key="%uei%:%dpname%:%nodeid%" alarm-type="1"/>
  </event>

  <event>
    <uei>uei.opennms.org/lab/diskOK</uei>
    <event-label>Lab: Disk usage normal</event-label>
    <descr>&lt;p&gt;Disk usage back to normal on %nodelabel%&lt;/p&gt;</descr>
    <logmsg dest="logndisplay">Disk usage normal on %nodelabel%</logmsg>
    <severity>Normal</severity>
    <alarm-data reduction-key="%uei%:%dpname%:%nodeid%" alarm-type="2"
                 clear-key="uei.opennms.org/lab/diskFull:%dpname%:%nodeid%"/>
  </event>

</events>
```

Register it and reload — since it's mounted at `/opt/opennms/etc/events/lab-custom-events.xml` already, you just need it referenced from `eventconf.xml` inside the container and Eventd reloaded:

```bash
podman exec -it opennms bash -c \
  "grep -q lab-custom-events /opt/opennms/etc/eventconf.xml || \
   sed -i '/<\/events>/i <event-file>events/lab-custom-events.xml<\/event-file>' /opt/opennms/etc/eventconf.xml"

podman exec -it opennms /usr/share/opennms/bin/send-event.pl \
  uei.opennms.org/internal/reloadDaemonConfig -p 'daemonName Eventd'
```

## B8. Run the lifecycle, end to end

All scripts run from inside `python-lab` via `podman exec`, all targeting the container DNS name `opennms`.

### B8.1 New alarm (problem occurs)

**`python-lab/scripts/fire_disk_full.py`:**

```python
import requests
from requests.auth import HTTPBasicAuth

OPENNMS = "http://opennms:8980/opennms/rest"
AUTH = HTTPBasicAuth("admin", "your-new-password")

def fire():
    event_xml = """<?xml version="1.0" encoding="UTF-8"?>
<log>
  <events>
    <event>
      <uei>uei.opennms.org/lab/diskFull</uei>
      <source>lab-simulator</source>
      <nodeid>3</nodeid>
      <interface>172.28.0.13</interface>
      <descr>Disk at 97%% on appserver1-lab</descr>
      <severity>Major</severity>
    </event>
  </events>
</log>"""
    r = requests.post(f"{OPENNMS}/events", data=event_xml,
                       headers={"Content-Type": "application/xml"}, auth=AUTH)
    print(r.status_code, r.text)

if __name__ == "__main__":
    fire()
```

```bash
podman exec -it python-lab python3 fire_disk_full.py
```

```sql
SELECT eventid, eventuei, eventnodeid, eventseverity, eventtime
FROM events ORDER BY eventid DESC LIMIT 1;

SELECT alarmid, reductionkey, severity_id, counter, alarmacktime, alarmcleartime
FROM alarms WHERE reductionkey LIKE '%diskFull%';
```

UI: **Alarms** page shows it Major, unacknowledged, counter `1`.

### B8.2 Escalation / dedup

```bash
podman exec -it python-lab python3 fire_disk_full.py   # run again
```

```sql
SELECT reductionkey, counter, lasteventtime FROM alarms WHERE reductionkey LIKE '%diskFull%';
```

Same `alarmid`, `counter` now `2` — no duplicate row.

### B8.3 Acknowledge

```bash
podman exec -it python-lab python3 -c "
import requests
from requests.auth import HTTPBasicAuth
requests.put('http://opennms:8980/opennms/rest/alarms/1',
             data={'ack': 'true'},
             auth=HTTPBasicAuth('admin','your-new-password'))
"
```

(Replace `1` with the real `alarmid` from your SQL query above.)

### B8.4 Clear

**`python-lab/scripts/fire_disk_ok.py`** — identical structure to `fire_disk_full.py`, but with `<uei>uei.opennms.org/lab/diskOK</uei>` and `<severity>Normal</severity>`.

```bash
podman exec -it python-lab python3 fire_disk_ok.py
```

```sql
SELECT reductionkey, severity_id, alarmcleartime FROM alarms WHERE reductionkey LIKE '%diskFull%';
```

`alarmcleartime` is now populated automatically via the clear-key match — you never touched this alarm directly.

### B8.5 SNMP-trap version (closer to real hardware)

**`python-lab/scripts/trap_sender.py`:**

```python
from pysnmp.hlapi import *

OPENNMS_HOST = "opennms"   # container DNS name

def send_trap(oid, varbind_oid, varbind_value, community="public"):
    errorIndication, errorStatus, errorIndex, varBinds = next(
        sendNotification(
            SnmpEngine(),
            CommunityData(community, mpModel=1),
            UdpTransportTarget((OPENNMS_HOST, 162)),
            ContextData(),
            'trap',
            NotificationType(ObjectIdentity(oid)).addVarBinds(
                (varbind_oid, OctetString(varbind_value))
            )
        )
    )
    print("Error:", errorIndication) if errorIndication else print(f"Trap sent: {oid}")

LINK_DOWN = "1.3.6.1.6.3.1.1.5.3"
LINK_UP   = "1.3.6.1.6.3.1.1.5.4"

if __name__ == "__main__":
    send_trap(LINK_DOWN, "1.3.6.1.2.1.2.2.1.1", "1")  # simulate switch1 port down
```

```bash
podman exec -it python-lab python3 trap_sender.py
# check Alarms UI — "Interface Down" alarm appears using OpenNMS's built-in reduction/clear-key pair
```

### B8.6 Syslog version

**`python-lab/scripts/syslog_sender.py`:**

```python
import socket, time

OPENNMS_HOST = "opennms"
SYSLOG_PORT = 514

def send_syslog(message, facility=1, severity=2, hostname="appserver1-lab"):
    pri = facility * 8 + severity
    timestamp = time.strftime("%b %d %H:%M:%S")
    packet = f"<{pri}>{timestamp} {hostname} appd[1234]: {message}"
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.sendto(packet.encode(), (OPENNMS_HOST, SYSLOG_PORT))
    sock.close()
    print(f"Syslog sent: {message}")

if __name__ == "__main__":
    send_syslog("CRITICAL disk usage at 97% on /var/log")
```

```bash
podman exec -it python-lab python3 syslog_sender.py
```

Add a matching `<ueiMatch>` pattern in the container's `syslogd-configuration.xml` (same reload pattern as B7) if you want it to map to a specific UEI instead of the generic syslog event.

## B9. Orchestrate a full incident with one script

**`python-lab/scripts/run_incident_simulation.py`:**

```python
import time
import fire_disk_full, fire_disk_ok

print("[T+0s] Problem occurs — firing diskFull event")
fire_disk_full.fire()

time.sleep(15)
print("[T+15s] Problem persists — firing duplicate diskFull (watch counter increment)")
fire_disk_full.fire()

time.sleep(10)
input("[T+25s] Acknowledge the alarm in the UI now, then press Enter...")

time.sleep(5)
print("[T+30s] Problem resolved — firing diskOK event (auto-clear)")
fire_disk_ok.fire()

print("Done. Check the Alarms page: severity=Cleared, alarmcleartime populated.")
```

```bash
podman exec -it python-lab python3 run_incident_simulation.py
```

---

# PART C — REFERENCE

## C1. Handy SQL

```sql
SELECT firsteventtime, lasteventtime, alarmacktime, alarmcleartime, counter, severity_id
FROM alarms WHERE alarmid = <id>;

SELECT eventid, eventuei, eventtime, eventseverity
FROM events WHERE eventreductionkey = '<reduction key string>'
ORDER BY eventtime;

SELECT alarmid, nodelabel, reductionkey, severity_id, counter
FROM alarms a JOIN node n ON a.nodeid = n.nodeid
WHERE alarmcleartime IS NULL;
```

Run these via `podman exec -it opennms-postgres psql -U opennms -d opennms`.

## C2. Key REST endpoints

| Purpose | Method + path |
|---|---|
| Upload/replace a requisition | `POST /rest/requisitions` |
| Trigger import | `PUT /rest/requisitions/{foreign-source}/import` |
| List nodes | `GET /rest/nodes` |
| Post a synthetic event | `POST /rest/events` |
| List/filter alarms | `GET /rest/alarms` |
| Acknowledge an alarm | `PUT /rest/alarms/{id}` with `ack=true` |
| Clear an alarm manually | `PUT /rest/alarms/{id}` with `clear=true` |

## C3. Podman-specific gotchas

- **`podman-compose up` fails to bind 162/udp or 514/udp:** you're rootless and the host's unprivileged port floor is blocking it. Run `sudo sysctl net.ipv4.ip_unprivileged_port_start=0`, or run the whole stack rootful (`sudo podman-compose up -d --build`).
- **Static IPs in `compose.yaml` are ignored / containers get random IPs:** your Podman is using the older `cni` network backend instead of `netavark`. Check with `podman info --format '{{.Host.NetworkBackend}}'` and upgrade Podman if needed (4.x+ defaults to netavark).
- **Postgres container can't write to its volume / "permission denied":** SELinux is blocking the mount — make sure every bind-mounted volume in `compose.yaml` has the `:Z` suffix (already included above). This is a Podman/SELinux thing you generally don't hit with plain Docker.
- **`python-lab` can't resolve `opennms` or `opennms-postgres` by name:** confirm all services are on the same user-defined network (`labnet`) — Podman's automatic DNS only works within a shared custom network, not the default bridge.
- **OpenNMS container restarts in a loop right after `up`:** almost always it started before PostgreSQL finished initializing. `depends_on` only waits for the container to *start*, not for Postgres to be ready — if this happens, add a short `sleep`/healthcheck loop, or just restart the `opennms` service once Postgres logs show it's accepting connections (`podman logs opennms-postgres`).
- **Trap/syslog sent from `python-lab` but nothing appears:** confirm `python-lab` and `opennms` are both actually up on `labnet` (`podman exec -it python-lab getent hosts opennms`) and that you used the container DNS name, not `localhost`, as the target.
- **Event appears in `events` but no alarm created:** the event definition has no `<alarm-data>` block, or `lab-custom-events.xml` isn't referenced in `eventconf.xml` yet — redo the B7 reload step.
- **Duplicate alarms instead of one incrementing counter:** your reduction key includes a field that varies between occurrences — reduction keys must be stable across repeats of the same problem.

---

## What changed from the VM version, and why

- Every major component (OpenNMS, PostgreSQL, snmpsim, the Python scripts) now runs in its **own container**, each with a static IP on a shared `labnet` network — this is what you asked for, and it also means you can tear down and rebuild any one piece (`podman-compose up -d --build snmpsim-switch1`) without touching the others, which is a nice property for a repeatable teaching lab.
- Container-to-container addressing uses **DNS names** (`opennms`, `opennms-postgres`, `snmpsim-switch1`) instead of a VM's static IP — this is idiomatic Podman/Docker networking and also means the requisition/scripts are portable across hosts without editing IPs.
- I flagged the two things that are genuinely different in Podman versus a VM setup and worth knowing up front: **rootless low-port binding** and **SELinux volume labeling** — both are common first-run blockers that have nothing to do with OpenNMS itself.
- As before, I didn't pin an exact OpenNMS container image tag or its full list of environment variables, since the official image and its config surface change between releases — check `docs.opennms.com`'s container deployment page at install time rather than trusting a version pinned here.

## Natural next steps

- **Rootless-only hardening pass** — if you specifically need to avoid `sudo podman-compose` entirely, I can write out the exact `sysctl`/`slirp4netns` config to make 162/514 work rootless reliably, rather than the "try it, fall back to rootful" approach above.
- **Thresholding** (Part D) — simulate CPU/memory metrics via SNMP and trigger threshold-crossing alarms.
- **Kubernetes/`podman play kube` variant** — if you want this same lab expressed as Kubernetes manifests instead of Compose, since Podman can generate/consume those directly.

Say the word and I'll build any of those out.
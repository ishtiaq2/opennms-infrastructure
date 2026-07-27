This is a classic **OSGi Declarative Services (SCR)** example that uses the **dynamic multiple reference** pattern (also called the whiteboard pattern) to track `ServiceMonitor` services as they come and go.

### What the code does

```java
@Component(immediate = true)
public class PollerDaemon {
```

- `@Component(immediate = true)` → The component is activated as soon as its dependencies are satisfied (no lazy activation).

```java
private final List<ServiceMonitor> monitors = new CopyOnWriteArrayList<>();
```

- A thread-safe list is used because bind/unbind methods can be called from different threads while the daemon may be iterating over the list.

```java
@Reference(
    cardinality = ReferenceCardinality.MULTIPLE,
    policy = ReferencePolicy.DYNAMIC,
    bind = "addMonitor",
    unbind = "removeMonitor"
)
public void addMonitor(ServiceMonitor newMonitor) { ... }
```

| Attribute | Meaning |
|-----------|---------|
| `cardinality = MULTIPLE` | Zero or more `ServiceMonitor` services are accepted |
| `policy = DYNAMIC` | Services can appear/disappear **while the component is active** (no deactivation) |
| `bind` / `unbind` | Explicit method names SCR will call |

When another bundle registers a `ServiceMonitor` service (e.g. an SNMP monitor), SCR calls `addMonitor()` on the **live** `PollerDaemon` instance.  
When that service is unregistered, SCR calls `removeMonitor()`.

### Typical next steps you would add

1. **Actual polling / scheduling logic**
2. A proper logger instead of `System.out`
3. Optional `@Activate` / `@Deactivate` methods
4. Possibly a scheduled executor that iterates over `monitors`

Example of a more complete skeleton:

```java
@Component(immediate = true)
public class PollerDaemon {

    private static final Logger LOG = LoggerFactory.getLogger(PollerDaemon.class);

    private final List<ServiceMonitor> monitors = new CopyOnWriteArrayList<>();
    private ScheduledExecutorService scheduler;

    @Activate
    void activate() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PollerDaemon");
            t.setDaemon(true);
            return t;
        });
        // start a periodic task that uses the current list of monitors
        scheduler.scheduleAtFixedRate(this::pollAll, 0, 30, TimeUnit.SECONDS);
        LOG.info("PollerDaemon activated");
    }

    @Deactivate
    void deactivate() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        monitors.clear();
        LOG.info("PollerDaemon deactivated");
    }

    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        bind = "addMonitor",
        unbind = "removeMonitor"
    )
    public void addMonitor(ServiceMonitor monitor) {
        LOG.info("New ServiceMonitor registered: {}", monitor);
        monitors.add(monitor);
        // optionally schedule it immediately
    }

    public void removeMonitor(ServiceMonitor monitor) {
        LOG.info("ServiceMonitor unregistered: {}", monitor);
        monitors.remove(monitor);
        // optionally cancel any ongoing work for this monitor
    }

    private void pollAll() {
        for (ServiceMonitor m : monitors) {
            try {
                // m.poll(...) or whatever the ServiceMonitor API looks like
            } catch (Exception e) {
                LOG.warn("Error polling {}", m, e);
            }
        }
    }
}
```

### Key points / common pitfalls

- Because the policy is `DYNAMIC`, the component stays alive when the last monitor disappears (cardinality is 0..n).
- `CopyOnWriteArrayList` is a good choice for the common “iterate often, mutate rarely” pattern.
- Never keep a strong reference to a service after `unbind` has been called – the list already handles removal.
- If you need the service properties (e.g. a `type` or `protocol` property), change the bind method signature to:

  ```java
  public void addMonitor(ServiceMonitor monitor, Map<String, Object> properties)
  ```

Would you like me to expand this into a full working example (including a sample `ServiceMonitor` interface and a concrete SNMP implementation), or are you looking for something specific (bug fix, alternative design, logging, etc.)?




Here’s a complete, self-contained working example of the dynamic multi-reference pattern you started with.

### 1. The service interface

```java
package com.example.poller;

/**
 * Contract that any monitor (SNMP, ICMP, HTTP, …) must implement.
 */
public interface ServiceMonitor {

    /**
     * Unique name of this monitor (used for logging / identification).
     */
    String getName();

    /**
     * Perform one poll cycle. Returns true if the target is considered UP.
     */
    boolean poll();
}
```

### 2. Concrete SNMP monitor (OSGi component)

```java
package com.example.poller.snmp;

import com.example.poller.ServiceMonitor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example SNMP monitor. In a real system you would inject Snmp4j or similar.
 * Here we just simulate a poll.
 */
@Component(
    service = ServiceMonitor.class,
    immediate = true,
    property = {
        "monitor.type=snmp",
        "monitor.target=192.168.1.1"
    }
)
public class SnmpServiceMonitor implements ServiceMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(SnmpServiceMonitor.class);

    private String target;

    @Activate
    void activate(org.osgi.service.component.ComponentContext ctx) {
        // You can read the property if you want
        Object t = ctx.getProperties().get("monitor.target");
        this.target = t != null ? t.toString() : "unknown";
        LOG.info("SNMP monitor for {} activated", target);
    }

    @Deactivate
    void deactivate() {
        LOG.info("SNMP monitor for {} deactivated", target);
    }

    @Override
    public String getName() {
        return "SNMP-" + target;
    }

    @Override
    public boolean poll() {
        // Simulate network call
        LOG.debug("Polling SNMP target {}", target);
        // Pretend it is up most of the time
        return Math.random() > 0.1;
    }
}
```

You can create as many of these as you like (HTTP monitor, ICMP monitor, …). Each will be automatically discovered by the `PollerDaemon`.

### 3. The complete PollerDaemon

```java
package com.example.poller;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component(immediate = true)
public class PollerDaemon {

    private static final Logger LOG = LoggerFactory.getLogger(PollerDaemon.class);

    // Thread-safe list – bind/unbind can happen while we are iterating
    private final List<ServiceMonitor> monitors = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService scheduler;

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Activate
    void activate() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PollerDaemon");
            t.setDaemon(true);
            return t;
        });

        // Poll every 30 seconds
        scheduler.scheduleAtFixedRate(this::pollAll, 5, 30, TimeUnit.SECONDS);
        LOG.info("PollerDaemon activated – waiting for ServiceMonitors");
    }

    @Deactivate
    void deactivate() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        monitors.clear();
        LOG.info("PollerDaemon deactivated");
    }

    // ------------------------------------------------------------------
    // Dynamic multiple reference (the part you originally posted)
    // ------------------------------------------------------------------

    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        bind = "addMonitor",
        unbind = "removeMonitor"
    )
    public void addMonitor(ServiceMonitor newMonitor) {
        LOG.info("New monitor arrived: {} – adding to schedule", newMonitor.getName());
        monitors.add(newMonitor);
    }

    public void removeMonitor(ServiceMonitor oldMonitor) {
        LOG.info("Monitor removed: {} – pausing its schedule", oldMonitor.getName());
        monitors.remove(oldMonitor);
    }

    // ------------------------------------------------------------------
    // Polling logic
    // ------------------------------------------------------------------

    private void pollAll() {
        if (monitors.isEmpty()) {
            LOG.debug("No monitors registered yet");
            return;
        }

        LOG.info("Starting poll cycle ({} monitors)", monitors.size());
        for (ServiceMonitor m : monitors) {
            try {
                boolean up = m.poll();
                LOG.info("{} → {}", m.getName(), up ? "UP" : "DOWN");
            } catch (Exception e) {
                LOG.warn("Error while polling {}", m.getName(), e);
            }
        }
    }
}
```

### How it works at runtime

1. `PollerDaemon` starts immediately and begins its scheduled task.
2. When the SNMP bundle (or any other monitor bundle) is installed and its component activates, SCR calls `addMonitor(...)` on the **already running** `PollerDaemon`.
3. The next poll cycle automatically includes the new monitor.
4. When the SNMP bundle is uninstalled, SCR calls `removeMonitor(...)` and the monitor disappears from subsequent cycles.
5. The daemon itself never stops, even if zero monitors are present.

### Optional: richer bind method (service properties)

If you want the properties that were published with the service:

```java
public void addMonitor(ServiceMonitor monitor, Map<String, Object> properties) {
    String type = (String) properties.get("monitor.type");
    String target = (String) properties.get("monitor.target");
    LOG.info("New {} monitor for target {} arrived", type, target);
    monitors.add(monitor);
}
```

### Build / deploy notes

- Use the OSGi Declarative Services annotation processor (bnd, maven-bundle-plugin with `_dsannotations`, or the official SCR annotations).
- Make sure both the interface package and the concrete monitor packages are exported / imported correctly.
- The `PollerDaemon` does **not** need to import the concrete SNMP class – it only depends on the `ServiceMonitor` interface. That is the whole point of the whiteboard pattern.

You can drop additional monitors (HTTP, JDBC, custom protocols, …) into the system at any time and the daemon will pick them up automatically.

# Add HTTP monitor implementation

Here’s a clean HTTP monitor that follows the same pattern as the SNMP one.

### HttpServiceMonitor

```java
package com.example.poller.http;

import com.example.poller.ServiceMonitor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Simple HTTP/HTTPS monitor.
 * Polls a configured URL and considers the service UP when the response code is 2xx or 3xx.
 */
@Component(
    service = ServiceMonitor.class,
    immediate = true,
    property = {
        "monitor.type=http",
        "monitor.target=https://httpbin.org/status/200",   // change me
        "monitor.timeout=5000"                             // milliseconds
    }
)
public class HttpServiceMonitor implements ServiceMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(HttpServiceMonitor.class);

    private volatile String target;
    private volatile int timeoutMs = 5000;

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Activate
    void activate(Map<String, Object> properties) {
        updateConfig(properties);
        LOG.info("HTTP monitor activated → {}", target);
    }

    @Modified
    void modified(Map<String, Object> properties) {
        updateConfig(properties);
        LOG.info("HTTP monitor configuration updated → {}", target);
    }

    @Deactivate
    void deactivate() {
        LOG.info("HTTP monitor deactivated → {}", target);
    }

    private void updateConfig(Map<String, Object> properties) {
        Object t = properties.get("monitor.target");
        if (t != null) {
            this.target = t.toString();
        }

        Object to = properties.get("monitor.timeout");
        if (to != null) {
            try {
                this.timeoutMs = Integer.parseInt(to.toString());
            } catch (NumberFormatException e) {
                LOG.warn("Invalid timeout value '{}', keeping {}", to, timeoutMs);
            }
        }
    }

    // ------------------------------------------------------------------
    // ServiceMonitor API
    // ------------------------------------------------------------------

    @Override
    public String getName() {
        return "HTTP-" + target;
    }

    @Override
    public boolean poll() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(target);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            boolean up = code >= 200 && code < 400;

            LOG.debug("{} → HTTP {} ({})", target, code, up ? "UP" : "DOWN");
            return up;

        } catch (Exception e) {
            LOG.warn("HTTP poll failed for {}: {}", target, e.toString());
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
```

### How it fits together

- The component registers itself as a `ServiceMonitor`.
- `PollerDaemon` automatically discovers it via the dynamic `@Reference` and starts polling it.
- You can change the target URL or timeout at runtime (Config Admin / metatype) thanks to the `@Modified` method.
- Multiple instances can be created simply by publishing more components with different `monitor.target` properties (or by using factory components).

### Quick test targets you can use

| URL | Expected result |
|-----|-----------------|
| `https://httpbin.org/status/200` | UP |
| `https://httpbin.org/status/500` | DOWN |
| `https://httpbin.org/delay/10`   | DOWN (if timeout < 10 s) |
| `http://127.0.0.1:9`             | DOWN (connection refused) |

Drop this class into your project alongside the SNMP monitor and the `PollerDaemon` will pick it up automatically the next time the bundle is refreshed.

# Use HttpClient instead of HttpURLConnection

Here’s the updated `HttpServiceMonitor` that uses the modern `java.net.http.HttpClient` (Java 11+):

```java
package com.example.poller.http;

import com.example.poller.ServiceMonitor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP/HTTPS monitor using java.net.http.HttpClient.
 * Considers the service UP when the response status is 2xx or 3xx.
 */
@Component(
    service = ServiceMonitor.class,
    immediate = true,
    property = {
        "monitor.type=http",
        "monitor.target=https://httpbin.org/status/200",
        "monitor.timeout=5000"          // milliseconds
    }
)
public class HttpServiceMonitor implements ServiceMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(HttpServiceMonitor.class);

    private volatile String target;
    private volatile Duration timeout = Duration.ofMillis(5000);

    // Re-usable, thread-safe client
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Activate
    void activate(Map<String, Object> properties) {
        updateConfig(properties);
        LOG.info("HTTP monitor activated → {}", target);
    }

    @Modified
    void modified(Map<String, Object> properties) {
        updateConfig(properties);
        LOG.info("HTTP monitor configuration updated → {}", target);
    }

    @Deactivate
    void deactivate() {
        LOG.info("HTTP monitor deactivated → {}", target);
    }

    private void updateConfig(Map<String, Object> properties) {
        Object t = properties.get("monitor.target");
        if (t != null) {
            this.target = t.toString();
        }

        Object to = properties.get("monitor.timeout");
        if (to != null) {
            try {
                this.timeout = Duration.ofMillis(Long.parseLong(to.toString()));
            } catch (NumberFormatException e) {
                LOG.warn("Invalid timeout value '{}', keeping {}", to, timeout.toMillis());
            }
        }
    }

    // ------------------------------------------------------------------
    // ServiceMonitor API
    // ------------------------------------------------------------------

    @Override
    public String getName() {
        return "HTTP-" + target;
    }

    @Override
    public boolean poll() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(target))
                    .timeout(timeout)
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            int code = response.statusCode();
            boolean up = code >= 200 && code < 400;

            LOG.debug("{} → HTTP {} ({})", target, code, up ? "UP" : "DOWN");
            return up;

        } catch (Exception e) {
            LOG.warn("HTTP poll failed for {}: {}", target, e.toString());
            return false;
        }
    }
}
```

### Key improvements

- Uses the modern, non-blocking-capable `HttpClient` (still used synchronously here for simplicity).
- Proper timeout handling via `HttpRequest.timeout(...)`.
- Redirects are followed automatically (`Redirect.NORMAL`).
- Response body is discarded (`BodyHandlers.discarding()`) – we only care about the status code.
- The `HttpClient` instance is created once and reused (it is thread-safe).

The rest of the system (`PollerDaemon`, SNMP monitor, etc.) remains unchanged.


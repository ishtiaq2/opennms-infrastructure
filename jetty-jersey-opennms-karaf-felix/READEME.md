# 1. Introduction to Jetty

Jetty is a lightweight, embeddable Java HTTP server and servlet container, developed by the Eclipse Foundation. 
Unlike Tomcat, which is usually run as a standalone server you deploy WARs into, Jetty is designed to be embedded inside your own application — 
you write a small amount of Java code that starts an HTTP server as part of your program's own process.

## Key things to know about Jetty:

It implements the Servlet API, so anything written against HttpServlet, ServletContext, 
filters, listeners, etc. works on Jetty the same way it would on Tomcat or any other compliant container.

## It's built from small, composable parts: 
* A Server, one or more Connectors (which bind to a port and protocol), and 
* One or more Handlers (which decide what to do with a request — serve a servlet, serve static files, proxy, etc.).

Because it starts in a few lines of code and has a small footprint, it's a popular choice for embedding a web server inside 
a larger Java application rather than shipping a separate app server. 

## This is exactly why OpenNMS uses it — 
* OpenNMS is a single Java process, and it needs an HTTP server living inside that process rather than a separate Tomcat instance to manage.

*****************************************************************************

# 2. Introduction to Jersey

Jersey is the reference implementation of JAX-RS (Jakarta RESTful Web Services, formerly javax.ws.rs) — 
the Java standard for building REST APIs using annotations.

Instead of writing raw servlets that parse URLs and HTTP methods by hand, 
with Jersey you write plain Java classes annotated with things like:

@Path("/nodes") — the URL path this class/method handles

@GET, @POST, @PUT, @DELETE — the HTTP method

@Produces(MediaType.APPLICATION_JSON) / @Consumes(...) — content negotiation

@PathParam, @QueryParam — pulling values out of the URL

* Jersey needs a servlet container to run inside — it doesn't talk HTTP itself. 
* That's where Jetty comes back in: 
* Jersey ships a servlet (ServletContainer) that you register with Jetty 
(or Tomcat, or any servlet container), and Jersey handles routing incoming requests to the right annotated method based on the @Path/@GET metadata.

So the relationship in one sentence: Jetty serves HTTP; Jersey decides which Java method handles a given REST request once Jetty hands it off.

*****************************************************************************


# 3. Their Relation with OpenNMS

OpenNMS relies heavily on REST APIs for its web interface, integrations, and external scripting.

OpenNMS uses Jetty as its internal web server to listen on port 8980.

It uses Jersey to map incoming requests to the correct Java code that 
actually performs network management tasks (like fetching an alarm or updating a node).


Putting 1 and 2 together, here's how OpenNMS actually uses them:

* Jetty is the HTTP server for the entire OpenNMS process. 
When you start OpenNMS, Jetty is what binds to port 8980 (by default) and accepts browser connections. 
Jetty also starts the Karaf/OSGi layer, and the primary OpenNMS web console runs as a web application started by Jetty — 
most network operations personnel interact with OpenNMS through this web console

* Jersey powers the OpenNMS REST API. 
The REST APIs run as an application started by Jetty, 
and most integrations use these REST APIs to interact with the OpenNMS core, 
with each access request requiring HTTP basic authentication for a user holding the ROLE_ADMIN or 
ROLE_REST permission.
Under the hood, those REST endpoints (/opennms/rest/nodes, /opennms/rest/alarms, etc.) are 
Jersey resource classes annotated with @Path, exactly like the GreetingResource example above.

* Login and session handling also happens through Jetty: 
After a successful login, a session cookie is created and passed to the browser, and 
Jetty together with Spring Security uses this cookie to control access to OpenNMS screens based on user permissions.

* So the practical mental model: 
OpenNMS = one JVM process, inside which Jetty is the "front door" for all HTTP traffic, 
and it routes traffic to two very different kinds of things — 
the legacy JSP/servlet-based web console, and 
the Jersey-powered REST layer — plus (as we'll see next) an entire OSGi/Karaf runtime.

### In OpenNMS, Jetty and Jersey configurations are managed primarily through Karaf's configuration files located in /opt/opennms/etc/.

* org.ops4j.pax.web.cfg: This file configures the Jetty web server under Karaf. Here you can define the HTTP ports, SSL/TLS certificates, and listening interfaces.

* Blueprint XML: OpenNMS uses OSGi Blueprint (usually located inside your plugin's JAR file at OSGI-INF/blueprint/blueprint.xml) to tell Karaf: "Hey, I have a Jersey REST service here, please register it with Jetty!"

*****************************************************************************

# 4. Introduction to Apache Karaf and Apache Felix

To understand why OpenNMS needs Karaf, you first need OSGi.

OSGi (Open Services Gateway initiative) is a module system and service platform for Java. 
It solves a problem plain Java doesn't: 

* Normal Java has no first-class concept of "module" beyond a JAR on the classpath, no way to hot-swap a 
piece of code without restarting the whole JVM, and 
no built-in way to say "only expose these packages publicly, hide everything else." 
OSGi introduces:

## Bundles — a JAR file plus extra metadata (in its MANIFEST.MF) declaring what packages it exports, what packages it imports/requires, and its version.

## A service registry — bundles can publish Java objects as "services" that other bundles can look up dynamically, without hard-coding a dependency.

## A bundle lifecycle — bundles can be installed, started, stopped, updated, and uninstalled individually, at runtime, without restarting the container.

* Apache Felix is one implementation of the OSGi specification — 
it's the actual low-level OSGi framework (the engine that loads bundles, resolves their dependencies, and manages their lifecycle).

## Apache Karaf is built on top of an OSGi framework (by default Felix, though it's pluggable) and adds a much friendlier layer around it:

A shell/console (karaf@root()>) for installing, starting, stopping, and inspecting bundles interactively.

## Features — a higher-level packaging concept: 
* a "feature" is a named, versioned bundle of bundles plus configuration, so you can say feature:install my-feature instead of manually installing ten individual bundles in the right order.
Configuration management, logging, and a hot-deploy directory.

### So the relationship: 
Felix is the engine; Karaf is the friendly car built around that engine. 
You could run raw Felix, but almost nobody does directly — you use Karaf (or Equinox, another popular OSGi implementation) because of the tooling it adds.

Quick hands-on: poking around a Karaf shell

### ********************
If you download standalone Karaf (unrelated to OpenNMS, just to get a feel for it):

tar -xzf apache-karaf-4.x.x.tar.gz
cd apache-karaf-4.x.x/bin
./karaf

Inside the shell, try:

karaf@root()> bundle:list
karaf@root()> feature:list | grep -i started
karaf@root()> bundle:install -s mvn:com.sun.jersey/jersey-core/1.18.1

bundle:list shows every installed bundle and its state (Active, Resolved, Installed). 
This is the exact mechanism OpenNMS uses internally — just wrapped in OpenNMS-specific tooling, as you'll see next.
### ********************

****************************************************************************************************

# 5. Jetty and Jersey and their relation with Apache Karaf and Felix

## Karaf uses a tool called Pax Web to run Jetty natively inside the Felix OSGi environment. 
When you write a Jersey REST service, you compile it into an OSGi bundle and drop it into Karaf. 
Karaf dynamically wires your Jersey code into the running Jetty server, immediately exposing your API to the world.

Here's where the four pieces click together.

OpenNMS uses a modified version of Karaf as the OSGi framework to control the internal OpenNMS environment within the OpenNMS Core, Sentinel, and Minion.
Traditional Karaf implementations manage application bundles, and OpenNMS's customizations allow Karaf to also interact with legacy OpenNMS daemons and 
parts of the web application, while continuing to use the same Spring Security components as the rest of the OpenNMS core.

## Concretely:

* Felix is the OSGi engine underneath Karaf, so it's underneath OpenNMS's whole plugin system, even though you rarely interact with Felix directly — 

* Karaf is the layer you actually touch (via its shell, reachable with ssh -p 8101 admin@localhost on a running OpenNMS instance, or via the karaf.sh script in $OPENNMS_HOME/bin).

* Jetty is what boots Karaf inside OpenNMS. <cite index="1-1">Jetty also starts the Karaf/OSGi layer</cite> — so rather than Karaf being a separate process, OpenNMS's single Jetty-hosted process brings the OSGi container up as part of its own startup sequence.

Jersey bundles can themselves run as OSGi bundles inside Karaf. This matters because OpenNMS is gradually moving functionality out of the old monolithic Spring classpath and into individually deployable OSGi bundles. <cite index="5-1">This document explains how to integrate a Maven module — an OSGi bundle — into the OpenNMS system, and describes how new Maven modules are usually distributed as a bundle consisting of a JAR file plus OSGi metadata.</cite>

A key wiring detail for developers extending OpenNMS: <cite index="5-1">if a Maven module should be loaded in Spring by default it needs to be included in opennms-base-assembly/pom.xml, and all modules loaded that way become available under ${opennms.target}/opennms/lib; some legacy/RPC modules are only defined in Spring and loaded via the default classloader; Spring beans can be exposed into the OSGi service registry using <onmsgi:service>; and packages listed in custom.properties are loaded into the default classloader even when they're also used from OSGi.</cite>

Why does OpenNMS bother with all this instead of just being one big JAR? Because OSGi/Karaf lets OpenNMS ship optional, independently-versioned add-ons (feature packs, integrations, Minion, Sentinel) that can be installed, updated, or removed on a running system without restarting the whole platform — the same benefit OSGi gives any large modular application.

A useful mental picture:

 OpenNMS JVM process
 └── Jetty (HTTP server, binds :8980)
       ├── Legacy web console (JSP/Spring MVC servlets)
       ├── Jersey servlet container → REST API (/opennms/rest/...)
       └── boots → Karaf container (OSGi runtime, powered by Felix)
             ├── bundle: opennms-osgi-core
             ├── bundle: various feature bundles (Minion RPC, Kafka IPC, ...)
             └── your custom bundle, deployed here in Section 7


****************************************************************************************************             

# 6. Jetty and Jersey configuration in OpenNMS

You generally won't hand-write Jetty/Jersey wiring for the core web console — that comes pre-built. 
But you should know where the levers are, both for troubleshooting and because they matter when you deploy your own bundle in Section 7.

Where to look in an installed OpenNMS:

* $OPENNMS_HOME/etc/ — holds most runtime configuration, including org.opennms.netmgt.jetty.cfg-style properties 
that control things like the HTTP port, HTTPS, thread pool sizing, and request/header size limits for the embedded Jetty instance.

* $OPENNMS_HOME/jetty-webapps/ (or webapps/, depending on release) — contains the actual web application(s) Jetty serves, including the main opennms web app whose WEB-INF/web.xml wires up the Jersey servlet (ServletContainer) and tells it which package(s) to scan for @Path-annotated resource classes.

* $OPENNMS_HOME/etc/org.apache.karaf.features.cfg — controls which Karaf feature repositories and features are loaded at startup; this is the file you'd extend if you wanted your own bundle to be picked up automatically.

* The Karaf shell itself — reachable with: ssh -p 8101 admin@localhost

From there, feature:list, bundle:list, and bundle:watch are your main troubleshooting commands, exactly as introduced in Section 4, just now running inside the actual OpenNMS process.

Typical Jersey wiring pattern (this is the shape of what OpenNMS's own web.xml does, simplified so you can recognize it):

xml
<servlet>
    <servlet-name>Jersey REST Service</servlet-name>
    <servlet-class>org.glassfish.jersey.servlet.ServletContainer</servlet-class>
    <init-param>
        <param-name>jersey.config.server.provider.packages</param-name>
        <param-value>org.opennms.web.rest.v2</param-value>
    </init-param>
</servlet>
<servlet-mapping>
    <servlet-name>Jersey REST Service</servlet-name>
    <url-pattern>/rest/*</url-pattern>
</servlet-mapping>

### That single init-param is the important line: it tells Jersey to scan the org.opennms.web.rest.v2 package for classes annotated with @Path, and 
automatically wire each one up as a REST endpoint under /opennms/rest/.... When you write your own REST resource class inside OpenNMS's codebase, this is the mechanism that picks it up — no manual servlet registration needed, just the right package and the right annotations.

For OSGi-deployed bundles (rather than code compiled directly into OpenNMS), the wiring is different again — 
you register your servlet or Jersey application with the OSGi HTTP Service that Jetty exposes into the OSGi container, which is exactly what we do hands-on in the next section.

****************************************************************************************************
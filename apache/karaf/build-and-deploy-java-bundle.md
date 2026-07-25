# Mastering OSGi: Bundles, DS, and Deployment

**Learning Objectives Checklist**
*   [ ] What makes an OSGi bundle different from a normal JAR
*   [ ] What the `MANIFEST.MF` file is and why it is the most important file
*   [ ] How Maven transforms Java code into an OSGi bundle
*   [ ] What Declarative Services (DS) are
*   [ ] What the Service Component Runtime (SCR) does
*   [ ] How Karaf hot-deploy works
*   [ ] What Apache Felix actually does during deployment
*   [ ] Understand the OSGi bundle lifecycle states
*   [ ] How to inspect and debug bundles

---

## Architecture Overview
*Before writing any code, understand what will happen.*

When you build and deploy a modern OpenNMS plugin, data flows through a strict pipeline from your source code into the live OSGi runtime. 

```text
[1. YOUR CODE] 
  @Component
  public class MyPlugin { ... }
        │
        ▼
[2. MAVEN BUILD (maven-bundle-plugin)]
  Compiles Java + Auto-generates MANIFEST.MF
        │
        ▼
[3. THE BUNDLE (.jar)]
  Dropped into Karaf's /deploy directory
        │
        ▼
[4. KARAF HOT-DEPLOY SCANNER]
  [4. KARAF FILEINSTALL]
  Detects the new bundle
        │
        ▼
  Requests installation through the OSGi Framework
        │
        ▼
[5. APACHE FELIX (The Kernel)]
  [5. APACHE FELIX (OSGi Framework)]

  Reads MANIFEST.MF
        │
  Resolves Imports
          │
  Creates Bundle
          │
  Creates Isolated ClassLoader
          │
  Transitions Bundle State
          │
  Notifies SCR
        │
        ▼
[6. SCR (Service Component Runtime)]
  Detects @Component -> Instantiates class -> Wires dependencies

```

---

## 1. The OSGi Bundle & The MANIFEST.MF

**What makes a bundle different from a normal JAR?**
A standard Java `.jar` file is just a zip archive containing compiled `.class` files. If you drop it into a standard Java application, all classes are instantly visible to everything else (a flat classpath).

An OSGi bundle is a standard `.jar` file with one critical addition: strict metadata located in `META-INF/MANIFEST.MF`.

**Why is `MANIFEST.MF` the most important file?**
The manifest acts as the **contract** between your bundle and the OSGi runtime. It declares what the bundle requires, what it provides, and how Felix should manage it. Without it, Felix will not let your code run.

It contains OSGi headers that define the "Invisible Shields", specifically:

* **`Import-Package`:** The exact Java packages your bundle requires from the outside world to survive.
* **`Export-Package`:** The exact Java packages your bundle is willing to share with the outside world. Everything else is locked down and hidden.
* **Bundle Metadata:** Information such as the bundle name, symbolic name, version, capabilities, and other configuration data used by the OSGi runtime.

---

## 2. Maven's Role

Writing a `MANIFEST.MF` by hand is tedious and error-prone. If you miss a single required package, your bundle crashes.

**How Maven transforms Java into OSGi:**
We use the `maven-bundle-plugin` (often powered by the BND tool). When you run `mvn clean install`, this plugin scans your compiled Java bytecode, identifies every external class you imported in your code, and **automatically generates the `MANIFEST.MF**` with the perfectly calculated `Import-Package` list.

---

## 3. Declarative Services (DS) and SCR

In the early days of OSGi, developers had to write hundreds of lines of boilerplate code (`BundleActivator`) just to register a service or look up a dependency.

**What are Declarative Services (DS)?**
DS is a modern OSGi specification that eliminates boilerplate. Instead of writing framework code, you simply declare what your class is using annotations like `@Component` and `@Reference`.

**What does the Service Component Runtime (SCR) do?**
SCR is itself implemented as one or more OSGi bundles. Like your plugin, it runs inside the same OSGi framework. This illustrates one of OSGi's core ideas: runtime services are built from modular bundles that cooperate through the framework.
DS is just the specification (the annotations). **SCR** is the actual engine running inside Karaf that reads those annotations and executes the following lifecycle:

```text
[ @Component Annotation ]
          │
          ▼
[ SCR detects component ]
          │
          ▼
[ Creates object instance ]
          │
          ▼
[ Injects @Reference services ]
          │
          ▼
[ Calls @Activate() method ]
          │
          ▼
[ Component Active & Registered ]

```

---

## 4. The Deployment Lifecycle

**How Karaf Hot-Deploy Works:**
Karaf runs a background tool called `FileInstall`. It constantly monitors the `deploy/` directory. When you copy a `.jar` file into that folder, `FileInstall` instantly detects the file system change and issues an automatic `bundle:install` and `bundle:start` command to the kernel.

**What Apache Felix actually does during deployment:**
When Felix receives the bundle from Karaf, it executes a strict survival sequence:

1. **Parse:** It reads the `MANIFEST.MF`.
2. **Resolve:** It checks the `Import-Package` list and searches the Karaf environment. *Are all required packages available in the exact versions requested?*
3. **Isolate:** If yes, it builds an isolated ClassLoader specifically for this bundle.
4. **Activate:** It transitions the bundle state to `Active` and hands control over to SCR to wake up your `@Component` classes.

*(Note: If step 2 fails, Felix blocks the bundle, trapping it in the `Installed` or `Resolved` state to protect the rest of the system from crashing).*

Parse

↓

Resolve

↓

Create ClassLoader

↓

Transition Bundle

↓

Notify SCR

↓

SCR Creates Components

↓

Component Active

---

## 5. Understanding Bundle States

5. When a bundle is deployed into Karaf, Felix transitions it through a strict lifecycle. When you run bundle:list in the console, you  
   will always see your bundle in one of these states:

# State,      What it means

Installed   The .jar has been physically loaded into Felix, but its dependencies (Import-Package) have not been successfully resolved yet. 
            If a bundle is stuck here, something is missing."
Resolved    All dependencies are successfully wired. The bundle is perfectly healthy and ready to run, but it has not been started yet (or 
            was manually stopped).
Starting    A temporary state. The bundle is actively waking up and handing control to SCR or executing its Activator.
Active      The goal. The bundle is running perfectly and providing its services to the OpenNMS ecosystem.
Stopping    A temporary state. The bundle is shutting down, closing connections, and unregistering from the Service Registry."
Uninstalled The bundle has been completely removed from Felix's memory.
---

## 6. Inspecting and Debugging Bundles

When things go wrong, use the Karaf SSH console (`karaf@root>`) to troubleshoot.

| Command | Purpose / When to use it |
| --- | --- |
| `bundle:list` | The dashboard. Look at the State column. You want `Active`. If it says `Installed` or `Resolved`, something is wrong. |
| `diag <bundle-id>` | **The absolute best debugging tool.** If a bundle won't start, `diag` will tell you exactly which `Import-Package` is missing from the environment. |
| `bundle:headers <id>` | Prints the `MANIFEST.MF` so you can see exactly what Maven auto-generated. |
| `scr:list` | Lists all Declarative Service `@Component`s and shows if they successfully wired together. |
| `log:tail` | Streams real-time application logs. Use this while dropping a file into `deploy/` to watch the deployment happen live. |

| Command        | Purpose                                                                                            |
| -------------- | -------------------------------------------------------------------------------------------------- |
| `service:list` | Displays all services currently registered in the OSGi Service Registry.                           |
| `feature:list` | Shows which Karaf features are installed. Useful when a runtime capability such as SCR is missing. |


7. How This Relates to OpenNMS
Every major OpenNMS subsystem is packaged as one or more OSGi bundles and follows this exact same deployment lifecycle.

+-------------------------------------------------------+
|                       OpenNMS                         |
|                                                       |
|  ├── Pollerd             ├── Provisiond               |
|  ├── Collectd            ├── REST APIs                |
|  ├── Eventd              ├── Web UI Plugins           |
|  ├── Alarmd              └── Your Plugin              |
+-------------------------------------------------------+
                            │
                            ▼
+-------------------------------------------------------+
|               Declarative Services (DS)               |
+-------------------------------------------------------+
                            │
                            ▼
+-------------------------------------------------------+
|                  Apache Felix (OSGi)                  |
+-------------------------------------------------------+
                            │
                            ▼
+-------------------------------------------------------+
|                 Java Virtual Machine                  |
+-------------------------------------------------------+


📌 Key Takeaways
An OSGi bundle is a standard Java JAR enhanced with OSGi metadata stored in META-INF/MANIFEST.MF.

The manifest is the contract between the bundle and the OSGi runtime. It defines the bundle's identity, dependencies, and capabilities.

Maven generates this metadata automatically, allowing developers to focus on writing Java code rather than maintaining the manifest by hand.

Declarative Services (DS) let developers declare components and dependencies using annotations instead of lifecycle boilerplate.

The Service Component Runtime (SCR) discovers those annotations, creates component instances, injects dependencies, and manages their lifecycle.

Apache Karaf provides deployment, configuration, logging, and management features, while Apache Felix performs bundle resolution, classloader isolation, lifecycle management, and service registry operations.

Understanding bundle lifecycle states (Installed, Resolved, Starting, Active, Stopping, Uninstalled) makes diagnosing deployment problems much easier.

The Karaf commands (bundle:list, diag, bundle:headers, scr:list, and service:list) are your primary tools for understanding what the OSGi runtime is doing.
What you're feeling is completely normal. In fact, **most Java developers—even experienced ones—find OSGi/Declarative Services unintuitive at first** because it changes one of the assumptions we've been taught:

> **"Objects create and manage other objects."**

OSGi says:

> **"Objects describe what they need. The runtime manages the relationships."**

That is a very different programming model.

The key realization is this:

> **SCR is not controlling your objects from inside them. It is controlling the environment around them.**

---

# Let's start with normal Java

This is probably how you've written code for years.

```java
public class PollerDaemon {

    private final SnmpMonitor snmp = new SnmpMonitor();
    private final HttpMonitor http = new HttpMonitor();

    public void run() {
        snmp.poll();
        http.poll();
    }
}
```

The picture is

PollerDaemon
    │
    ├────────► SnmpMonitor
    │
    └────────► HttpMonitor

Everything is obvious.

* PollerDaemon creates them.
* PollerDaemon owns them.
* PollerDaemon destroys them.

This is classic Object-Oriented Programming.

---

# Now imagine plugins

Suppose someone writes a new plugin.

Tomorrow someone develops

```text
CiscoMonitor
```

How can PollerDaemon know about it?

It can't.

You would have to modify PollerDaemon.

```java
private CiscoMonitor cisco = new CiscoMonitor();
```

Then next year

```text
JuniperMonitor
```

Again you modify PollerDaemon.

Eventually

```text
PollerDaemon

├── Snmp
├── HTTP
├── Cisco
├── Juniper
├── VMware
├── Oracle
├── SAP
├── Azure
├── AWS
├── ...
```

This completely defeats the purpose of plugins.

---

# The inversion

OSGi flips the ownership.

Instead of

```text
PollerDaemon
    │
creates
    │
    ▼
Monitor
```

it becomes

```text
Monitor
```

exists independently.

Then

```text
PollerDaemon

says

"I can use any monitor."
```

Notice how different that is.

---

# Think of electricity

Imagine a wall socket.

Does your house know what appliance you'll plug in tomorrow?

No.

Today

```text
Wall Socket

↓

Phone Charger
```

Tomorrow

```text
Wall Socket

↓

Vacuum Cleaner
```

Next week

```text
Wall Socket

↓

Laptop
```

Did the wall change?

No.

The wall simply provides electricity.

---

PollerDaemon is like the wall socket.

It doesn't know

* SNMP
* HTTP
* ICMP

It simply accepts

```text
ServiceMonitor
```

objects.

---

# Who plugs things in?

This is SCR.

```text
             SCR

        plugs things together

      ┌───────────────┐
      │               │
      ▼               ▼

PollerDaemon     SnmpMonitor
```

Notice

SCR is **not inside** PollerDaemon.

It is outside.

Exactly like an electrician plugging something into the wall.

---

# Another analogy: USB

Your computer has USB.

```text
Computer

USB Port
```

When you plug in

```text
Mouse
```

Windows detects it.

Then

```text
Mouse Driver
```

starts working.

When you unplug it

Windows tells applications

> Mouse disconnected.

Did Word create the mouse?

No.

Did Excel own the mouse?

No.

The operating system manages the relationship.

---

OSGi is doing almost exactly that.

```text
Operating System

↓

detect USB

↓

notify applications
```

becomes

```text
SCR

↓

detect ServiceMonitor

↓

notify PollerDaemon
```

---

# This is Dependency Injection

You've probably already used this in Spring.

```java
@Service
public class UserService {

    @Autowired
    Database database;
}
```

Did UserService create Database?

No.

Spring did.

So

```text
Spring

↓

creates Database

↓

injects it

↓

UserService
```

That's already an external object manager.

---

OSGi is simply more dynamic.

Spring usually injects once.

```text
Startup

↓

Inject

↓

Done
```

OSGi can inject

```text
Startup

↓

Inject

↓

Remove

↓

Inject another

↓

Remove again

↓

Inject five more
```

while the application keeps running.

---

# So who owns the objects?

Think of SCR as a property manager.

```text
Apartment Building

Property Manager
```

Tenants move in.

```text
Apartment 101

↓

Alice
```

Later

```text
Apartment 101

↓

Bob
```

The apartment didn't decide.

The manager did.

The apartment simply says

> Someone lives here.

PollerDaemon is like the apartment.

SCR is like the property manager.

---

# Why this feels strange

Most object-oriented books teach

```text
Object A

↓

creates

↓

Object B
```

or

```text
Object A

↓

inherits

↓

Object B
```

OSGi introduces another relationship:

```text
Runtime

↓

creates both

↓

connects them

↓

disconnects them

↓

reconnects them
```

This isn't inheritance.

It isn't composition.

It isn't aggregation.

It's **runtime composition**.

---

# A Better Mental Model

Instead of thinking:

> "SCR is another object."

Think:

> **SCR is part of the runtime, like the operating system.**

Your Java objects don't ask Linux to schedule CPU time.

Linux just does it.

Your objects don't ask the JVM to perform garbage collection.

The JVM just does it.

Likewise, your objects don't ask SCR to wire services together.

SCR just does it based on the metadata (`@Component`, `@Reference`, etc.).

---

## I think the analogy that will make everything click

Given the kinds of questions you've been asking about OpenNMS, Karaf, and OSGi, I think the explanation that will connect all the pieces is to **build a miniature SCR from scratch in plain Java**.

We'll start with:

1. A simple `ServiceRegistry` class.
2. A tiny `ComponentManager` that scans components.
3. A custom `@Reference` annotation.
4. Code that manually calls `addMonitor()` and `removeMonitor()`.
5. Then we'll compare each piece to the real SCR in Karaf.

Once you see SCR implemented in about 150–200 lines of ordinary Java, you'll realize there's no magic at all—just a runtime applying metadata and making normal Java method calls. That's often the point where OSGi "clicks."


# Rather than explaining SCR conceptually, we can build our own miniature version of SCR in plain Java.
This is a brilliant idea. There is no better way to demystify "framework magic" than to build it yourself from scratch.

By writing our own Mini-SCR in plain Java, we can see exactly how Inversion of Control, Dependency Injection, and the Component Lifecycle work using standard **Java Reflection**—no Karaf or OSGi libraries required!

Here is how we can build a working miniature SCR in under 100 lines of code.

1. **Create the Custom Annotations:**
First, we need to define the markers that tell our engine what to do. These replace the standard `@Component`, `@Reference`, and `@Activate` annotations. We set their retention to `RUNTIME` so we can read them using Reflection.

```java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MiniComponent {}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MiniReference {}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MiniActivate {}

```


2. **Write the Business Logic (Provider and Consumer):**
Next, we write the exact same Provider and Consumer logic from our OSGi lab, but using our custom annotations. Notice how the Consumer has a private, uninitialized field for the interface.

```java
public interface GreetingService {
    String sayHello();
}

@MiniComponent
public class Provider implements GreetingService {
    public String sayHello() {
        return "Hello from the Mini-Provider!";
    }
}

@MiniComponent
public class Consumer {
    // We expect the Mini-SCR to inject this memory pointer
    @MiniReference
    private GreetingService greetingService;

    @MiniActivate
    public void start() {
        System.out.println("[CONSUMER ACTIVE] Calling service: " + greetingService.sayHello());
    }
}

```


3. **Build the Mini-SCR Engine (The Scrub Nurse):**
This is the core engine. It manages a `registry` (a map of active services). When we ask it to deploy a class, it uses Reflection to bypass Java's normal OOP access rules to wire everything together.

```java
import java.lang.reflect.*;
import java.util.*;

public class MiniSCR {
    // The "OSGi Service Registry"
    private Map<Class<?>, Object> registry = new HashMap<>();

    public void deployBundle(Class<?> clazz) throws Exception {
        if (!clazz.isAnnotationPresent(MiniComponent.class)) return;

        System.out.println(">> SCR: Attempting to deploy " + clazz.getSimpleName());

        // 1. INSTANTIATE: Create the object
        Object instance = clazz.getDeclaredConstructor().newInstance();

        // 2. DISCOVER & INJECT: Look for @MiniReference
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(MiniReference.class)) {
                Object dependency = registry.get(field.getType());
                
                if (dependency == null) {
                    System.out.println("   [ERROR] UNSATISFIED: Missing " + field.getType().getSimpleName());
                    System.out.println("   [ERROR] Halting deployment.\n");
                    return; // Component stays unsatisfied
                }
                
                // Reflection Magic: Force injection into a private field!
                field.setAccessible(true); 
                field.set(instance, dependency);
                System.out.println("   [WIRED] Injected " + field.getType().getSimpleName());
            }
        }

        // 3. ACTIVATE: Look for @MiniActivate and run it
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(MiniActivate.class)) {
                method.setAccessible(true);
                method.invoke(instance);
            }
        }

        // 4. PUBLISH: Add this instance to the registry for others to use
        for (Class<?> iface : clazz.getInterfaces()) {
            registry.put(iface, instance);
        }
        registry.put(clazz, instance); // Register by its own class type too
        
        System.out.println(">> SCR: Successfully deployed " + clazz.getSimpleName() + "\n");
    }
}

```


4. **Run the Simulation:**
Now, let's create a `Main` class to act as our Apache Karaf container. We will try to deploy the Consumer *before* the Provider to see our framework's dependency checking in action.

```java
public class Main {
    public static void main(String[] args) throws Exception {
        MiniSCR karaf = new MiniSCR();

        // 1. Try to deploy the Consumer first (It should fail gracefully)
        karaf.deployBundle(Consumer.class);

        // 2. Deploy the Provider (It has no dependencies, so it succeeds)
        karaf.deployBundle(Provider.class);

        // 3. Try deploying the Consumer again (Now it should wire and activate!)
        karaf.deployBundle(Consumer.class);
    }
}

```


### The Output

If you compile and run this plain Java code, your terminal will print:

```text
>> SCR: Attempting to deploy Consumer
   [ERROR] UNSATISFIED: Missing GreetingService
   [ERROR] Halting deployment.

>> SCR: Attempting to deploy Provider
>> SCR: Successfully deployed Provider

>> SCR: Attempting to deploy Consumer
   [WIRED] Injected GreetingService
[CONSUMER ACTIVE] Calling service: Hello from the Mini-Provider!
>> SCR: Successfully deployed Consumer

```

By building this, you can physically see the "magic" of Inversion of Control. Your `Consumer` class never called `new Provider()`. It never managed its own dependencies. The `MiniSCR` engine used `field.setAccessible(true)` to literally open up the `Consumer` object and drop the `Provider` pointer inside it before turning it on.


# ChatGPT example:
I think this is the single best way to understand SCR. We're going to forget about Karaf, OSGi, annotations, and OpenNMS for a moment and build the core idea ourselves.

By the end, you'll realize that **SCR is essentially a very sophisticated object manager**.

---

# Phase 1: Ordinary Java (No SCR)

Suppose we have this interface.

```java
public interface ServiceMonitor {
    void poll();
}
```

An implementation:

```java
public class SnmpMonitor implements ServiceMonitor {

    @Override
    public void poll() {
        System.out.println("Polling SNMP...");
    }
}
```

And our daemon:

```java
public class PollerDaemon {

    private final List<ServiceMonitor> monitors = new ArrayList<>();

    public void addMonitor(ServiceMonitor monitor) {
        monitors.add(monitor);
    }

    public void run() {
        for (ServiceMonitor monitor : monitors) {
            monitor.poll();
        }
    }
}
```

Main program:

```java
public static void main(String[] args) {

    PollerDaemon daemon = new PollerDaemon();

    SnmpMonitor snmp = new SnmpMonitor();

    daemon.addMonitor(snmp);

    daemon.run();
}
```

Everything is straightforward.

```text
main()

   │

   ├── new PollerDaemon()

   ├── new SnmpMonitor()

   └── daemon.addMonitor(snmp)
```

The **main method is acting as the manager**.

---

# Phase 2: What if plugins appear later?

Suppose five minutes later a new plugin appears.

```java
HttpMonitor http = new HttpMonitor();
```

Who tells PollerDaemon?

Right now **main()** must do it.

```java
daemon.addMonitor(http);
```

But imagine plugins can appear at any time.

Who watches for them?

This is where SCR comes in.

---

# Phase 3: Let's invent a Service Registry

Instead of PollerDaemon knowing every monitor, let's build a registry.

```java
public class ServiceRegistry {

    private final List<ServiceMonitor> monitors = new ArrayList<>();

    public void register(ServiceMonitor monitor) {
        monitors.add(monitor);
    }

    public List<ServiceMonitor> getMonitors() {
        return monitors;
    }
}
```

Now

```java
registry.register(new SnmpMonitor());
```

instead of

```java
daemon.addMonitor(...)
```

Current picture:

```text
             Registry

           +-----------+
           |  SNMP     |
           +-----------+
```

PollerDaemon doesn't know the registry exists yet.

---

# Phase 4: PollerDaemon asks the Registry

Instead of manually calling addMonitor()

we write

```java
public class PollerDaemon {

    public PollerDaemon(ServiceRegistry registry) {

        for(ServiceMonitor monitor : registry.getMonitors()) {
            addMonitor(monitor);
        }
    }

    ...
}
```

Now

```text
Registry

↓

returns monitors

↓

PollerDaemon
```

This is already dependency injection.

---

# But there is still a problem

Suppose later

```java
registry.register(new HttpMonitor());
```

PollerDaemon never finds out.

We need notifications.

---

# Phase 5: Let's invent listeners

We'll create a new interface.

```java
public interface RegistryListener {

    void serviceAdded(ServiceMonitor monitor);

}
```

Now Registry stores listeners.

```java
public class ServiceRegistry {

    private List<ServiceMonitor> monitors = new ArrayList<>();

    private List<RegistryListener> listeners = new ArrayList<>();

    ...
}
```

Now add

```java
public void addListener(RegistryListener listener) {

    listeners.add(listener);

}
```

---

Now modify register()

```java
public void register(ServiceMonitor monitor) {

    monitors.add(monitor);

    for(RegistryListener listener : listeners) {
        listener.serviceAdded(monitor);
    }

}
```

Now every listener gets notified.

---

# PollerDaemon becomes a listener

```java
public class PollerDaemon
        implements RegistryListener {

    private List<ServiceMonitor> monitors
            = new ArrayList<>();

    @Override
    public void serviceAdded(ServiceMonitor monitor) {

        addMonitor(monitor);

    }

}
```

Notice something incredible.

The registry is now doing

```java
listener.serviceAdded(snmpMonitor);
```

Exactly like SCR later does

```java
addMonitor(snmpMonitor);
```

---

# Runtime

```text
main()

      │

creates Registry

      │

creates PollerDaemon

      │

registry.addListener(daemon)

      │

register(new SnmpMonitor())

      │

Registry notifies listeners

      │

daemon.serviceAdded(snmp)

      │

daemon.addMonitor(snmp)
```

We're getting very close to SCR.

---

# Phase 6: We don't like serviceAdded()

We'd rather call

```java
addMonitor()
```

because that's our existing method.

Easy.

Instead of

```java
listener.serviceAdded(...)
```

the registry could simply call

```java
listener.addMonitor(...)
```

But how does it know the method name?

Reflection.

---

Suppose Registry receives

```java
Object component
```

instead of

```java
RegistryListener
```

It can do

```java
Method method =
    component.getClass()
             .getMethod(
                 "addMonitor",
                 ServiceMonitor.class);
```

Then

```java
method.invoke(component, monitor);
```

That is almost exactly what SCR does.

---

# We're Missing One Thing

Right now we still write

```java
registry.addListener(daemon);
```

SCR doesn't require that.

How?

Annotations.

Instead of

```java
registry.addListener(...)
```

SCR scans

```java
@Component
```

and automatically registers the component.

Likewise

```java
@Reference(bind="addMonitor")
```

tells SCR

> When a ServiceMonitor appears,
> invoke addMonitor().

---

# Congratulations...

At this point, we've recreated the core ideas behind SCR:

```text
                Mini SCR

          +------------------+
          | Service Registry |
          +------------------+
                    │
     register(ServiceMonitor)
                    │
                    ▼
          Notify interested components
                    │
         Reflection invokes method
                    │
                    ▼
      PollerDaemon.addMonitor(...)
```

No inheritance.

No parent/child relationship.

Just:

* a registry,
* listeners,
* reflection,
* metadata.

---

# What's Next?

We've intentionally cheated in one place: **`main()` still creates everything**.

```java
PollerDaemon daemon = new PollerDaemon();
ServiceRegistry registry = new ServiceRegistry();
```

The real SCR doesn't even require that. It discovers components, creates them, wires them together, and manages their lifecycle automatically.

I suggest we continue by building **MiniSCR v2**, where we'll eliminate `main()` almost entirely. We'll introduce our own `@Component` and `@Reference` annotations and write a tiny container that uses reflection to discover and wire components automatically. That will bring us remarkably close to how Apache Felix SCR actually works.



# Example 2

Excellent. This is where OSGi starts becoming much less mysterious.

We're going to build a tiny framework—not an application.

At the end, you'll realize that **SCR is just another Java program** that happens to create and wire other Java objects.

Our goal is to reach this point:

```java
@Component
public class PollerDaemon {

    @Reference
    private List<ServiceMonitor> monitors;

}
```

without writing

```java
new PollerDaemon();
new SnmpMonitor();
daemon.addMonitor(...);
```

Our MiniSCR will do everything.

---

# Step 0 – What are we building?

Imagine we are writing our own tiny version of Spring or OSGi.

```text
                     MiniSCR

            +----------------------+
            | Component Scanner    |
            +----------------------+
                       │
                       ▼
            +----------------------+
            | Object Factory       |
            +----------------------+
                       │
                       ▼
            +----------------------+
            | Service Registry     |
            +----------------------+
                       │
                       ▼
            +----------------------+
            | Dependency Injector  |
            +----------------------+
```

Every one of these boxes is just an ordinary Java class.

---

# Step 1 – Create our annotations

Let's invent our own annotations.

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
}
```

Why `RetentionPolicy.RUNTIME`?

Because annotations normally disappear after compilation.

We want Java Reflection to see them.

```
Source Code

@Component
class PollerDaemon

        │
        ▼

Compiled

@Component still exists

        │
        ▼

Reflection can read it
```

---

Now create another annotation.

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Reference {
}
```

Notice something.

These annotations do absolutely nothing.

Zero.

They are only labels.

Like this:

```
Box

+-----------------+
|   Fragile       |
+-----------------+
```

The sticker doesn't protect the box.

Someone must read the sticker.

That someone will be MiniSCR.

---

# Step 2 – Create some components

```java
public interface ServiceMonitor {

    void poll();

}
```

---

```java
@Component
public class SnmpMonitor implements ServiceMonitor {

    @Override
    public void poll() {

        System.out.println("SNMP");

    }

}
```

---

Now PollerDaemon.

```java
@Component
public class PollerDaemon {

    @Reference
    private List<ServiceMonitor> monitors;

    public void run() {

        for(ServiceMonitor monitor : monitors){

            monitor.poll();

        }

    }

}
```

Notice something incredible.

There is

```java
new ArrayList()
```

nowhere.

The field is null.

```
PollerDaemon

monitors

↓

null
```

Who fills it?

MiniSCR.

---

# Step 3 – Create the container

Let's build the simplest container imaginable.

```java
public class MiniSCR {

    private Map<Class<?>, Object> components
            = new HashMap<>();

}
```

This map is the heart of everything.

Initially

```
components

(empty)
```

---

# Step 4 – Register components

We'll cheat slightly.

Instead of scanning packages, we'll register manually.

```java
public void register(Class<?> clazz)
```

Inside:

```java
Object object =
        clazz.getDeclaredConstructor()
             .newInstance();

components.put(clazz, object);
```

What happened?

Reflection executed

```java
new SnmpMonitor()
```

without us writing

```java
new SnmpMonitor()
```

The picture becomes

```
MiniSCR

components

+-----------------------------+
| SnmpMonitor → instance      |
+-----------------------------+
```

Register another.

```
MiniSCR

components

+-----------------------------+
| SnmpMonitor → object        |
| PollerDaemon → object       |
+-----------------------------+
```

Notice

MiniSCR created both objects.

Exactly what SCR does.

---

# Step 5 – Inject dependencies

Now comes the magic.

MiniSCR loops through every object.

```
SnmpMonitor

PollerDaemon
```

For every field

it asks

```
Does this field have

@Reference ?
```

Reflection lets us inspect fields.

```java
for(Field field : clazz.getDeclaredFields()){
```

Suppose it finds

```java
@Reference
private List<ServiceMonitor> monitors;
```

MiniSCR now knows

```
This field must be injected.
```

---

# Step 6 – Find matching services

MiniSCR asks

```
Which objects implement

ServiceMonitor ?
```

It loops over every object.

```
components

↓

SnmpMonitor

↓

instanceof ServiceMonitor ?

YES
```

Now build

```java
List<ServiceMonitor> services =
        List.of(snmpMonitor);
```

---

# Step 7 – Inject using Reflection

This is the part everyone imagines is magic.

It isn't.

MiniSCR simply does

```java
field.setAccessible(true);

field.set(pollerDaemon,
          services);
```

That's it.

Exactly one line.

Before

```
PollerDaemon

monitors

↓

null
```

After

```
PollerDaemon

monitors

↓

+-----------+
| SNMP      |
+-----------+
```

No constructor.

No setter.

Reflection changed the field.

---

# Step 8 – Run

Now

```java
PollerDaemon daemon =
        miniSCR.get(PollerDaemon.class);

daemon.run();
```

Output

```
SNMP
```

---

# Let's visualize everything

```
                MiniSCR

         register(SnmpMonitor)

                 │

                 ▼

         new SnmpMonitor()

                 │

                 ▼

        Store in component map

                 │

────────────────────────────────────

         register(PollerDaemon)

                 │

                 ▼

       new PollerDaemon()

                 │

                 ▼

      PollerDaemon.monitors = null

────────────────────────────────────

          Dependency Injection

                 │

                 ▼

    Find @Reference field

                 │

                 ▼

Find every ServiceMonitor instance

                 │

                 ▼

Reflection

field.set(...)

                 │

                 ▼

PollerDaemon.monitors

↓

[SnmpMonitor]
```

---

# Compare this to the real SCR

| Our MiniSCR          | Real Apache Felix SCR                                         |
| -------------------- | ------------------------------------------------------------- |
| `register(Class<?>)` | Discovers `@Component` from bundle metadata                   |
| `newInstance()`      | Creates component instances                                   |
| `Map<Class,Object>`  | Component registry                                            |
| Loop through fields  | Reads `@Reference` metadata                                   |
| `field.set()`        | Injects references                                            |
| Reflection           | Reflection (plus generated metadata and lifecycle management) |

We're already about **50% of the way** to understanding the real SCR.

---

# Where our MiniSCR still differs from the real SCR

The biggest missing piece is **dynamic behavior**.

Right now, our MiniSCR injects dependencies **once**:

```
Start application
      │
      ▼
Create objects
      │
      ▼
Inject dependencies
      │
      ▼
Finished
```

The real SCR never stops watching. It continuously monitors the service registry:

```
Bundle starts
      │
      ▼
New ServiceMonitor appears
      │
      ▼
Registry publishes event
      │
      ▼
SCR updates PollerDaemon
      │
      ▼
addMonitor(newMonitor)

Bundle stops
      │
      ▼
removeMonitor(oldMonitor)
```

That continuous observation and updating is what transforms our simple dependency injector into a dynamic runtime like OSGi.

## MiniSCR v3 Proposal

The next step is to replace our passive `Map<Class<?>, Object>` with a real **Service Registry** that publishes **service registration and removal events**. Our `MiniSCR` will subscribe to those events and invoke `addMonitor()`/`removeMonitor()` dynamically—just like Apache Felix SCR does. At that point, you'll have built a simplified but recognizable version of the actual OSGi Declarative Services engine.

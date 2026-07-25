![alt text](image.png)



Felix = OSGI specification implementation 
Karaf translates our commands (OS) and hands them to Felix (Kernel) to execute.

It is an open-source project by the Apache Software Foundation that implements the OSGi Core Framework specification,
that actually runs the OSGi bundles.

Apache Karaf (the container we use in OpenNMS) is built around Apache Felix. 
Felix provides the core wiring, and Karaf wraps it in a nice shell with logging, SSH, and feature management.

If Karaf is the operating system, then Felix is the kernel.

1. Zero-Downtime Hot Swapping (What you just learned)
2. Dependency Isolation (The "Invisible Shields")
3. Distributed Monitoring (Minions & Sentinels)


Analogy: Connect it back to the "aquarium" and "car engine" analogies used previously to maintain consistency. 
(OSGi = the laws of physics/rulebook; Felix = the actual water/engine; Karaf = the aquarium glass/car chassis).

Karaf uses Felix under the hood to handle the actual classloading, bundle lifecycle (Install, Start, Stop), and Service Registry.

Features: Mention its footprint (very small) and its core responsibilities.


Core Responsibilities:
Lifecycle Management: Starts, stops, installs, and uninstalls the .jar bundles without rebooting the JVM.
Classloading Isolation: Creates the "invisible shields" around each bundle so they don't clash.
Service Registry: Acts as the phonebook where bundles register and discover services (like your SshService).

We don't interact with it directly: 
We interact with Karaf. Karaf is just a wrapper around Felix that provides the nice SSH console, logging, and feature:install commands. Underneath, Karaf translates our commands and hands them to Felix to execute.


OSGi is the laws of physics (the rules).
Apache Felix is the water and gravity that actually enforce the rules.
Apache Karaf is the glass tank, the filter, and the lighting system built around the water.
Your plugins are the fish.


Inside your OpenNMS / Karaf environment, Apache Felix sits right on top of the Java Virtual Machine (JVM) and is responsible for three critical jobs:

The Lifecycle Manager: When you type bundle:install or bundle:start in the Karaf console, Karaf hands that request down to Felix. Felix is the engine that physically loads your .jar file into memory, checks its dependencies, and starts it up without restarting the JVM.

The Invisible Shields (Classloaders): Felix is the bouncer that reads the MANIFEST.MF in your .jar file. It physically builds the isolated ClassLoader walls around your plugin so that your libraries don't clash with OpenNMS's libraries.

The Service Registry: Felix manages the internal "phonebook" where bundles register themselves. When your BackupRestore plugin asks for an SshService, Felix looks up who provides it and wires them together.

Apache Felix is incredibly low-level and bare-bones. It has almost no user interface, no advanced logging, and no SSH server.
If you just ran raw Apache Felix, deploying a plugin would be a nightmare of typing massive filesystem paths and manually resolving dozens of dependencies one by one.

Apache Karaf was created to wrap Apache Felix in a user-friendly enterprise container. Karaf gives you the nice karaf@root> SSH console, the features.xml dependency resolver, and the hot-deploy folders, while letting Apache Felix quietly do the heavy lifting of class isolation in the background.
Apache Felix is the OSGi framework implementation that actually runs your OSGi bundles.
Apache Karaf (the container we use in OpenNMS) is built around Apache Felix. Felix provides the core wiring, and Karaf wraps it in a nice shell with logging, SSH, and feature management.

If Karaf is the operating system, then Felix is the kernel.

Apache Karaf: Commands, Features, Config, Shell
Pollerd -> Karaf -> Felix -> JVM

Analogy: Connect it back to the "aquarium" and "car engine" analogies used previously to maintain consistency. 
(OSGi = the laws of physics/rulebook; Felix = the actual water/engine; Karaf = the aquarium glass/car chassis).

Karaf uses Felix under the hood to handle the actual classloading, bundle lifecycle (Install, Start, Stop), and Service Registry.

Features: Mention its footprint (very small) and its core responsibilities.

We interact with Karaf. Karaf is just a wrapper around Felix that provides the nice SSH console, logging, and feature:install commands. Underneath, Karaf translates our commands and hands them to Felix to execute.

OSGi is the laws of physics (the rules).
Apache Felix is the water and gravity that actually enforce the rules.
Apache Karaf is the glass tank, the filter, and the lighting system built around the water.
Your plugins are the fish.
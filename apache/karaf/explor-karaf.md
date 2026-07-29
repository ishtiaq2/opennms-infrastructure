# The difference between a JAR and a KAR comes down to Single Component vs. Complete Delivery Box:

* JAR (Java ARchive / OSGi Bundle): Contains one single module or plugin.
* KAR (Karaf ARchive): A zip package designed specifically for Apache Karaf/OpenNMS that bundles multiple JARs, configuration 
  files, and feature blueprints into a single deployment file.

# If you change the extension of a .kar file to .zip and extract it, you will see a specific folder structure:

my-feature.kar
├── META-INF/
│   └── MANIFEST.MF
└── repository/                              <-- A embedded Maven repository!
    └── com/
        └── example/
            ├── my-feature/
            │   └── 1.0.0/
            │       └── my-feature-features.xml  <-- Tells Karaf how to install everything
            ├── provider/
            │   └── 1.0.0/
            │       └── provider-1.0.0.jar       <-- Bundle 1
            └── consumer/
                └── 1.0.0/
                    └── consumer-1.0.0.jar       <-- Bundle 2

# Why use a KAR instead of dropping multiple JARs?
Self-Contained Offline Deployment:

If your OpenNMS server has no internet access, dropping a single .jar that depends on 10 third-party libraries (like Jackson, Slf4j, or Commons-Lang) will fail because Karaf can't download the dependencies. A .kar file packages all dependencies inside its embedded repository/ folder.

## Automatic Configuration (/etc files):
* A .kar file can include a repository/.../my-config.cfg file. When you drop the .kar into /usr/share/opennms/deploy, Karaf 
  unpacks the JARs into its runtime cache and automatically drops the .cfg file directly into Karaf's etc/ folder.

## Atomic Feature Management:
* Instead of running bundle:install for 5 separate JAR files manually, dropping a KAR registers a Karaf Feature. Uninstalling the feature cleanly stops and removes all associated bundles simultaneously.

Summary
Think of a JAR as an individual brick, and a KAR as a pre-packaged pallet containing the bricks, the mortar, and the instructions to build a room inside Apache Karaf.



# Command                     What you should look for

bundle:list                 Shows all active user-level OSGi bundles. Notice they all have an ID, State (Active), and Version.
feature:list                Shows the massive library of available features. Most are uninstalled.
feature:list | grep -i web  Filters the list. You will see features like http and http-whiteboard which OpenNMS uses for REST APIs.
log:tail                    Streams the live system logs. Press Ctrl+C to exit the log view and return to the prompt.


# Phase 3: Expose Apache Felix (The Kernel)
  * # bundle:list -t 0

By default, Karaf hides the core framework (Felix) so you don't accidentally break it. Let's peel back the curtain.Run this command to show system threshold bundles:
* # bundle:list -t 0

What you are seeing:
* ID 0 (System Bundle, Lvl 0): This is the Apache Felix Kernel. Notice how it is the only bundle at Start Level 0. It booted first,   
  created the universe, and then started loading the rest of the bundles.
* ID 4 & 5 (Pax Logging, Lvl 8): Karaf loads the logging system very early so that if anything else fails, it can log the error.
* ID 13 (Configuration Admin, Lvl 10): The engine that reads properties files and passes them to bundles.
* ID 45 & 46 (Karaf Shell & SSH, Lvl 30): This is Karaf giving you the actual karaf@root()> prompt and allowing you to connect via SSH!

Now, let's look at Felix's "Invisible Shields" (Classloader isolation):
* # bundle:headers 0

  This prints the MANIFEST.MF metadata for the Felix framework. If you run bundle:headers on any other ID, you will see exactly which Java packages that specific bundle is allowed to Import and Export.Phase 4: The "Hot Deploy" MagicLet's test Felix's ability to instantly wire up new components without restarting. We will use a simple Blueprint XML file.Open a second terminal window (leave your Karaf shell running in the first one) and SSH into your CentOS VM.

# 1.Tail the Logs in Karaf:
  In your first terminal (inside the Karaf shell), start watching the live logs:
  * # log:tail

# 2.Create a Blueprint File:
  In your second terminal (the standard CentOS bash prompt), create a simple XML file inside Karaf's deploy folder.
  cat < ~/apache-karaf-4.4.11/deploy/hello-world.xml EOF

# 3. Watch Felix React:
  Look immediately at your first terminal. You will see Karaf intercept the file and hand it to Felix, which dynamically wires the XML into a running OSGi bundle.You should see a log line similar to:Blueprint bundle hello-world.xml has been started

# 4. Verify the Bundle:
  Press Ctrl+C to stop tailing the logs, then run:Plaintextbundle:list
  Look at the very bottom of the list. Your hello-world.xml file was dynamically wrapped into a bundle, assigned an ID, and moved to the Active state!
  
  To remove it, simply delete the file from the deploy directory in your second terminal (rm ~/apache-karaf-4.4.11/deploy/hello-world.xml). Felix will instantly detect the deletion and uninstall the bundle.
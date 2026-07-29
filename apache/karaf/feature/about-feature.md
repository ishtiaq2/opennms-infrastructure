To group your provider and consumer bundles into a single installable unit, 
you write a Karaf Feature XML file (traditionally named feature.xml or features.xml).

When you register this XML file with Apache Karaf or OpenNMS, Karaf reads the instructions and 
handles downloading, installing, and starting all declared bundles in the correct dependency order.

Here is how to structure a complete feature.xml file for your OSGi bundles.

1. The feature.xml Template
Create a file named feature.xml with the following content:

<?xml version="1.0" encoding="UTF-8"?>
<features name="osgi-lab-features-1.0.0" 
          xmlns="http://karaf.apache.org/xmlns/features/v1.6.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://karaf.apache.org/xmlns/features/v1.6.0 
                              http://karaf.apache.org/xmlns/features/v1.6.0">

    <!-- Define your feature -->
    <feature name="osgi-lab-suite" version="1.0.0" description="OSGi Lab Provider and Consumer Suite">
        
        <!-- 1. PREREQUISITE: Ensure Karaf's Declarative Services (SCR) runtime is loaded -->
        <feature>scr</feature>

        <!-- 2. BUNDLE 1: Provider Bundle -->
        <!-- start-level="80" ensures core infrastructure starts before business bundles -->
        <bundle start-level="80" start="true">
            mvn:com.example.osgi/provider/1.0.0
        </bundle>

        <!-- 3. BUNDLE 2: Consumer Bundle -->
        <bundle start-level="80" start="true">
            mvn:com.example.osgi/consumer/1.0.0
        </bundle>

    </feature>

</features>


# It is not a strict, hard-coded rule like src/main/java.
* Unlike Java source code, Maven does not have a built-in default rule that 
  says "all Karaf features must live in src/main/feature."
  The only reason it needs to be there right now is because we explicitly told Maven to 
  look there inside your osgi-lab-kar/pom.xml.

If you look at the <build> section of your osgi-lab-kar/pom.xml, you will see this block:
<build>
    <resources>
        <resource>
            <directory>src/main/feature</directory> <!-- THIS IS THE LINK -->
            <filtering>true</filtering>
            <includes>
                <include>feature.xml</include>
            </includes>
        </resource>
    </resources>
    ...

# Can you put it somewhere else?
Yes! If you prefer standard Maven conventions, you could put 
your feature.xml inside src/main/resources/ instead.

To do that, you would simply change the POM to match:
<directory>src/main/resources</directory>

Why use src/main/feature then?
In the Karaf/OSGi community, using src/main/feature/ is a widely accepted best practice.

Because a .kar packaging project doesn't usually contain Java code anyway, creating a dedicated feature/ folder makes it immediately obvious to other developers that this module is dedicated strictly to Karaf feature descriptors, keeping it separate from standard properties or configuration files you might put in resources/.
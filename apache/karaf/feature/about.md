To group your provider and consumer bundles into a single installable unit, you write a Karaf Feature XML file (traditionally named feature.xml or features.xml).

When you register this XML file with Apache Karaf or OpenNMS, Karaf reads the instructions and handles downloading, installing, and starting all declared bundles in the correct dependency order.

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
        <feature version="[4,5)">scr</feature>

        <!-- 2. BUNDLE 1: Provider Bundle -->
        <!-- start-level="80" ensures core infrastructure starts before business bundles -->
        <bundle start-level="80" start="true">
            mvn:com.example.ishtiaq/provider/1.0.0
        </bundle>

        <!-- 3. BUNDLE 2: Consumer Bundle -->
        <bundle start-level="80" start="true">
            mvn:com.example.osgi/consumer/1.0.0
        </bundle>

    </feature>

</features>
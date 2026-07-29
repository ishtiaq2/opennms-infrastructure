Here is a complete, production-ready README.md file that documents the architecture, directory structure, build instructions, deployment process, and troubleshooting steps for your OSGi Provider-Consumer KAR project in OpenNMS / Apache Karaf.

OpenNMS OSGi Lab: Provider, Consumer & KAR Packaging
This project demonstrates how to build modular OSGi Declarative Services (DS) bundles and package them into a single Karaf ARchive (.kar) for automated deployment inside OpenNMS or Apache Karaf.

# 🏗 Architecture Overview
* The suite consists of three distinct Maven modules:

plugin/
├── provider/          # Exports the GreetingService interface & registers Provider implementation
├── consumer/          # Injects GreetingService via @Reference and invokes it on activation
└── osgi-lab-kar/      # Bundles Provider, Consumer, and feature.xml into a single .kar package


+-----------------------------------------------------------------------------------+
|                              osgi-lab-kar-1.0.0.kar                               |
|                                                                                   |
|   +-----------------------+     Dependency Injection     +--------------------+   |
|   |    Provider Bundle    | <--------------------------- |  Consumer Bundle   |   |
|   | com.example.osgi  |      via @Reference          | com.example.osgi   |   |
|   +-----------------------+                              +--------------------+   |
|               |                                                     |             |
|               +-----------------> feature.xml <---------------------+             |
+-----------------------------------------------------------------------------------+

* Provider (com.example.osgi:provider): Defines the GreetingService interface and exports package com.
  example.provider. Registers an OSGi component using @Component.

* Consumer (com.example.osgi:consumer): Uses Declarative Services (@Reference) to dynamically bind the 
  GreetingService instance without using standard class instantiation (new). Logs messages via SLF4J.

* KAR Package (com.example.osgi:osgi-lab-kar): Packages both bundles and a feature.xml descriptor using 
  karaf-maven-plugin.

#  📋 Prerequisites
*  JDK: Java 17
*  Apache Maven: 3.8+
*  Target Runtime: OpenNMS 33+ or Apache Karaf 4.3+

# 🛠 Configuration Files
*  1. Provider pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.osgi</groupId>
    <artifactId>provider</artifactId>
    <version>1.0.0</version>
    <packaging>bundle</packaging>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.osgi</groupId>
            <artifactId>org.osgi.service.component.annotations</artifactId>
            <version>1.4.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-bundle-plugin</artifactId>
                <version>5.1.9</version>
                <extensions>true</extensions>
                <configuration>
                    <instructions>
                        <Export-Package>com.example.provider</Export-Package>
                    </instructions>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>

# 2. Consumer pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.osgi</groupId>
    <artifactId>consumer</artifactId>
    <version>1.0.0</version>
    <packaging>bundle</packaging>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.osgi</groupId>
            <artifactId>org.osgi.service.component.annotations</artifactId>
            <version>1.4.0</version>
            <scope>provided</scope>
        </dependency>
        
        <dependency>
            <groupId>com.example.osgi</groupId>
            <artifactId>provider</artifactId>
            <version>1.0.0</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.36</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-bundle-plugin</artifactId>
                <version>5.1.9</version>
                <extensions>true</extensions>
            </plugin>
        </plugins>
    </build>
</project>


# 3. Feature Descriptor (osgi-lab-kar/src/main/feature/feature.xml)
<?xml version="1.0" encoding="UTF-8"?>
<features name="osgi-lab-features-1.0.0" 
          xmlns="http://karaf.apache.org/xmlns/features/v1.6.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://karaf.apache.org/xmlns/features/v1.6.0 
                              http://karaf.apache.org/xmlns/features/v1.6.0">

    <feature name="osgi-lab-suite" version="1.0.0" description="OSGi Lab Provider and Consumer Suite">
        <!-- Prerequisite: Karaf Declarative Services (SCR) engine -->
        <feature>scr</feature>

        <!-- Bundle 1: Provider -->
        <bundle start-level="80" start="true">
            mvn:com.example.osgi/provider/1.0.0
        </bundle>

        <!-- Bundle 2: Consumer -->
        <bundle start-level="80" start="true">
            mvn:com.example.osgi/consumer/1.0.0
        </bundle>
    </feature>

</features>

# 4. KAR Module pom.xml (osgi-lab-kar/pom.xml)
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.osgi</groupId>
    <artifactId>osgi-lab-kar</artifactId>
    <version>1.0.0</version>
    <packaging>kar</packaging>

    <properties>
        <karaf.version>4.3.10</karaf.version>
        <karaf.plugin.version>4.3.10</karaf.plugin.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.example.osgi</groupId>
            <artifactId>provider</artifactId>
            <version>1.0.0</version>
        </dependency>
        <dependency>
            <groupId>com.example.osgi</groupId>
            <artifactId>consumer</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>

    <build>
        <resources>
            <resource>
                <directory>src/main/feature</directory>
                <filtering>true</filtering>
                <includes>
                    <include>feature.xml</include>
                </includes>
            </resource>
        </resources>

        <plugins>
            <plugin>
                <groupId>org.apache.karaf.tooling</groupId>
                <artifactId>karaf-maven-plugin</artifactId>
                <version>${karaf.plugin.version}</version>
                <extensions>true</extensions>
                <executions>
                    <execution>
                        <id>features-generate-descriptor</id>
                        <phase>generate-resources</phase>
                        <goals>
                            <goal>features-generate-descriptor</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>


# 🚀 Build Instructions
  Execute the Maven builds in sequence so that artifacts are installed into your local ~/.m2/repository before the KAR archive is generated:

## Step 1: Compile and install Provider
cd osgi-lab/provider
mvn clean install

## Step 2: Compile and install Consumer
cd ../consumer
mvn clean install

## Step 3: Build the KAR archive
cd ../osgi-lab-kar
mvn clean install

* Upon successful build, the KAR file will be located at:
osgi-lab-kar/target/osgi-lab-kar-1.0.0.kar


# 📦 Deployment
  Deploy to OpenNMS / Karaf
  Copy the assembled .kar file directly into the deployment directory monitored by FileInstall:
  * scp /tmp/opennms-infra/apache/karaf/feature/plugin/osgi-lab-kar/target/osgi-lab-kar-1.0.0.kar 
    maven-admin@192.168.0.33:/tmp/

  * podman cp /tmp/osgi-lab-kar-1.0.0.kar opennms:/opt/opennms/deploy/
  
  
# 🔍 Verification & Logs
## 1. Tail OpenNMS Karaf Logs
  Connect to the OpenNMS Karaf console and tail the log stream:
  ssh -p 8101 admin@localhost -> log:tail


## 2. Verify Component Wiring
  ### Inspect Declarative Services wiring directly in the shell:
  * admin@opennms()> scr:list
  * admin@opennms()> scr:details com.example.consumer.GreetingConsumer

# 🧹 Clean Redeployment Procedure
  If you need to perform a clean redeployment or remove the KAR feature from OpenNMS:
  ### Delete the KAR file from disk:
  podman exec opennms rm -f /opt/opennms/deploy/osgi-lab-kar-1.0.0.kar
  
  ### Uninstall feature wrappers in Karaf SSH shell:
  admin@opennms()> feature:uninstall osgi-lab-kar
  admin@opennms()> feature:uninstall osgi-lab-suite
  admin@opennms()> feature:repo-remove mvn:com.example.osgi/osgi-lab-kar/1.0.0/xml/features

  ### Verify clean state:  
  *admin@opennms()> list | grep -i example
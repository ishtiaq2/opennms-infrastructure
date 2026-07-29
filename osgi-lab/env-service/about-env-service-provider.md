The purpose of this lab is to create a standalone OSGI bundle that
reads system environment variables and provides to other plugins

Because it is built using standard Declarative Services (DS) and the maven-bundle-plugin, it can be compiled on its own and dropped directly into Karaf without needing a .kar file wrapper.

# Step 1: Project Structure
  Create a directory named env-provider and set up this exact folder structure:

env-service-provider/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── osgi/
                        └── env/
                            ├── EnvService.java
                            └── impl/
                                └── EnvServiceImpl.java
                                
# Step 2: The pom.xml

# Step 3 A: src/main/java/com/example/osgi/env/EnvService.java

# Step 3 B: src/main/java/com/example/osgi/env/impl/EnvServiceImpl.java

# Step 4: Build and Deploy:
* cd env-provider
* mvn clean install

* Deploy: 
  cp target/env-provider-1.0.0.jar /home/admin/karaf/ apache-karaf-4.4.11/deploy

# Step 5: Verify it is Running:
* karaf@opennms()> scr:list | grep -i env
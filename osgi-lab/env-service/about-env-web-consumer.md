env-web-consumer/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── mydomain/
                    └── env/
                        └── web/
                            └── EnvServlet.java

# pom.xml

# The Java Servlet Code

# Build and Deploy
* cd env-web-consumer
* mvn clean install
* scp target/env-web-consumer-1.0.0.jar admin@192.168.0.46://home/admin/karaf/apache-karaf-4.4.11/deploy

# Ensure Karaf's Web Server is active:
## ssh -p 8101 admin@localhost
* karaf@root()> feature:install http
* karaf@root()> feature:install http-whiteboard



# Test Locally First
* curl http://localhost:8181/env

# Check the Firewall (If connecting externally)
If 192.168.0.46 is a remote Linux VM and you are connecting from your laptop, the Linux firewall is likely blocking port 8181.

Run the appropriate command on the Karaf server (192.168.0.46) to open the port:
* sudo ufw allow 8181/tcp

# If using Firewalld (CentOS/RHEL/Fedora):
* sudo firewall-cmd --zone=public --add-port=8181/tcp --permanent
* sudo firewall-cmd --reload


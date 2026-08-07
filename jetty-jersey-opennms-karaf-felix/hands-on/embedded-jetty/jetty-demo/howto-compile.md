# Runs jetty in the java app

## The Traditional Way (Standalone Server)
Historically, web servers like Tomcat, Jetty, or GlassFish were standalone pieces of software installed on a server machine.

* You write your Java code.

* You package it into a zip-like archive called a WAR file (Web Application Archive).

* You drop that WAR file into a specific folder (like webapps/) inside the standalone server.

* You start the server, and the server loads your application.

* The relationship: 
The Server is the boss. It owns the process and runs your application.


## The Embedded Way (What you just did)

With embedded Jetty, you flip that relationship upside down.

You add Jetty to your pom.xml just like any other normal library (like a JSON parser or database driver).

You write a standard main() method.

You instantiate the server programmatically (Server server = new Server(8081);) and start it.

The relationship: Your Application is the boss. It owns the process and starts the server as an internal component.

## Almost all modern Java frameworks (like Spring Boot, Dropwizard, and Quarkus) use embedded web servers by default.

* Microservices & Docker: 
When you run an app in a Docker container, it is much easier to just run java -jar myapp.jar (which contains its own embedded server) than to configure a base image with a standalone Tomcat server and inject a WAR file into it.

* No Server Configuration: 
You don't have to worry about configuring XML files in a standalone server's installation directory. Everything is configured in your Java code.

* Easy Testing: 
You can start and stop the server programmatically inside a JUnit test to test your HTTP endpoints automatically.

***********************************************************************

# cd to dir where pom.xml is located
* cd jetty-demo 
* mvn clean compile exec:java -Dexec.mainClass="HelloJetty"

# Here is the exact breakdown of what each part does, executed in order from left to right:

* mvn: The command-line tool for Apache Maven.

* clean: Deletes the target/ directory. This removes any old, previously compiled files to guarantee you are starting with a completely fresh build.

* compile: Reads your pom.xml, downloads any missing dependencies, and compiles your .java source code into .class bytecode files (saving them inside the newly created target/classes/ directory).

* exec:java: Tells Maven to invoke a specific tool called the exec-maven-plugin. The :java part is the specific "goal" that executes a Java program inside the same Java Virtual Machine (JVM) that Maven is currently using.

* -Dexec.mainClass="HelloJetty": The -D flag is used to pass a system property to Maven. This specific property tells the exec plugin exactly which class contains your public static void main(String[] args) method so it knows where to start the program.

Simple: 
* mvn clean compile exec:java
While Servlets are powerful, writing complex REST APIs with them requires a lot of repetitive code to parse URLs, read JSON, and format responses.

This is where Jersey comes in. Jersey is the official reference implementation for JAX-RS (Jakarta RESTful Web Services). It allows you to build web APIs simply by adding annotations (like @GET and @Path) to standard Java methods.

Here is a hands-on tutorial to build a standalone REST API using Jersey 3 and its default embedded server, Grizzly.

OpenNMS relies heavily on Jetty, using Jetty as the embedded engine for your own tools keeps your tech stack consistent.

One of the massive benefits of using standard JAX-RS (Jersey) is that your API code does not care what server is running it. You will notice that the HelloResource.java file below is completely identical to the previous tutorial—only the pom.xml and the App.java bootstrapper change.


# Bootstrapper: 
Here we use JettyHttpContainerFactory to bridge Jersey's routing system with an embedded Jetty server instance.
Create src/main/java/com/example/App.java:

jersey-demo
        |
        |_____pom.xml                                  
        |
        |_____srce/main/java/com/example/HelloResource.java
        |
        |_____srce/main/java/com/example/App.java
        |
        |_____srce/main/java/com/example/json/Message.java


# Compile and run the application from your terminal:￼
￼
mvn clean compile exec:java

Once the Jetty server has spun up, test the endpoints in a new terminal tab:￼
￼
curl http://localhost:8080/api/hello
Output: Hello from Jersey running on Jetty!
￼
curl http://localhost:8080/api/greet/ishtiaq

# Return JSON using Jackson:

* Create a POJO (Plain Old Java Object)

sudo firewall-cmd --add-port=8080/tcp --permanent
sudo firewall-cmd --reload
package com.example;

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

public class App {
    public static final String BASE_URI = "http://0.0.0.0:8080/";

    public static void main(String[] args) throws Exception {
        // Tell Jersey to scan this package for any classes with @Path annotations
        final ResourceConfig rc = new ResourceConfig().packages("com.example");

        // Create and start the embedded Jetty server
        // (The factory automatically starts the server by default)
        final Server server = JettyHttpContainerFactory.createServer(URI.create(BASE_URI), rc);

        System.out.println("Jersey app started with embedded Jetty at " + BASE_URI);
        System.out.println("Press Ctrl+C to stop it.");

        // Keep the Java application running by joining the Jetty server thread
        server.join();
    }
}
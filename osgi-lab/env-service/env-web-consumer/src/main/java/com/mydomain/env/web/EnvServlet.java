package com.mydomain.env.web;

import com.mydomain.env.EnvService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * The 'property' block tells the OSGi HTTP Whiteboard to mount this 
 * Servlet at the URL path: http://localhost:8181/env
 */
@Component(
    service = Servlet.class,
    property = {
        "osgi.http.whiteboard.servlet.pattern=/env",
        "osgi.http.whiteboard.servlet.name=EnvServlet"
    }
)
public class EnvServlet extends HttpServlet {

    // Inject the EnvService running in Karaf
    @Reference
    private EnvService envService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Set response type to plain text
        resp.setContentType("text/plain");
        PrintWriter writer = resp.getWriter();

        // Query the provider bundle
        Map<String, String> envs = envService.getAllEnv();

        // Print variables to the web response
        writer.println("=== SYSTEM ENVIRONMENT VARIABLES ===");
        for (Map.Entry<String, String> entry : envs.entrySet()) {
            writer.println(entry.getKey() + "=" + entry.getValue());
        }
    }
}
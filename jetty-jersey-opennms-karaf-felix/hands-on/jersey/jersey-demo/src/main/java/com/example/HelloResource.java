package com.example;

import com.example.json.Message;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;


@Path("/api")
public class HelloResource {

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello() {
        return "Hello from Jersey running on Jetty!";
    }

    @GET
    @Path("/greet/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Message greetUser(@PathParam("name") String name) {
        return new Message("Welcome to the server!", name);
    }
}
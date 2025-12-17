package com.hiku.trailImport.controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.enterprise.context.RequestScoped;

@RequestScoped
@Path("/hello")
public class Hello {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello() {
        System.out.println("Hello endpoint was called");
        return "Hello, Peaks and Hikes Service!";
    }
}
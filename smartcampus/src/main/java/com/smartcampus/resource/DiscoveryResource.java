package com.smartcampus.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("/")
public class DiscoveryResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discover() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", "Smart Campus API");
        info.put("version", "1.0");
        info.put("description", "RESTful API for campus room and IoT sensor management");
        info.put("contact", "admin@smartcampus.ac.uk");
        Map<String, String> links = new LinkedHashMap<>();
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        info.put("resources", links);
        return Response.ok(info).build();
    }
}

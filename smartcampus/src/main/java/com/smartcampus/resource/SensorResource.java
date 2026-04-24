package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @Context
    private ResourceContext resourceContext;
    
    
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        var result = DataStore.getSensors().values().stream()
                .filter(s -> type == null || s.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
        return Response.ok(result).build();
    }
    
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null)
            return Response.status(400).entity("{\"message\":\"Sensor id is required\"}").build();
        if (sensor.getRoomId() != null) {
            Room room = DataStore.getRooms().get(sensor.getRoomId());
            if (room == null)
                throw new LinkedResourceNotFoundException(
                        "Referenced roomId '" + sensor.getRoomId() + "' does not exist.");
            room.getSensorIds().add(sensor.getId());
        }
        DataStore.getSensors().put(sensor.getId(), sensor);
        return Response.status(201).entity(sensor).build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.getSensors().get(sensorId);
        if (sensor == null)
            return Response.status(404).entity("{\"message\":\"Sensor not found: " + sensorId + "\"}").build();
        return Response.ok(sensor).build();
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        if (!DataStore.getSensors().containsKey(sensorId))
            throw new NotFoundException("Sensor not found: " + sensorId);
        SensorReadingResource resource = resourceContext.getResource(SensorReadingResource.class);
        resource.setSensorId(sensorId);
        return resource;
    }
    
    
}

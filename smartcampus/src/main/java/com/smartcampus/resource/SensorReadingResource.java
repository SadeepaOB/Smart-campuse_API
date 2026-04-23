package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private String sensorId;

    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    @GET
    public Response getReadings() {
        return Response.ok(DataStore.getReadingsForSensor(sensorId)).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = DataStore.getSensors().get(sensorId);
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus()))
            throw new SensorUnavailableException(sensorId);
        SensorReading newReading = new SensorReading(reading.getValue());
        sensor.setCurrentValue(newReading.getValue()); // side effect
        DataStore.getReadingsForSensor(sensorId).add(newReading);
        return Response.status(201).entity(newReading).build();
    }
}

package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.ArrayList;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @GET
    public Response getAllRooms() {
        return Response.ok(new ArrayList<>(DataStore.getRooms().values())).build();
    }

    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank())
            return Response.status(400).entity("{\"message\":\"Room id is required\"}").build();
        if (DataStore.getRooms().containsKey(room.getId()))
            return Response.status(409).entity("{\"message\":\"Room already exists: " + room.getId() + "\"}").build();
        DataStore.getRooms().put(room.getId(), room);
        return Response.status(201).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRooms().get(roomId);
        if (room == null)
            return Response.status(404).entity("{\"message\":\"Room not found: " + roomId + "\"}").build();
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRooms().get(roomId);
        if (room == null)
            return Response.status(404).entity("{\"message\":\"Room not found: " + roomId + "\"}").build();
        if (!room.getSensorIds().isEmpty())
            throw new RoomNotEmptyException(roomId);
        DataStore.getRooms().remove(roomId);
        return Response.noContent().build();
    }
    @PUT
    @Path("/{roomId}")
    public Response updateRoom(@PathParam("roomId") String roomId, Room updatedRoom) {
        Room existingRoom = DataStore.getRooms().get(roomId);
        
        if (existingRoom == null) {
            return Response.status(404)
                .entity("{\"message\":\"Room not found: " + roomId + "\"}").build();
        }

        // Update the fields
        if (updatedRoom.getName() != null) {
            existingRoom.setName(updatedRoom.getName());
        }
        if (updatedRoom.getCapacity() > 0) {
            existingRoom.setCapacity(updatedRoom.getCapacity());
        }

        // Save back to DataStore (though since it's a Map of objects, 
        // existingRoom is already a reference to the one in the map)
        return Response.ok(existingRoom).build();
    }
}

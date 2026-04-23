package com.smartcampus.exception;

import com.smartcampus.model.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {
    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        return Response.status(409).type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(409, "Conflict", ex.getMessage())).build();
    }
}

@Provider
class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        return Response.status(422).type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(422, "Unprocessable Entity", ex.getMessage())).build();
    }
}

@Provider
class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException ex) {
        return Response.status(403).type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(403, "Forbidden", ex.getMessage())).build();
    }
}

@Provider
class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());
    @Override
    public Response toResponse(Throwable ex) {
        LOGGER.log(Level.SEVERE, "Unhandled exception", ex);
        return Response.status(500).type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(500, "Internal Server Error",
                        "An unexpected error occurred. Contact the administrator.")).build();
    }
}

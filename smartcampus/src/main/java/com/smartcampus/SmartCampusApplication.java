package com.smartcampus;

import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application entry point.
 *
 * NO @ApplicationPath here — web.xml is the single source of truth for URL mapping.
 * Having both @ApplicationPath AND a web.xml servlet-mapping causes the path to
 * double up (e.g. /api/v1/api/v1/rooms), resulting in 404 errors.
 *
 * Lifecycle: JAX-RS creates a NEW resource instance per request by default.
 * Shared mutable state (rooms, sensors) lives in DataStore using static
 * ConcurrentHashMap fields to ensure thread safety across requests.
 */
public class SmartCampusApplication extends Application {
}

# 🏫 Smart Campus API

> **5COSC022W — Client-Server Architectures Coursework | University of Westminster 2025/26**
>
> A production-grade RESTful API built with **JAX-RS (Jersey 3.1.3)** and deployed on **Apache Tomcat 10.x**, managing campus Rooms, IoT Sensors, and historical Sensor Readings.

---

## Table of Contents

1. [API Overview](#1-api-overview)
2. [Project Structure](#2-project-structure)
3. [Technology Stack](#3-technology-stack)
4. [Prerequisites](#4-prerequisites)
5. [Build Instructions](#5-build-instructions)
6. [Running the Server](#6-running-the-server)
7. [API Endpoints Reference](#7-api-endpoints-reference)
8. [Sample curl Commands](#8-sample-curl-commands)
9. [Error Handling Strategy](#9-error-handling-strategy)
10. [Conceptual Report (Q&A)](#10-conceptual-report-qa)

---

## 1. API Overview

The **Smart Campus API** provides a campus-wide infrastructure for managing physical rooms and the diverse IoT sensors deployed within them (temperature monitors, CO₂ sensors, occupancy trackers, etc.).

### Design Principles

- **Resource-based URIs** — every noun (Room, Sensor, Reading) is a first-class resource with its own path
- **Versioned entry point** — all routes are prefixed with `/api/v1` for forward compatibility
- **Sub-resource nesting** — sensor readings are scoped under their parent sensor (`/sensors/{id}/readings`)
- **Consistent JSON responses** — all success and error responses are JSON; raw stack traces are never exposed
- **In-memory data store** — `ConcurrentHashMap` backed `DataStore` class (no database required)
- **HATEOAS Discovery** — `GET /api/v1/` returns navigation links to all primary resource collections

### Resource Hierarchy

```
/api/v1/
├── /rooms
│   ├── GET     — list all rooms
│   ├── POST    — create a room
│   └── /{roomId}
│       ├── GET    — get a specific room
│       ├── PUT    — update a room
│       └── DELETE — delete room (blocked if sensors assigned)
│
└── /sensors
    ├── GET     — list all sensors (supports ?type= filter)
    ├── POST    — register a sensor (validates roomId exists)
    └── /{sensorId}
        ├── GET    — get a specific sensor
        ├── PUT    — update a sensor
        └── /readings
            ├── GET  — list historical readings
            └── POST — append a new reading (updates sensor's currentValue)
```

### Data Models

```java
Room          { id, name, capacity, sensorIds[] }
Sensor        { id, type, status, currentValue, roomId }
SensorReading { id (UUID), timestamp (epoch ms), value }
```

Sensor `status` is one of: `ACTIVE` | `MAINTENANCE` | `OFFLINE`

---

## 2. Project Structure

```
smartcampus/
├── pom.xml
└── src/
    └── main/
        ├── java/com/smartcampus/
        │   ├── SmartCampusApplication.java        # JAX-RS Application subclass
        │   ├── model/
        │   │   ├── Room.java
        │   │   ├── Sensor.java
        │   │   ├── SensorReading.java
        │   │   └── ErrorResponse.java
        │   ├── store/
        │   │   └── DataStore.java                 # ConcurrentHashMap in-memory store
        │   ├── resource/
        │   │   ├── DiscoveryResource.java          # GET /api/v1/
        │   │   ├── RoomResource.java               # /api/v1/rooms
        │   │   ├── SensorResource.java             # /api/v1/sensors
        │   │   └── SensorReadingResource.java      # Sub-resource: /readings
        │   ├── exception/
        │   │   ├── RoomNotEmptyException.java
        │   │   ├── LinkedResourceNotFoundException.java
        │   │   ├── SensorUnavailableException.java
        │   │   └── ExceptionMappers.java           # All @Provider mappers in one file
        │   └── filter/
        │       ├── LoggingFilter.java              # Request + response logging
        │       └── CORSFilter.java                 # Cross-origin headers
        └── webapp/
            ├── WEB-INF/web.xml                    # Jersey servlet + URL mapping
            ├── META-INF/context.xml
            └── index.html                         # Browser dashboard (frontend)
```

---

## 3. Technology Stack

| Layer | Technology |
|---|---|
| REST Framework | Jersey 3.1.3 (JAX-RS / Jakarta EE 10) |
| Server | Apache Tomcat 10.x |
| JSON Binding | Jackson (via `jersey-media-json-jackson`) |
| Build Tool | Apache Maven 3.8+ |
| Java Version | JDK 17+ (tested up to JDK 25) |
| Data Storage | `ConcurrentHashMap` — in-memory only, no database |
| Frontend | Plain HTML + CSS + Vanilla JavaScript |

---

## 4. Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 17 or higher | JDK 25 is compatible |
| Apache Maven | 3.8+ | Used for build and dependency management |
| Apache Tomcat | **10.x only** | Jersey 3.x requires the `jakarta.*` namespace; Tomcat 9.x uses `javax.*` and **will not work** |
| NetBeans IDE | 19+ | Recommended; handles deployment automatically |

> ⚠️ **Critical:** Do not use Tomcat 9.x. The application will fail to start with `ClassNotFoundException` because Jersey 3.x depends on `jakarta.ws.rs.*`, which is only available in Tomcat 10+.

---

## 5. Build Instructions

### Clone the repository

```bash
git clone https://github.com/<your-username>/smartcampus.git
cd smartcampus
```

### Compile and package

```bash
mvn clean package
```

This compiles all sources, runs any tests, and produces:

```
target/smartcampus.war
```

### Verify the build

```bash
ls -lh target/smartcampus.war
# Should show the WAR file (~5–10 MB with dependencies)
```

---

## 6. Running the Server

### Option A — NetBeans (Recommended)

1. Open **NetBeans** → `File` → `Open Project` → select the `smartcampus/` directory
2. Right-click the project → `Properties` → `Run` → select your **Tomcat 10** server instance
3. Click the **Run** button (▶) — NetBeans builds, deploys, and opens the browser automatically

### Option B — Manual Deployment (Command Line)

```bash
# 1. Build the WAR
mvn clean package

# 2. Copy WAR to Tomcat's webapps directory
cp target/smartcampus.war /opt/tomcat/webapps/
# (adjust the path to match your Tomcat installation)

# 3. Start Tomcat
/opt/tomcat/bin/startup.sh        # macOS / Linux
# OR
C:\tomcat\bin\startup.bat          # Windows

# 4. Verify deployment — look for:
# INFO: Deployment of web application archive [...] smartcampus.war has finished
tail -f /opt/tomcat/logs/catalina.out
```

### Option C — Tomcat Manager UI

1. Navigate to `http://localhost:8080/manager/html`
2. Under **Deploy** → **WAR file to deploy**, click **Choose File**
3. Select `target/smartcampus.war` → click **Deploy**
4. The app appears in the application list as `/smartcampus`

### Verify the server is running

```bash
curl http://localhost:8080/smartcampus/api/v1/
```

Expected response:

```json
{
  "name": "Smart Campus API",
  "version": "1.0",
  "description": "RESTful API for campus room and IoT sensor management",
  "contact": "admin@smartcampus.ac.uk",
  "resources": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

---

## 7. API Endpoints Reference

All endpoints are relative to: `http://localhost:8080/smartcampus`

### Discovery

| Method | Path | Description | Response |
|---|---|---|---|
| `GET` | `/api/v1/` | API metadata and navigation links | `200 OK` |

### Rooms — `/api/v1/rooms`

| Method | Path | Description | Success | Error |
|---|---|---|---|---|
| `GET` | `/api/v1/rooms` | List all rooms | `200 OK` | — |
| `POST` | `/api/v1/rooms` | Create a new room | `201 Created` | `400`, `409` |
| `GET` | `/api/v1/rooms/{roomId}` | Get room by ID | `200 OK` | `404` |
| `PUT` | `/api/v1/rooms/{roomId}` | Update room name / capacity | `200 OK` | `404` |
| `DELETE` | `/api/v1/rooms/{roomId}` | Delete room (must have no sensors) | `204 No Content` | `404`, `409` |

### Sensors — `/api/v1/sensors`

| Method | Path | Description | Success | Error |
|---|---|---|---|---|
| `GET` | `/api/v1/sensors` | List all sensors | `200 OK` | — |
| `GET` | `/api/v1/sensors?type=CO2` | Filter sensors by type | `200 OK` | — |
| `POST` | `/api/v1/sensors` | Register sensor (validates `roomId`) | `201 Created` | `400`, `422` |
| `GET` | `/api/v1/sensors/{sensorId}` | Get sensor by ID | `200 OK` | `404` |
| `PUT` | `/api/v1/sensors/{sensorId}` | Update sensor fields | `200 OK` | `404` |

### Sensor Readings — `/api/v1/sensors/{sensorId}/readings`

| Method | Path | Description | Success | Error |
|---|---|---|---|---|
| `GET` | `/api/v1/sensors/{sensorId}/readings` | Get all historical readings | `200 OK` | `404` |
| `POST` | `/api/v1/sensors/{sensorId}/readings` | Append a new reading | `201 Created` | `403`, `404` |

> **Side effect:** A successful `POST` reading also updates `sensor.currentValue` to the new value.

### HTTP Status Codes Used

| Code | Meaning | When returned |
|---|---|---|
| `200` | OK | Successful GET / PUT |
| `201` | Created | Successful POST |
| `204` | No Content | Successful DELETE |
| `400` | Bad Request | Missing required fields |
| `403` | Forbidden | Sensor is in MAINTENANCE status |
| `404` | Not Found | Resource does not exist |
| `409` | Conflict | Duplicate ID on create / room has sensors on delete |
| `415` | Unsupported Media Type | Wrong Content-Type sent (handled automatically by JAX-RS) |
| `422` | Unprocessable Entity | `roomId` in sensor payload references a non-existent room |
| `500` | Internal Server Error | Unhandled runtime exception (stack trace never exposed) |

---

## 8. Sample curl Commands

Set the base URL once for convenience:

```bash
BASE=http://localhost:8080/smartcampus/api/v1
```

### 1. Discover the API

```bash
curl -X GET "$BASE/" \
  -H "Accept: application/json"
```

### 2. List all rooms (pre-seeded data)

```bash
curl -X GET "$BASE/rooms"
```

### 3. Create a new room

```bash
curl -X POST "$BASE/rooms" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "CS-301",
    "name": "Networks Lab",
    "capacity": 35
  }'
```

### 4. Get a specific room by ID

```bash
curl -X GET "$BASE/rooms/LIB-301"
```

### 5. Register a sensor (with room validation)

```bash
curl -X POST "$BASE/sensors" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEMP-002",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 21.5,
    "roomId": "CS-301"
  }'
```

### 6. Filter sensors by type

```bash
curl -X GET "$BASE/sensors?type=CO2"
```

### 7. Post a sensor reading (updates currentValue)

```bash
curl -X POST "$BASE/sensors/TEMP-001/readings" \
  -H "Content-Type: application/json" \
  -d '{"value": 24.8}'
```

### 8. Get historical readings for a sensor

```bash
curl -X GET "$BASE/sensors/TEMP-001/readings"
```

### 9. Attempt to delete a room that still has sensors → expect 409 Conflict

```bash
curl -X DELETE "$BASE/rooms/LIB-301"
```

Expected response:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Room 'LIB-301' still has active sensors. Remove all sensors before deleting.",
  "timestamp": 1714000000000
}
```

### 10. Register a sensor with a non-existent roomId → expect 422

```bash
curl -X POST "$BASE/sensors" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "BAD-001",
    "type": "CO2",
    "status": "ACTIVE",
    "currentValue": 0,
    "roomId": "FAKE-999"
  }'
```

Expected response:

```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Referenced roomId 'FAKE-999' does not exist.",
  "timestamp": 1714000000000
}
```

### 11. Post a reading to a MAINTENANCE sensor → expect 403

```bash
curl -X POST "$BASE/sensors/OCC-001/readings" \
  -H "Content-Type: application/json" \
  -d '{"value": 15}'
```

Expected response:

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Sensor 'OCC-001' is under MAINTENANCE and cannot accept new readings.",
  "timestamp": 1714000000000
}
```

---

## 9. Error Handling Strategy

The API uses a layered, "leak-proof" exception handling strategy. Raw Java stack traces are **never** returned to API consumers.

| Exception Class | Trigger | HTTP Status | Mapper Class |
|---|---|---|---|
| `RoomNotEmptyException` | DELETE room with sensors assigned | `409 Conflict` | `RoomNotEmptyExceptionMapper` |
| `LinkedResourceNotFoundException` | POST sensor with non-existent `roomId` | `422 Unprocessable Entity` | `LinkedResourceNotFoundExceptionMapper` |
| `SensorUnavailableException` | POST reading to a `MAINTENANCE` sensor | `403 Forbidden` | `SensorUnavailableExceptionMapper` |
| `Throwable` (catch-all) | Any unhandled runtime exception | `500 Internal Server Error` | `GlobalExceptionMapper` |

All error responses share a consistent JSON shape:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Human-readable explanation here.",
  "timestamp": 1714000000000
}
```

### Logging

The `LoggingFilter` class implements both `ContainerRequestFilter` and `ContainerResponseFilter`. Every request and response is logged to `java.util.logging` in the format:

```
[REQUEST]  POST http://localhost:8080/smartcampus/api/v1/sensors
[RESPONSE] POST http://localhost:8080/smartcampus/api/v1/sensors -> HTTP 201
```

---

## 10. Conceptual Report (Q&A)

---

### Part 1.1 — JAX-RS Resource Lifecycle & Impact on In-Memory State Management

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** — this is called *per-request scoping*. Instance fields are initialised fresh on each request and discarded when the response is sent; there is no reuse between requests, the opposite of a singleton.

**Impact on shared data management:** Because resource instances are ephemeral, shared mutable state cannot live in instance fields — any data written there would vanish after the response. All shared state must live outside the resource class in a structure that survives across requests.

In this project, the `DataStore` class holds all data in `static` fields backed by `ConcurrentHashMap`. `ConcurrentHashMap` provides thread-safe, non-blocking reads and fine-grained locking on writes. This is critical because Tomcat is a multi-threaded server — two simultaneous POST requests could otherwise overwrite each other's entries, or a DELETE could interleave with a GET and expose stale data. Using `static` fields in `DataStore` ensures there is exactly one shared store regardless of how many resource instances JAX-RS creates.

---

### Part 1.2 — HATEOAS and the Benefits of Hypermedia in RESTful Design

HATEOAS (Hypermedia as the Engine of Application State) is considered a hallmark of advanced REST design because it makes an API **self-describing and navigable at runtime**. Rather than requiring clients to memorise all URL patterns from external documentation, each response embeds hyperlinks that advertise what actions are available next and where to find related resources.

**Benefits for client developers:**

- **Discoverability:** A client can start at a single entry point (`GET /api/v1/`) and explore the entire API by following embedded links, with no prior knowledge of the URL structure.
- **Reduced coupling:** If the server changes a URL path, clients following embedded links adapt automatically rather than breaking. The client is not hard-coding routes.
- **Self-documentation:** The API serves as its own interactive guide, drastically lowering onboarding time for new integrators.
- **Evolvability:** New resources and actions can be advertised without forcing a client-side release.

The `DiscoveryResource` (`GET /api/v1/`) in this project is a direct implementation of this principle — it returns a JSON map pointing to every primary resource collection, giving any client a complete navigation starting point in a single call.

---

### Part 2.1 — Implications of Returning Only IDs vs Full Room Objects

Returning **only IDs** is bandwidth-efficient for large collections, but it forces the client to make one additional HTTP request per room to retrieve the details it actually needs — the classic **N+1 request problem**. For a list of 200 rooms, this means 200 additional round-trips, dramatically increasing latency and server load.

Returning **full objects** increases the initial response payload but allows the client to render a complete list in a single HTTP call — far more efficient in practice for dashboard-style clients that always need all fields.

For the Smart Campus management dashboard, returning full room objects is the correct default since facilities managers always need the room name and capacity alongside the ID. For very large datasets, the professional solution is **pagination** (`?page=1&size=20`) combined with optional **field projection** (`?fields=id,name`) to balance completeness and bandwidth without introducing N+1 issues.

---

### Part 2.2 — Is the DELETE Operation Idempotent?

**Yes, DELETE is idempotent in this implementation.** HTTP idempotency is defined in terms of *server-side state*, not response codes.

- **First call:** The room is located, removed from `DataStore`, and `HTTP 204 No Content` is returned.
- **Second (and subsequent) calls:** The room no longer exists. The handler returns `HTTP 404 Not Found`.

After both calls, the server's state is identical — the room does not exist. The response code differs between the first and second call, but idempotency is defined by the RFC as: *"the side-effects of N > 0 identical requests is the same as for a single request"*. The side-effect (room deletion) happens exactly once regardless of how many times the request is repeated, which satisfies the definition. This is standard, correct REST behaviour for DELETE operations.

---

### Part 3.1 — Technical Consequences of a `@Consumes` Media Type Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation declares which `Content-Type` formats the method will accept. When a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`, JAX-RS inspects the incoming header during the **request-dispatching phase** and finds no resource method annotated to consume that format.

The JAX-RS runtime automatically returns **HTTP 415 Unsupported Media Type** before any application code is executed — the resource method body is never reached. No manual `if/else` error handling is required in the resource class. This is a clean separation between protocol-level content negotiation (JAX-RS's responsibility) and application business logic (the developer's responsibility), and it ensures that malformed requests are rejected at the earliest possible stage.

---

### Part 3.2 — `@QueryParam` vs Path Segment for Filtering Collections

Query parameters (`/sensors?type=CO2`) are the superior design choice for filtering, for several reasons:

1. **Optional by nature:** The parameter can be omitted entirely, making the same endpoint serve both filtered and unfiltered responses without requiring separate routes (`/sensors` and `/sensors/type/{type}`).
2. **Composability:** Multiple filters combine naturally and readably (`?type=CO2&status=ACTIVE`); nesting them as path segments (`/sensors/type/CO2/status/ACTIVE`) becomes unwieldy and cannot scale to arbitrary filter combinations.
3. **Semantic correctness:** URL paths identify *resources*; query strings refine or filter *representations* of a resource. A path like `/sensors/type/CO2` incorrectly implies that `type/CO2` is a distinct child resource, when it is actually a filter applied to the sensor collection.
4. **Industry convention:** Filtering, searching, sorting, and pagination are universally handled via query strings in established REST APIs (GitHub, Stripe, Twitter/X all follow this pattern), making it immediately intuitive for any developer.

---

### Part 4.1 — Architectural Benefits of the Sub-Resource Locator Pattern

The Sub-Resource Locator pattern allows a parent resource (`SensorResource`) to delegate request handling for a nested path (`/{sensorId}/readings`) to a dedicated, purpose-built class (`SensorReadingResource`). The locator method performs validation (checking the sensor exists) and injects context (the `sensorId`), then returns the sub-resource instance for JAX-RS to dispatch to.

**Advantages over a monolithic controller class:**

- **Single Responsibility Principle:** Each class owns exactly one concern — `SensorResource` manages sensor CRUD; `SensorReadingResource` manages historical readings. Neither needs to know the internals of the other.
- **Independent testability:** `SensorReadingResource` can be unit-tested in isolation by injecting a mock `sensorId`, without bootstrapping the full parent resource hierarchy.
- **Prevention of God Classes:** Without this pattern, every nested path (`/sensors/{id}/readings`, future `/sensors/{id}/alerts`, `/sensors/{id}/config`, etc.) would accumulate inside one massive resource class, making it brittle, hard to read, and prone to merge conflicts in a team environment.
- **Team scalability:** Different developers can own and modify different sub-resource classes in parallel without conflicts in a shared file.

---

### Part 5.2 — Why HTTP 422 Is More Semantically Accurate Than 404

When a client POSTs a new sensor with a `roomId` that does not exist in the system, the request URI (`/api/v1/sensors`) is valid, reachable, and correctly resolved by JAX-RS. Returning **404 Not Found** would be misleading because it implies the *endpoint itself* was not located — which is false.

**HTTP 422 Unprocessable Entity** is semantically accurate because it signals precisely that:

1. The request was received and understood (it is syntactically valid JSON).
2. The endpoint was successfully located and dispatched to.
3. Processing failed due to a **semantic / logical failure inside the payload** — a field value (`roomId`) references a resource that does not exist.

The problem is in the *meaning* of the data, not its format or the routing of the request. HTTP 422 makes the error immediately actionable for client developers: they know to fix the `roomId` value in their request body, not the URL they are calling. A 404 response would send them looking for a missing endpoint that is actually there, wasting debugging time.

---

### Part 5.4 — Security Risks of Exposing Internal Java Stack Traces

Returning raw Java stack traces in API responses is a significant security vulnerability. An attacker can extract the following from a single trace:

1. **Internal architecture and class names:** Fully-qualified class and package names (e.g., `com.smartcampus.store.DataStore`) reveal the application's internal design, making it easier to craft targeted payloads.
2. **Server file-system layout:** Source file paths embedded in traces (e.g., `/home/ubuntu/app/src/main/java/com/smartcampus/store/DataStore.java:47`) expose the exact directory structure of the server, aiding directory traversal and privilege escalation attempts.
3. **Library names and versions:** Dependency class names visible in the trace allow an attacker to cross-reference public CVE databases to identify known, exploitable vulnerabilities in specific library releases.
4. **Internal logic and variable names:** Exception messages such as `NullPointerException at DataStore.getRooms():83` reveal code flow, helping attackers identify predictable failure modes and craft inputs that deliberately trigger those paths for further exploitation.

The `GlobalExceptionMapper` in this project mitigates all of these risks: it logs the complete stack trace server-side using `java.util.logging` for developer diagnostics, while returning only a generic `HTTP 500 Internal Server Error` message with no internal details to the client.

---

### Part 5.5 — Filters vs Manual Logging in Every Resource Method

Using JAX-RS filters for cross-cutting concerns like logging is architecturally superior to inserting `Logger.info()` calls manually into every resource method for several reasons:

1. **DRY (Don't Repeat Yourself):** One filter class automatically covers every endpoint in the API — including endpoints added in the future — with zero code duplication. Manual logging requires touching every new method written.
2. **Separation of concerns:** Resource methods should contain only business logic. Infrastructure concerns (logging, authentication, rate-limiting, CORS) belong in filters, keeping code focused and readable.
3. **Completeness:** Filters intercept *all* requests — including those that fail before reaching a resource method (e.g., a `415 Unsupported Media Type` rejection or an authentication failure). Manual logging inside resource methods would silently miss these cases.
4. **Maintainability:** Changing the log format, switching to a structured logging library, or adding a correlation ID requires editing a single filter class rather than updating dozens of resource methods across the codebase.
5. **Richer context:** `ContainerRequestContext` and `ContainerResponseContext` expose HTTP method, full URI, all request headers, and the final response status code in one place — information that is not always conveniently accessible from within a specific resource method.

---

*University of Westminster — 5COSC022W Client-Server Architectures — 2025/26*

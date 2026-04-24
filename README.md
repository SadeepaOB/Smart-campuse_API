# 🏛️ Smart Campus — Sensor & Room Management API

> **Module:** 5COSC022W — Client-Server Architectures
> **University:** University of Westminster | 2025/26
> **Title:** Smart Campus Sensor & Room Management API

---

## 📌 API Design Overview

This project implements a fully RESTful web service for the University of Westminster's **Smart Campus** initiative. The system provides a scalable, hierarchical API for managing campus infrastructure — specifically Rooms and their IoT Sensors — alongside a browser-based management dashboard.

### Resource Hierarchy

```
/api/v1/
├── rooms/
│   ├── GET    /rooms              → List all rooms
│   ├── POST   /rooms              → Create a room
│   ├── GET    /rooms/{roomId}     → Get room details
│   └── DELETE /rooms/{roomId}     → Delete a room (blocked if sensors exist)
└── sensors/
    ├── GET    /sensors            → List all sensors (optional ?type= filter)
    ├── POST   /sensors            → Register a sensor (validates roomId)
    ├── GET    /sensors/{id}       → Get sensor details
    └── {id}/readings/
        ├── GET  /readings         → Get historical readings
        └── POST /readings         → Submit a new reading
```

### Core Data Models

**Room** — represents a physical campus space
```json
{
  "id": "LIB-301",
  "name": "Library Quiet Study",
  "capacity": 50,
  "sensorIds": ["TEMP-001"]
}
```

**Sensor** — an IoT device deployed within a room
```json
{
  "id": "TEMP-001",
  "type": "Temperature",
  "status": "ACTIVE",
  "currentValue": 22.5,
  "roomId": "LIB-301"
}
```

**SensorReading** — a timestamped measurement from a sensor
```json
{
  "id": "a3f1c2d4-...",
  "timestamp": 1713962400000,
  "value": 23.7
}
```

### Design Decisions

- **No database** — all data stored in thread-safe `ConcurrentHashMap` static fields inside `DataStore.java`
- **WAR deployment** — packaged as a WAR file for Apache Tomcat 10
- **Jersey 3.x (JAX-RS)** — only technology used for the REST layer, as required
- **Consistent error responses** — all errors return a structured JSON body, never a raw stack trace
- **Sub-resource locator** — readings are managed via a dedicated `SensorReadingResource` class, delegated from `SensorResource`

---

## 🛠️ How to Build and Run

### Prerequisites

| Requirement | Version |
|-------------|---------|
| Java JDK | 17 or higher (JDK 25 tested) |
| Apache Maven | 3.8+ |
| Apache Tomcat | **10.1.x only** |
| NetBeans IDE | 20+ (recommended) |

> ⚠️ **Tomcat 10.x is required.** Jersey 3.x uses the `jakarta.*` namespace introduced in Jakarta EE 10. Tomcat 9.x uses the old `javax.*` namespace and will fail to load the application.

---

### Step 1 — Clone the Repository

```bash
git clone https://github.com/<your-username>/smart-campus-api.git
cd smart-campus-api
```

---

### Step 2 — Build the WAR File

```bash
mvn clean package
```

This produces: `target/smartcampus.war`

---

### Step 3 — Deploy and Run

#### Option A: NetBeans (Recommended for Mac)

1. Open NetBeans → `File` → `Open Project` → select the project folder
2. Go to `Tools` → `Servers` → `Add Server` → choose **Apache Tomcat 10**
3. Right-click the project → `Properties` → `Run` → set the Server to Tomcat 10
4. Click **Run** (▶)
5. NetBeans builds, deploys, and opens the browser automatically

#### Option B: Command Line (Mac/Linux)

```bash
# Copy the WAR to Tomcat
cp target/smartcampus.war /path/to/tomcat/webapps/

# Start Tomcat
/path/to/tomcat/bin/startup.sh

# Open the dashboard
open http://localhost:8080/smartcampus/
```

#### Option C: Tomcat Manager UI

1. Go to `http://localhost:8080/manager/html`
2. Under **"WAR file to deploy"** → upload `target/smartcampus.war`
3. Click **Deploy**

---

### Step 4 — Verify the Application is Running

Open your browser and visit:

| URL | Expected Result |
|-----|----------------|
| `http://localhost:8080/smartcampus/` | Web dashboard loads |
| `http://localhost:8080/smartcampus/api/v1/` | JSON discovery response |
| `http://localhost:8080/smartcampus/api/v1/rooms` | JSON array of rooms |

---

## 🔗 Sample curl Commands

> Base URL: `http://localhost:8080/smartcampus/api/v1`

### 1. Discovery — Get API metadata and resource links

```bash
curl -X GET http://localhost:8080/smartcampus/api/v1/ \
  -H "Accept: application/json"
```

**Expected response:**
```json
{
  "name": "Smart Campus API",
  "version": "1.0",
  "contact": "admin@smartcampus.ac.uk",
  "resources": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

---

### 2. Create a new room

```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"CS-301","name":"Networks Lab","capacity":35}'
```

**Expected response:** `201 Created`
```json
{
  "id": "CS-301",
  "name": "Networks Lab",
  "capacity": 35,
  "sensorIds": []
}
```

---

### 3. Register a sensor linked to a room

```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","status":"ACTIVE","currentValue":21.0,"roomId":"CS-301"}'
```

**Expected response:** `201 Created`

---

### 4. Filter sensors by type

```bash
curl -X GET "http://localhost:8080/smartcampus/api/v1/sensors?type=CO2" \
  -H "Accept: application/json"
```

**Expected response:** `200 OK` — array of sensors where type is CO2

---

### 5. Post a new sensor reading

```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":24.3}'
```

**Expected response:** `201 Created`
```json
{
  "id": "a3f1c2d4-uuid-here",
  "timestamp": 1713962400000,
  "value": 24.3
}
```

---

### 6. Get historical readings for a sensor

```bash
curl -X GET http://localhost:8080/smartcampus/api/v1/sensors/TEMP-001/readings \
  -H "Accept: application/json"
```

---

### 7. Delete a room that has sensors → expect 409 Conflict

```bash
curl -X DELETE http://localhost:8080/smartcampus/api/v1/rooms/LIB-301 \
  -H "Accept: application/json"
```

**Expected response:** `409 Conflict`
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Room 'LIB-301' still has active sensors assigned. Remove all sensors before deleting.",
  "timestamp": 1713962400000
}
```

---

### 8. Register a sensor with a non-existent roomId → expect 422

```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"BAD-001","type":"CO2","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-999"}'
```

**Expected response:** `422 Unprocessable Entity`

---

### 9. Post a reading to a MAINTENANCE sensor → expect 403 Forbidden

```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":15.0}'
```

**Expected response:** `403 Forbidden`
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Sensor 'OCC-001' is under MAINTENANCE and cannot accept new readings.",
  "timestamp": 1713962400000
}
```

---

## 📝 Conceptual Report

> *Answers to all questions posed in the coursework specification.*

---

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** (per-request scope). Instance variables are freshly initialised on each request and garbage-collected once the response is sent. This is the opposite of a singleton — the runtime never reuses a resource object across requests.

**Impact on in-memory data management:** Because resource instances are transient, any state stored as an instance field would be lost immediately after the request completes. To share mutable state (such as the map of rooms or sensors) across multiple requests from multiple clients, that state must live *outside* the resource class in a structure with application-wide scope. In this project, the `DataStore` class holds all data as `static` fields backed by `ConcurrentHashMap`. `ConcurrentHashMap` is thread-safe: it allows multiple threads (one per concurrent request on Tomcat) to read and write entries simultaneously without corrupting data or causing race conditions. Using a regular `HashMap` in a multi-threaded server environment could result in lost updates, corrupted entries, or infinite loops during concurrent structural modifications.

---

### Part 1.2 — HATEOAS (Hypermedia as the Engine of Application State)

HATEOAS is considered a hallmark of advanced RESTful design because it makes an API **self-describing and navigable at runtime**. Instead of a client needing to memorise all URL patterns from static external documentation, each response embeds links pointing to related resources and available actions. For example, a response for a room could include a direct link to its sensors collection, and a sensor response could include a link to its readings history.

The benefit for client developers is significant: the client can explore the entire API by following links from a single known entry point (the discovery endpoint), without relying on documentation that may be outdated. The server becomes the single source of truth for URL structure, reducing the coupling between client and server implementations. When the server changes a URL, clients following links adapt automatically rather than breaking. The `GET /api/v1/` Discovery endpoint in this project demonstrates HATEOAS — it returns a map of all primary resource collections with their paths, enabling a client to navigate the full API programmatically.

---

### Part 2.1 — Returning IDs vs Full Room Objects in List Responses

Returning only IDs is bandwidth-efficient but creates the **N+1 problem**: a client listing 100 rooms would need to make 100 additional requests to fetch each room's details, dramatically increasing total latency and server load. Returning full objects sends more data per response but allows clients to render a complete list in a single round trip.

For a campus management API where dashboards almost always need the full room details (name, capacity, sensor count), returning complete objects is the more practical design. For very large datasets where bandwidth is a genuine concern, the professional solution is **pagination** (e.g., `?page=1&size=20`) combined with **field projection** (allowing clients to request only the fields they need via query parameters), rather than returning bare IDs.

---

### Part 2.2 — Idempotency of the DELETE Operation

Yes, DELETE is **idempotent** in this implementation. Idempotency means that executing the same request multiple times results in the same server state as executing it once.

- **First DELETE request:** The room is found, passes the sensor-count check, is removed from the `ConcurrentHashMap`, and the server returns `204 No Content`.
- **Second (and any subsequent) DELETE request:** The room no longer exists in the map, so the server returns `404 Not Found`.

The server's state is identical after the first and all subsequent calls — the room is absent. The HTTP response code changes (204 → 404), but idempotency is defined strictly in terms of **server-side state**, not the response code returned to the client. This behaviour is fully aligned with the HTTP/1.1 specification (RFC 9110), which explicitly classifies DELETE as an idempotent method.

---

### Part 3.1 — Technical Consequences of a `@Consumes` Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells the JAX-RS runtime that the annotated method is only capable of processing requests where the `Content-Type` header is `application/json`. During request dispatch, before any application code executes, the runtime inspects the incoming `Content-Type` header and attempts to find a matching resource method.

If a client sends the request with `Content-Type: text/plain` or `Content-Type: application/xml`, the runtime finds no method that declares it can consume those types and automatically returns **HTTP 415 Unsupported Media Type**. The method body is never invoked. This is a key advantage of declarative content negotiation in JAX-RS — the framework enforces the contract at the infrastructure level, eliminating the need for manual type-checking inside every method.

---

### Part 3.2 — `@QueryParam` vs Path Segment for Filtering

Using `@QueryParam` for filtering (e.g., `GET /sensors?type=CO2`) is considered superior to embedding the filter in the path (e.g., `GET /sensors/type/CO2`) for several reasons:

1. **Optional by nature:** A query parameter can be omitted entirely (`GET /sensors`), allowing a single endpoint to serve both filtered and unfiltered requests. A path-based filter requires a separate route for each case.
2. **Composable:** Multiple query parameters combine naturally — `?type=CO2&status=ACTIVE` — to build complex filters. Path segments cannot express this elegantly.
3. **Semantic correctness:** URL paths identify resources; query strings parameterise or refine representations of those resources. `/sensors/type/CO2` implies `type/CO2` is a distinct sub-resource, which is semantically wrong — it is a filtered view of the sensors collection.
4. **REST convention:** Filtering, sorting, searching, and pagination are universally understood as query string concerns. Placing them in the path violates widely accepted REST design conventions and makes the API less predictable for client developers.

---

### Part 4.1 — Architectural Benefits of the Sub-Resource Locator Pattern

The Sub-Resource Locator pattern allows a parent resource class (`SensorResource`) to delegate the handling of a nested path (`/{sensorId}/readings`) to a dedicated child class (`SensorReadingResource`). The locator method is responsible only for validation and context injection — it verifies the parent sensor exists and sets the `sensorId` on the child — then returns the child resource instance. The JAX-RS runtime then dispatches the actual HTTP method (GET or POST) to the appropriate method on the child class.

**Architectural benefits in large APIs:**

- **Single Responsibility Principle:** Each class owns exactly one concern. `SensorResource` manages sensor CRUD; `SensorReadingResource` manages readings history. Neither class needs to know about the other's internal logic.
- **Independent testability:** `SensorReadingResource` can be instantiated and unit tested in complete isolation without involving the parent resource class.
- **Avoids monolithic controllers:** Without this pattern, every nested path must be crammed into a single class. As APIs grow (e.g., adding `/sensors/{id}/alerts`, `/sensors/{id}/calibrations`), the class becomes unmanageable and fragile.
- **Parallel team development:** Separate resource classes can be developed, reviewed, and merged by different team members concurrently without conflicting edits.

---

### Part 5.2 — Why HTTP 422 is More Semantically Accurate than 404

When a client POSTs a new sensor body containing `"roomId": "FAKE-999"`, the **request URI** (`/api/v1/sensors`) is valid and the endpoint was successfully located — nothing about the *endpoint* is missing. Returning `404 Not Found` would incorrectly signal that the API path itself does not exist, which is misleading and confusing for the client developer.

**HTTP 422 Unprocessable Entity** is semantically precise for this scenario because it communicates that the server: (1) understood the request format, (2) successfully parsed the JSON body, (3) located the correct endpoint, but (4) could not complete the operation because the **content of the entity is semantically invalid** — specifically, a field references a resource that does not exist in the system. The problem is in the *meaning* of the data, not in the HTTP plumbing. HTTP 422 was designed exactly for this class of error: syntactically well-formed content that fails business-rule or referential integrity validation.

---

### Part 5.4 — Security Risks of Exposing Java Stack Traces

Exposing raw Java stack traces in API error responses is a serious security vulnerability for several reasons:

1. **Internal architecture disclosure:** Fully qualified class names (e.g., `com.smartcampus.store.DataStore`) reveal the package structure, framework choices, and code organisation, giving attackers a detailed map of the system internals.
2. **Server file system paths:** Absolute paths such as `/home/ubuntu/app/src/main/java/DataStore.java:47` expose server directory layout, which is directly useful for path traversal and local file inclusion attacks.
3. **Dependency fingerprinting:** Stack traces typically include third-party library names and version numbers (e.g., `org.glassfish.jersey.server 3.1.3`). Attackers cross-reference these against public CVE databases (e.g., NVD, Snyk) to identify known, exploitable vulnerabilities in those specific versions.
4. **Business logic leakage:** Exception messages such as `NullPointerException at RoomResource.java:52` or `KeyNotFoundException in ConcurrentHashMap` reveal the names of internal data structures and the exact lines of code where logic can be manipulated, enabling crafted payloads that target specific weaknesses.

The `GlobalExceptionMapper` in this project mitigates all of these risks by catching every unhandled `Throwable`, logging the full stack trace server-side (visible only to administrators), and returning a completely generic error message to the external client — ensuring no implementation details are ever leaked.

---

### Part 5.5 — Why Filters Are Superior to Manual Logging in Resource Methods

Using a JAX-RS filter that implements both `ContainerRequestFilter` and `ContainerResponseFilter` for logging is architecturally superior to inserting `Logger.info()` calls directly inside each resource method for the following reasons:

1. **DRY Principle (Don't Repeat Yourself):** One filter class automatically applies to every current and future endpoint. Adding a new resource class requires zero additional logging code.
2. **Separation of concerns:** Resource methods remain focused exclusively on business logic. Cross-cutting infrastructure concerns (logging, authentication, CORS) are handled at the framework layer.
3. **Completeness and coverage:** Filters intercept *all* requests, including those that fail before reaching a resource method — such as `415 Unsupported Media Type`, `404 Not Found`, or authentication failures. Manual logging inside resource methods would miss these entirely.
4. **Maintainability:** Changing the log format, adding request correlation IDs, or switching logging frameworks requires modifying a single class rather than hunting through dozens of resource methods across the codebase.
5. **Richer contextual data:** `ContainerRequestContext` and `ContainerResponseContext` provide access to the full HTTP method, request URI, all headers, and the final response status code — information that is not always conveniently available from inside a resource method without injecting additional context objects.

---

*University of Westminster — 5COSC022W Client-Server Architectures — 2025/26*

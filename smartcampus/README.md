# Smart Campus API — Web Application

A full-stack web application: **JAX-RS REST API backend** + **browser-based dashboard frontend**, deployed as a WAR on Apache Tomcat.

---

## Architecture

```
Browser  ──→  Tomcat :8080/smartcampus/          (serves index.html)
Browser  ──→  Tomcat :8080/smartcampus/api/v1/*  (Jersey JAX-RS)
```

Tomcat serves both the static HTML/JS frontend **and** the REST API from the same WAR file. No separate web server needed.

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| REST Framework | Jersey 3.1.3 (JAX-RS) |
| Server | Apache Tomcat 10.x |
| JSON | Jackson (via Jersey) |
| Build | Maven 3.x |
| Java | JDK 17+ (JDK 25 compatible) |
| Frontend | Plain HTML + CSS + JavaScript (no framework) |
| Database | `ConcurrentHashMap` (in-memory only) |

---

## Prerequisites

- JDK 17 or higher
- Apache Maven 3.8+
- Apache Tomcat **10.x** (required for Jakarta EE 10 / Jersey 3.x)
- NetBeans IDE (recommended)

> ⚠️ **Tomcat version matters**: Jersey 3.x uses the `jakarta.*` namespace. This requires **Tomcat 10.x**. Tomcat 9.x uses `javax.*` and will NOT work.

---

## Build

```bash
cd smartcampus
mvn clean package
```

This produces: `target/smartcampus.war`

---

## Deploy to Tomcat

### Option A — NetBeans (easiest)

1. Open NetBeans → **File → Open Project** → select `smartcampus/`
2. Right-click project → **Properties → Run** → set Server to your Tomcat 10 instance
3. Click **Run** (green play button) — NetBeans builds, deploys, and opens the browser

### Option B — Manual deployment

```bash
# Build the WAR
mvn clean package

# Copy to Tomcat webapps directory (adjust path to your Tomcat install)
cp target/smartcampus.war /path/to/tomcat/webapps/

# Start Tomcat
/path/to/tomcat/bin/startup.sh   # Mac/Linux
```

Then open: **http://localhost:8080/smartcampus**

### Option C — Tomcat Manager

1. Go to `http://localhost:8080/manager/html`
2. Scroll to "Deploy" → "WAR file to deploy"
3. Upload `target/smartcampus.war` → click Deploy

---

## Application URLs

| URL | Description |
|-----|-------------|
| `http://localhost:8080/smartcampus/` | Web dashboard (frontend) |
| `http://localhost:8080/smartcampus/api/v1/` | Discovery endpoint |
| `http://localhost:8080/smartcampus/api/v1/rooms` | Room management |
| `http://localhost:8080/smartcampus/api/v1/sensors` | Sensor management |
| `http://localhost:8080/smartcampus/api/v1/sensors/{id}/readings` | Reading history |

---

## Sample curl Commands

```bash
BASE=http://localhost:8080/smartcampus/api/v1

# 1. Discovery
curl -X GET $BASE/ -H "Accept: application/json"

# 2. List all rooms
curl -X GET $BASE/rooms

# 3. Create a room
curl -X POST $BASE/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"CS-301","name":"Networks Lab","capacity":35}'

# 4. Register a sensor (with room validation)
curl -X POST $BASE/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","status":"ACTIVE","currentValue":21.0,"roomId":"CS-301"}'

# 5. Filter sensors by type
curl -X GET "$BASE/sensors?type=CO2"

# 6. Post a sensor reading
curl -X POST $BASE/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":24.3}'

# 7. Get reading history
curl -X GET $BASE/sensors/TEMP-001/readings

# 8. Try deleting a room with sensors → expect 409
curl -X DELETE $BASE/rooms/LIB-301

# 9. Register sensor with fake roomId → expect 422
curl -X POST $BASE/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"BAD-001","type":"CO2","status":"ACTIVE","currentValue":0,"roomId":"FAKE-999"}'
```

---

## Report: Conceptual Questions

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of each resource class per incoming HTTP request**. Instance variables are re-created for every request and discarded when the response is sent — the opposite of a singleton. This is called *per-request scoping*.

**Impact on data management:** Since resource instances are short-lived, you cannot store shared state as instance fields — all data would reset on every request. Shared mutable state must live outside the resource class. In this project, `DataStore` uses `static` fields backed by `ConcurrentHashMap`. `ConcurrentHashMap` allows multiple threads (concurrent requests) to read and write safely without data corruption or race conditions, which is essential in a multi-threaded server like Tomcat.

---

### Part 1.2 — HATEOAS

HATEOAS (Hypermedia as the Engine of Application State) makes an API self-describing by embedding navigation links inside responses. Instead of a client needing to memorise all URL patterns, each response tells the client what actions are available and where to find related resources.

The benefit for client developers is that the API becomes explorable without external documentation. The server controls navigation, reducing coupling between client and server. If the server changes a URL structure, clients following links adapt automatically rather than breaking. The Discovery endpoint at `GET /api/v1` in this project is an example of HATEOAS in practice.

---

### Part 2.1 — Returning IDs vs Full Room Objects

Returning only IDs is bandwidth-efficient but causes the N+1 problem — the client must make one additional request per room to fetch details, multiplying round trips and server load. Returning full objects increases response size but allows a complete list to be rendered in a single HTTP call.

For a campus management dashboard, full objects are preferable since clients almost always need all fields. For very large datasets, pagination (`?page=1&size=20`) and field projection are the professional solutions.

---

### Part 2.2 — Idempotency of DELETE

Yes, DELETE is idempotent in this implementation. The first call removes the room and returns `204 No Content`. A second identical call finds nothing and returns `404 Not Found`. The server's state is identical after both calls — the room is gone. The response code differs, but idempotency is defined in terms of server-side state, not response codes. This aligns with the HTTP specification.

---

### Part 3.1 — @Consumes(APPLICATION_JSON) Mismatch

The `@Consumes` annotation declares the `Content-Type` formats the method accepts. If a client sends `Content-Type: text/plain` or `application/xml`, JAX-RS inspects the header during request dispatch and finds no matching method. It returns **HTTP 415 Unsupported Media Type** automatically, before any application code executes. The resource method body is never reached, and no manual error handling is needed.

---

### Part 3.2 — @QueryParam vs Path Segment for Filtering

Query parameters (`/sensors?type=CO2`) are superior for filtering for several reasons:

1. **Optional**: The parameter can be omitted, making the same endpoint serve both filtered and unfiltered responses without duplicate routes.
2. **Composable**: Multiple filters combine naturally (`?type=CO2&status=ACTIVE`); path segments do not.
3. **Semantic correctness**: Paths identify resources; query strings refine representations. `/sensors/type/CO2` implies `type/CO2` is a sub-resource, which is semantically wrong.
4. **REST convention**: Filtering, sorting, and searching are query string concerns by established convention.

---

### Part 4.1 — Sub-Resource Locator Pattern

The Sub-Resource Locator pattern lets a parent resource (`SensorResource`) hand off request handling for a nested path to a dedicated class (`SensorReadingResource`). The locator method handles validation and context injection, then returns the sub-resource instance.

Benefits in large APIs include Single Responsibility Principle compliance (each class owns one concern), independent testability of sub-resources, prevention of monolithic "God classes", and the ability for teams to work on different resource hierarchies concurrently without conflicts.

---

### Part 5.2 — HTTP 422 vs 404

When a client POSTs a sensor with `roomId: "FAKE-999"`, the request URI (`/api/v1/sensors`) is valid and found — returning 404 would be misleading because the endpoint itself exists. HTTP 422 Unprocessable Entity is semantically correct because it signals that the request was syntactically valid JSON and the endpoint was located, but the entity's content could not be processed due to a logical/semantic failure: a field references a non-existent resource. The problem is in the *meaning* of the data, which 422 precisely conveys.

---

### Part 5.4 — Security Risks of Exposing Stack Traces

Exposing raw Java stack traces to API consumers is a significant security vulnerability:

1. **Class and package names** reveal internal architecture and code structure to attackers.
2. **File paths** (e.g., `/home/ubuntu/app/DataStore.java:47`) expose server directory structure.
3. **Library versions** allow cross-referencing against CVE databases to find exploitable vulnerabilities.
4. **Exception messages** reveal internal logic (e.g., `NullPointerException at DataStore`) helping attackers craft targeted malicious payloads.

The `GlobalExceptionMapper` in this project logs the full trace server-side for developers and returns only a generic message to the client.

---

### Part 5.5 — Filters vs Manual Logging

JAX-RS filters for cross-cutting concerns are superior to manual `Logger.info()` calls in every resource method because:

1. **DRY**: One filter class covers every endpoint automatically, including future ones.
2. **Separation of concerns**: Resource methods contain only business logic.
3. **Completeness**: Filters catch requests that fail before reaching a resource method (e.g., auth failures, 415 errors).
4. **Maintainability**: Changing log format means editing one class, not dozens.
5. **Richer context**: `ContainerRequestContext` and `ContainerResponseContext` provide headers, URIs, and status codes not always available inside a resource method.

# RideFlow — Local Interview Demo Results

This document records the local demo verification of RideFlow for interview and technical evaluation. All tests and live endpoints executed locally without external cloud services or paid dependencies.

---

## 1. Environment & Infrastructure Pre-flight

| Component | Target / Specification | Actual Status | Verification Method |
| :--- | :--- | :--- | :--- |
| **Java Runtime** | Java 21 (LTS) | `21.0.12.1+1-LTS-4` | `java -version` |
| **Relational Database** | PostgreSQL 17 (`rideflow_db`) | Running on port `5432` | TCP port connection test |
| **Geospatial Cache** | Redis Alpine (`rideflow-redis`) | Running on port `6379` | Docker status & Redis CLI ping |
| **Application Server** | Spring Boot 3.3.5 | Running on port `8080` (PID 10392) | Actuator & TCP listener |

---

## 2. Environment Variables Configuration

For secure local execution without hardcoded credentials or committed secrets, the following environment variables are required:

| Variable | Description | Recommended Local Configuration |
| :--- | :--- | :--- |
| `DB_URL` | JDBC connection string | `jdbc:postgresql://localhost:5432/rideflow_db` |
| `DB_USERNAME` | PostgreSQL database user | `rideflow` |
| `DB_PASSWORD` | PostgreSQL database password | Provided dynamically in local terminal session |
| `JWT_SECRET` | 256-bit signing key (>= 32 chars) | Provided dynamically in local terminal session |

### Local PowerShell Setup (Without Sharing Secrets)
```powershell
$env:JAVA_HOME   = "C:\Program Files\Java\jdk-21.0.12.1"
$env:DB_URL      = "jdbc:postgresql://localhost:5432/rideflow_db"
$env:DB_USERNAME = "rideflow"
$env:DB_PASSWORD = "<your_local_db_password>"
$env:JWT_SECRET  = "<your_256_bit_jwt_secret_key_at_least_32_characters>"
```

---

## 3. System Health & Documentation Verification

* **Health Endpoint**: `GET http://localhost:8080/actuator/health`
  * Status: `HTTP 200 OK`
  * Response: `{"status": "UP"}`
* **Interactive OpenAPI / Swagger UI**: `GET http://localhost:8080/swagger-ui.html`
  * Status: `HTTP 302` redirect to `/swagger-ui/index.html` (`HTTP 200 OK`)
* **OpenAPI 3 JSON Specification**: `GET http://localhost:8080/api-docs`
  * Status: `HTTP 200 OK`

---

## 4. Live API Demonstration Sequence

The demonstration was executed using existing local test accounts without generating redundant test entities or modifying unrelated records:

### Step 1: Driver Authentication
* **Endpoint**: `POST /api/v1/auth/login`
* **Role Verified**: `Role.DRIVER`
* **Result**: `HTTP 200 OK` — Minted signed JWT Bearer access token containing driver claims.

### Step 2: Driver Profile & Verification Inspection
* **Endpoint**: `GET /api/v1/drivers/me`
* **Headers**: `Authorization: Bearer <driver_token>`
* **Result**: `HTTP 200 OK` — License `DL-LOC-TEST-001`, verification status `APPROVED`, active availability `true`.

### Step 3: Vehicle Configuration Query
* **Endpoint**: `GET /api/v1/drivers/me/vehicle`
* **Headers**: `Authorization: Bearer <driver_token>`
* **Result**: `HTTP 200 OK` — `Toyota Camry` (`SEDAN`, `KA-01-RF-9999`).

### Step 4: Real-Time GPS Location Telemetry Ingestion
* **Endpoint**: `PUT /api/v1/drivers/me/location`
* **Headers**: `Authorization: Bearer <driver_token>`
* **Payload**: `{"latitude": 12.9716, "longitude": 77.5946, "heading": 90.0, "speed": 25.0}`
* **Result**: `HTTP 200 OK` — Ingested coordinates timestamped and stored in Redis.
* **Direct Redis Verification**:
  * `GEOPOS rideflow:drivers:geo <driverId>` returned `77.594598, 12.971599`.
  * `TTL rideflow:drivers:loc:<driverId>` confirmed active expiration window (`581s` remaining).

### Step 5: Administrator Authentication
* **Endpoint**: `POST /api/v1/auth/login`
* **Role Verified**: `Role.ADMIN`
* **Result**: `HTTP 200 OK` — Minted signed JWT Bearer access token with admin privileges.

### Step 6: Geospatial Proximity Discovery (Nearby Driver Search)
* **Endpoint**: `GET /api/v1/drivers/nearby?latitude=12.9716&longitude=77.5946&radiusKm=5.0`
* **Headers**: `Authorization: Bearer <admin_token>`
* **Result**: `HTTP 200 OK` — Redis `GEOSEARCH` discovered 1 active driver within 5.0 km radius at `0.0 km` distance with vehicle specifications attached.

### Step 7: Dynamic Fare Estimation
* **Endpoint**: `POST /api/v1/rides/fare-estimate`
* **Payload**: Pickup `(12.9716, 77.5946)`, Dropoff `(12.9352, 77.6245)`
* **Result**: `HTTP 200 OK` — Estimated fare calculated as `127.70 INR` based on configurable base fare and per-kilometer rate.

### Step 8: Historical Ride Ledger Inspection
* **Endpoint**: `GET /api/v1/rides/my-rides?page=0&size=5`
* **Headers**: `Authorization: Bearer <admin_token>`
* **Result**: `HTTP 200 OK` — Spring Data JPA paginated ledger returned existing ride history with full state metadata.

---

## 5. Automated Regression Test Suite

Executed with Java 21 against in-memory H2 database with Redis mocked:

```text
Build tool:   Maven Wrapper (mvnw.cmd, Apache Maven 3.9.9)
Java:         Oracle JDK 21.0.12.1
Command:      $env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"; .\mvnw.cmd test

Results:
Tests run:    217
Failures:     0
Errors:       0
Skipped:      0
Total Time:   48.894 s
Build Status: BUILD SUCCESS
```

---

## 6. Conclusion

RideFlow is ready for local live interview demonstrations. All endpoints, database connections, Redis geospatial queries, and authentication mechanisms operate reliably without third-party services or leaked credentials.

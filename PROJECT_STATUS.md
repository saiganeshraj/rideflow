# RideFlow — Project Status

## Current Phase

**Phase 2: Real-Time Engine & Live Redis Integration** — Complete & live verified (PostgreSQL 17, Docker Redis, WebSockets)

## Completed Tasks

| # | Task | Status |
|---|------|--------|
| 1 | Maven project with Spring Boot 3.3.5 / Java 21 | ✅ Done |
| 2 | Modular package structure (11 modules) | ✅ Done |
| 3 | PostgreSQL datasource (env-configurable) | ✅ Done |
| 4 | Flyway migration setup with initial placeholder | ✅ Done |
| 5 | Spring Boot Actuator (health, info) | ✅ Done |
| 6 | OpenAPI / Swagger UI | ✅ Done |
| 7 | Global exception handler (RFC 7807) | ✅ Done |
| 8 | Context-loading smoke test (H2, no PG required) | ✅ Done |
| 9 | `.gitignore` for Java/Maven/IDE/OS | ✅ Done |
| 10 | `README.md` with setup instructions | ✅ Done |
| 11 | `PROJECT_STATUS.md` | ✅ Done |
| 12 | Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper`) | ✅ Done |
| 13 | Task 1: User Foundation (Security & JJWT 0.12.x deps, V2 migration, Role, User, UserRepository) | ✅ Done |
| 14 | Task 2A: User Registration (`POST /api/v1/auth/register`, DTOs, BCrypt hashing, RFC 7807 400/409 handling, MockMvc tests) | ✅ Done |
| 15 | Task 2B: User Login & JWT Generation (`POST /api/v1/auth/login`, `JwtTokenProvider` JJWT 0.12.6, BCrypt verification, 401 handling, 20 tests) | ✅ Done |
| 16 | Task 2C: JWT Request Auth & Current User Profile (`JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`, `GET /api/v1/users/me`, 27 tests) | ✅ Done |
| 17 | Task 2C Smoke-Test: PostgreSQL Runtime & End-to-End Auth Smoke Test (`rideflow_db`, Flyway V2, Actuator UP, register/login/profile 200, unauth 401) | ✅ Done |
| 18 | Task 3A: Driver Entity, V3 Migration & Repository (`Driver`, `VerificationStatus`, `DriverRepository`, `V3__create_drivers_table.sql`, 31 tests) | ✅ Done |
| 19 | Task 3B: Driver Profile Registration (`POST /api/v1/drivers/register`, `DriverService`, `DriverRegistrationRequest`, `DriverResponse`, 39 tests) | ✅ Done |
| 20 | Task 3C: Retrieve Current Driver Profile (`GET /api/v1/drivers/me`, `DriverService`, `DriverResponse`, 42 tests) | ✅ Done |
| 21 | Task 3D: Driver Availability Toggle (`PATCH /api/v1/drivers/me/availability`, V4 migration, `Driver.available`, approval checks, 56 tests) | ✅ Done |
| 22 | Task 3E: Admin Driver Verification (`PATCH /api/v1/admin/drivers/{driverId}/verification`, `Role.ADMIN` RBAC, `JwtAccessDeniedHandler`, approval/rejection availability invariant, 71 tests) | ✅ Done |
| 23 | Task 3F: Vehicle Details Foundation (`POST /api/v1/drivers/me/vehicle`, `GET /api/v1/drivers/me/vehicle`, `PUT /api/v1/drivers/me/vehicle`, `Vehicle`, `VehicleType`, `VehicleRepository`, `V5__create_vehicles_table.sql`, 86 tests) | ✅ Done |
| 24 | Task 4: Ride Entity & Database Migration (`Ride`, `RideStatus`, `RideRepository`, `V6__create_rides_table.sql`, coordinate & fare validations, 93 tests) | ✅ Done |
| 25 | Task 5: Customer Ride Requests & Fare Estimation (`POST /api/v1/rides/fare-estimate`, `POST /api/v1/rides`, `FareService`, `RideService`, `RideController`, Haversine formula, INR-only ride requests, 110 tests) | ✅ Done |
| 26 | Task 6: Ride Lifecycle & State Transitions (`PATCH/POST /api/v1/rides/{id}/accept`, `arrive`, `start`, `complete`, pessimistic concurrency lock, ownership & role checks, 123 tests) | ✅ Done |
| 27 | Task 7: Ride Cancellation & History (`PATCH/POST /api/v1/rides/{id}/cancel`, `GET /api/v1/rides/my-rides`, Spring Data pagination, strict history isolation, 136 tests) | ✅ Done |
| 28 | Task 8: Simulated Ride Payments (`POST /api/v1/payments`, `GET /api/v1/payments/ride/{id}`, V7 Flyway migration, duplicate protection, ownership isolation, 149 tests) | ✅ Done |
| 29 | Task 9: Security, Validation, Authorization & Error-Handling Review (prevent driver self-acceptance, validate payment simulation path/body rideId, constrain paymentMethod length & status, handle IllegalArgumentException & HttpRequestMethodNotSupportedException, 155 tests) | ✅ Done |
| 30 | Task 10: Phase 1 Integration Verification & Completion Report (Automated test baseline: 155 tests passing, PostgreSQL integration verified, all 9 application flows audited) | ✅ Done |
| 31 | Phase 2, Task 1: Driver Location Tracking & Geospatial Ingestion (`PUT /api/v1/drivers/me/location`, `GET /api/v1/drivers/me/location`, `DriverLocationStore`, `InMemoryDriverLocationStore`, thread-safe tracking, RFC 7807 validation, 171 tests) | ✅ Done |
| 32 | Phase 2, Task 2: Proximity Search & Nearby Driver Discovery (`GET /api/v1/drivers/nearby`, `DriverDiscoveryService`, distance ordering, 10-minute freshness cutoff, active ride filtering, vehicle type filtering, RFC 7807 validation, 182 tests) | ✅ Done |
| 43 | Phase 2, Task 3: Ride Dispatch & Driver Offers (`POST /api/v1/rides/{rideId}/dispatch`, `GET /api/v1/drivers/me/offers`, `POST /api/v1/offers/{offerId}/accept`, `POST /api/v1/offers/{offerId}/reject`, `RideOffer`, V8 Flyway migration, pessimistic lock & unique index concurrency guarantees, 191 tests) | ✅ Done |
| 44 | Phase 2, Task 4: Real-Time Updates using WebSockets/STOMP (`/ws` SockJS endpoint, STOMP CONNECT JWT authentication, subscription authorization, transaction-bound domain events via `@TransactionalEventListener(phase = AFTER_COMMIT)`, `RideLifecycleEvent`, `DriverLocationUpdateEvent`, `RideOfferEvent`, customer & driver data isolation, 202 tests) | ✅ Done |
| 45 | Phase 2, Task 5: Redis Geospatial Caching & Dispatch Recovery (`spring-boot-starter-data-redis`, `RedisDriverLocationStore` with GEOADD / GEOSEARCH, 10-minute TTL per-driver keys, in-memory fallback, cascading auto re-dispatch on offer reject/expire, duplicate driver offer suppression, configurable retries & batch size, pessimistic locking on Ride, 217 tests) | ✅ Done |
| 46 | Phase 2 Live Verification: Real Redis Direct Storage Evidence (`GEOPOS`, key TTL, JSON state), Live Driver Onboarding & Admin Approval, Live Proximity Discovery & Dispatch Lifecycle | ✅ Done |

## Build & Test Results

```
Build tool:   Maven Wrapper (mvnw.cmd, Apache Maven 3.9.9)
Java:         Oracle JDK 21.0.12.1 (C:\Program Files\Java\jdk-21.0.12.1)
Spring Boot:  3.3.5

Command:      $env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"; .\mvnw.cmd test
Tests run:    217, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Live Phase 2 Integration Verification Summary

All core Phase 2 components were verified against a live local deployment (PostgreSQL 17 on `127.0.0.1:5432` and Docker Redis container `rideflow-redis` on `127.0.0.1:6379`):

1. **Role-Based Access Control & Fresh JWT Minting**:
   - `admin.local@rideflow.test` authenticated with `Role.ADMIN` claim.
   - `driver.test@rideflow.test` authenticated with `Role.DRIVER` claim.
2. **End-to-End Driver Profile & Vehicle Onboarding**:
   - Profile created via `POST /api/v1/drivers/register` with license `DL-LOC-TEST-001` (initial status `PENDING`).
   - Vehicle registered via `POST /api/v1/drivers/me/vehicle` (`SEDAN`, Toyota Camry, `KA-01-RF-9999`).
   - Admin approved profile via `PATCH /api/v1/admin/drivers/{driverId}/verification` (`status: APPROVED`).
   - Driver availability enabled via `PATCH /api/v1/drivers/me/availability` (`available: true`).
3. **Live Redis Storage Verification (No Fallback)**:
   - Driver location reported via `PUT /api/v1/drivers/me/location` (`lat: 12.9716, lon: 77.5946, heading: 180.0, speed: 35.0`).
   - Direct Docker inspection (`docker exec rideflow-redis redis-cli`):
     - `GEOPOS rideflow:drivers:geo <driverId>` returned exact coordinates `77.594598 12.971599`.
     - `GET rideflow:drivers:loc:<driverId>` confirmed JSON serialized driver payload with ISO-8601 timestamp.
     - `TTL rideflow:drivers:loc:<driverId>` confirmed positive 599s TTL (configured 600s).
   - Confirmed live write was executed directly into Redis, not the in-memory fallback.
4. **Live Nearby-Driver Discovery & Dispatch Lifecycle**:
   - `GET /api/v1/drivers/nearby?latitude=12.9716&longitude=77.5946&radiusKm=5.0` returned the active driver (`dist: 0.0 km`, vehicle details intact).
   - Customer requested ride (`POST /api/v1/rides`) ➔ Dispatched (`POST /api/v1/rides/{id}/dispatch`).
   - Driver inspected pending offers (`GET /api/v1/drivers/me/offers`) ➔ Accepted offer (`POST /api/v1/offers/{id}/accept`).
   - Full ride lifecycle transition: `ACCEPTED` ➔ `ARRIVED` ➔ `IN_PROGRESS` ➔ `COMPLETED`.
   - Simulated payment processed: `POST /api/v1/rides/{id}/payment` completed with status `SUCCESS` (`127.70 INR`).

## Real-Time Updates Architecture (WebSockets / STOMP)

- **WebSocket Endpoint**: `/ws` (with SockJS fallback, CORS-configurable via `rideflow.cors.allowed-origins`).
- **Broker Relays**: Simple in-memory message broker with destination prefixes `/topic` and `/queue`, and application destination prefix `/app`. User destination prefix is `/user`.
- **Authentication**: JWT token extraction from STOMP `CONNECT` frame native headers (`Authorization: Bearer <token>` or `token: <token>`). Tokens are validated via `JwtTokenProvider`, authenticating the `UserPrincipal` and establishing the STOMP session security context.
- **Subscription Authorization**:
  - `/topic/rides/{rideId}`: Only the owning customer or the assigned driver may subscribe. Eager fetch query prevents lazy-loading exceptions.
  - `/topic/rides/{rideId}/location`: Only the customer of the ride or the assigned driver may subscribe to live vehicle locations.
  - `/topic/drivers/{driverId}/offers`: Only the authenticated driver matching `driverId` may subscribe to incoming dispatch offers.
  - `/user/queue/**`: Private queue destinations scoped to authenticated users.
- **Transaction-Safe Event Publishing**:
  - Domain events (`RideLifecycleDomainEvent`, `DriverLocationDomainEvent`, `RideOfferDomainEvent`) are published by service layers.
  - Handled by `WebSocketEventListener` using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)`.
  - Guarantees that WebSocket messages are NEVER broadcast if the enclosing database transaction rolls back, preventing REST/WebSocket state divergence.

## Redis Geospatial Caching Architecture & Local Setup

- **Dependency**: `spring-boot-starter-data-redis` (Lettuce connection provider).
- **Geospatial Storage**: `rideflow:drivers:geo` (Redis Geospatial index via `GEOADD` and `GEOSEARCH` with radial kilometers).
- **Driver State Storage**: `rideflow:drivers:loc:{driverId}` (JSON-serialized with Jackson and configured with a 10-minute TTL via `SET EX`).
- **Expiration Sync**: When `findLocationsNear` searches the geo index, any member whose key has expired in Redis is pruned from the geo index.
- **Resilient Fallback**: `RedisDriverLocationStore` maintains consistency with `InMemoryDriverLocationStore`. If Redis is disabled (`rideflow.redis.enabled=false`) or encounters connection errors, the application seamlessly degrades to in-memory tracking without failing requests or throwing unhandled exceptions.
- **Local Development Redis (No Paid Service Required)**:
  Run locally using Docker:
  ```bash
  docker run -d --name rideflow-redis -p 6379:6379 redis:alpine
  ```
  Or via native package manager (e.g. `brew install redis` / `apt install redis-server` / WSL2).
  Configuration defaults to `localhost:6379` without password.

## Dispatch Recovery Architecture

- **Cascading Re-Dispatch**: When a driver explicitly rejects an offer or when pending offers expire, `DispatchService.recoverDispatch(rideId)` is triggered.
- **Pessimistic Concurrency Serialization**: `rideRepository.findByIdForUpdate(rideId)` locks the `Ride` row with `SELECT FOR UPDATE`, preventing race conditions where multiple threads (rejection, expiration, or acceptance) might corrupt state or generate duplicate offers.
- **Duplicate Offer Suppression**: Collects all drivers previously offered the ride (`allOffers.stream().map(o -> o.getDriver().getId())`) and filters them out, guaranteeing a driver is never offered the same ride twice.
- **Configurable Limits**:
  - `rideflow.dispatch.offer-expiry-seconds` (default: 60s)
  - `rideflow.dispatch.batch-size` (default: 5)
  - `rideflow.dispatch.max-retries` (default: 3)
  - `rideflow.dispatch.auto-re-dispatch` (default: true)
- **Exhaustion & Retry Limit Safety**: Returns explicit `EXHAUSTED_DRIVERS` when no further unoffered eligible drivers exist, and `RETRY_LIMIT_EXCEEDED` when the retry limit is reached.

## Known Limitations

- **In-Memory Simple Broker**: Embedded STOMP broker is suitable for single-node development/testing. Production clustered deployments require an external message broker relay (e.g. RabbitMQ or Redis Pub/Sub).
- **Role Elevation Security Boundary**: By architectural design, public self-promotion to `ADMIN` is prohibited; initial admin bootstrapping for local development requires direct database role elevation.

## What Is Not Yet Implemented

- Push notification integration (APNS/FCM)
- External multi-node STOMP broker relay

## Next Steps (Phase 3)

- Push notifications or driver rating & review system
- Production load testing and multi-instance deployment profile

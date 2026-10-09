# RideFlow — Project Status

## Current Phase

**Phase 1: Core Backend** — Foundation setup (in progress)

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

## Build & Test Results

```
Build tool:   Maven Daemon (mvnd 1.0.3) + Maven 3.9.11
Java:         Oracle JDK 21.0.12.1
Spring Boot:  3.3.5

Tests run:    1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time:   ~16s (warm cache)
```

Verified on: 2026-10-09

## Known Issues

| Issue | Severity | Notes |
|-------|----------|-------|
| `mvn` not on PATH | Low | `mvnd` available at `C:\mvnd\...`. Consider adding Maven wrapper (`mvnw`) or adding `mvn` to PATH. |
| Byte-buddy dynamic agent warning | Cosmetic | JVM warning from Mockito byte-buddy agent on JDK 21. Does not affect functionality. |
| `JAVA_HOME` points to JDK 17 | Low | Must set `JAVA_HOME` to JDK 21 (`C:\Program Files\Java\jdk-21.0.12.1`) before building. |

## What Is Not Yet Implemented

- User authentication (JWT)
- User / Driver entities, repositories, services, controllers
- Ride lifecycle management
- Fare calculation engine
- Payment abstractions
- Notification system
- Admin endpoints
- Spring Security configuration
- Database tables (only Flyway placeholder exists)

## Next Steps (Phase 1 Continuation)

1. **User entity & auth** — JWT-based registration/login with Spring Security
2. **Driver entity & profile** — Driver registration and availability
3. **Ride entity & lifecycle** — Request, accept, start, complete, cancel
4. **Fare calculation** — Distance-based pricing with configurable rates
5. **REST API layer** — Controllers with validation and Swagger docs
6. **Database migrations** — Real Flyway migrations for all entities
7. **Integration tests** — API-level tests with MockMvc

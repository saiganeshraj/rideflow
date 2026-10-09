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
| 12 | Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper`) | ✅ Done |

## Build & Test Results

```
Build tool:   Maven Wrapper (mvnw.cmd, Apache Maven 3.9.9)
Java:         Oracle JDK 21.0.12.1 (C:\Program Files\Java\jdk-21.0.12.1)
Spring Boot:  3.3.5

Command:      $env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"; .\mvnw.cmd clean test -B
Tests run:    1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time:   11.771 s
```

Verified on: 2026-10-09

## Tooling & Environment Notes

| Item | Status | Notes |
|------|--------|-------|
| Maven Wrapper | ✅ Available | `mvnw` and `mvnw.cmd` added with Maven 3.9.9 distribution; no system-wide `mvn` required. |
| JDK 21 | ✅ Confirmed | Confirmed at `C:\Program Files\Java\jdk-21.0.12.1`. |
| `JAVA_HOME` | Note | Default system `JAVA_HOME` points to JDK 17; set `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"` in the current terminal session before running `./mvnw`. |
| Byte-buddy dynamic agent warning | Cosmetic | JVM warning from Mockito byte-buddy agent on JDK 21. Does not affect test outcome. |

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

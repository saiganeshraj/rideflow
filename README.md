# RideFlow — Real-Time Ride Booking Backend

A Java Spring Boot backend demonstrating professional backend engineering: authentication, ride lifecycle management, fare calculation, real-time driver matching, and production-ready deployment.

## Tech Stack

| Technology       | Version / Detail        |
|------------------|-------------------------|
| Java             | 21 (Oracle JDK 21)      |
| Spring Boot      | 3.3.5                   |
| Build Tool       | Maven                   |
| Database         | PostgreSQL (local)      |
| Migrations       | Flyway                  |
| API Docs         | Swagger / OpenAPI 3     |
| Testing          | JUnit 5, H2 (in-memory) |

## Prerequisites

- **Java 21** — [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21) or any JDK 21 distribution
- **Maven 3.9+** — [Download](https://maven.apache.org/download.cgi) (or use the Maven wrapper if added)
- **PostgreSQL 15+** — Required for running the application (not required for tests)
- **Git** — For version control

## Quick Start

### 1. Clone the repository

```bash
git clone <repository-url>
cd rideflow
```

### 2. Run tests (no database required)

```bash
mvn clean test
```

Tests use an embedded H2 database, so no PostgreSQL setup is needed.

### 3. Set up PostgreSQL (for running the application)

Create a database and user:

```sql
CREATE DATABASE rideflow;
CREATE USER rideflow WITH PASSWORD 'rideflow';
GRANT ALL PRIVILEGES ON DATABASE rideflow TO rideflow;
```

### 4. Run the application

Using default connection settings:

```bash
mvn spring-boot:run
```

Or with custom database configuration:

```bash
DB_URL=jdbc:postgresql://localhost:5432/rideflow \
DB_USERNAME=rideflow \
DB_PASSWORD=rideflow \
mvn spring-boot:run
```

### 5. Verify

- **Health check:** http://localhost:8080/actuator/health
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **API docs:** http://localhost:8080/api-docs

## Project Structure

```
com.rideflow
├── RideFlowApplication.java    # Application entry point
├── auth/                        # Authentication & authorization
├── user/                        # Rider profile management
├── driver/                      # Driver profile management
├── ride/                        # Ride lifecycle
├── fare/                        # Fare calculation
├── payment/                     # Payment processing
├── notification/                # Notification dispatching
├── admin/                       # Admin endpoints
├── config/                      # App configuration (OpenAPI, etc.)
├── security/                    # Security configuration
└── common/                      # Shared exceptions, utilities
```

## Environment Variables

| Variable       | Default                                      | Description         |
|----------------|----------------------------------------------|---------------------|
| `DB_URL`       | `jdbc:postgresql://localhost:5432/rideflow`   | JDBC connection URL |
| `DB_USERNAME`  | `rideflow`                                    | Database username   |
| `DB_PASSWORD`  | `rideflow`                                    | Database password   |
| `SERVER_PORT`  | `8080`                                        | Server port         |

## Development Phases

- **Phase 1:** Core Backend — auth, profiles, ride lifecycle, fare calculation, REST APIs
- **Phase 2:** Real-Time Engine — driver matching, location tracking, Redis, WebSockets
- **Phase 3:** Production-Ready — Kafka, notifications, Docker, CI/CD, monitoring

## License

MIT

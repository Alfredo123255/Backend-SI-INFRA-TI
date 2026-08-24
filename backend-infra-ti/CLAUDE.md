# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4.1.1 REST API backend for the INFRA-TI system, using Java 21, Spring Data JPA, and PostgreSQL.

## Prerequisites

Requires **Java 21**. JDK 21 is installed at `C:\Program Files\Java\jdk-21.0.12` but the system `JAVA_HOME` may still point to JDK 17. If the build fails with "release version 21 not supported", set `JAVA_HOME` to the JDK 21 path before running Maven.

## Commands

All commands use the Maven Wrapper. On Windows, use `mvnw.cmd`; on Unix, use `./mvnw`.

```bash
# Build (skipping tests)
mvnw.cmd clean package -DskipTests

# Run the application
mvnw.cmd spring-boot:run

# Run all tests
mvnw.cmd test

# Run a single test class
mvnw.cmd test -Dtest=BackendInfraTiApplicationTests

# Run a single test method
mvnw.cmd test -Dtest=ClassName#methodName
```

## Stack

- **Spring Boot 4.1.1** with Spring Web MVC (servlet stack, not reactive)
- **Spring Data JPA** + **PostgreSQL** driver for persistence
- **Lombok** for boilerplate reduction (`@Data`, `@Builder`, `@RequiredArgsConstructor`, etc.)
- **Spring Boot DevTools** for hot reload during development

## Configuration

`application.properties` currently only sets the app name. Before running, add PostgreSQL connection details:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/<db_name>
spring.datasource.username=<user>
spring.datasource.password=<password>
spring.jpa.hibernate.ddl-auto=update
```

## Architecture

This is an early-stage project. As features are added, follow the standard Spring Boot layered pattern:

- `controller/` — `@RestController` classes handling HTTP endpoints
- `service/` — business logic, annotated with `@Service`
- `repository/` — `@Repository` interfaces extending `JpaRepository<Entity, ID>`
- `model/` (or `entity/`) — JPA `@Entity` classes mapped to PostgreSQL tables
- `dto/` — request/response objects (separate from entities)

Root package: `com.example.backendinfrati`

## Testing

The test module uses `spring-boot-starter-data-jpa-test` and `spring-boot-starter-webmvc-test`. The `contextLoads` test in `BackendInfraTiApplicationTests` requires a running PostgreSQL instance (or an in-memory override) to pass.
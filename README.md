# TaskBoard

TaskBoard is a Spring Boot REST API for managing users, projects, project members, tasks, tags, and comments.

The project demonstrates a layered backend architecture with validation, global error handling, caching, asynchronous business operations, concurrency-safe counters, and load testing.

## Features

- CRUD operations for users, projects, tasks, tags, and comments
- Project member management with roles
- Bulk member assignment in a single transaction
- Task search endpoints with filtering
- Native overdue task query
- Asynchronous project summary report generation
- Async task status tracking by `asyncTaskId`
- Thread-safe and unsafe counters for concurrency demonstration
- OpenAPI / Swagger UI

## Tech Stack

- Java 21
- Spring Boot 4.0.2
- Spring Web
- Spring Data JPA
- Bean Validation
- Spring AOP
- PostgreSQL
- Lombok
- springdoc-openapi
- JUnit 5
- Mockito
- JaCoCo
- Sonar
- JMeter

## Project Structure

```text
src/main/java/com/ykleyka/taskboard
├── aop
├── cache
├── config
├── controller
├── dto
├── exception
├── mapper
├── model
├── repository
├── service
└── validation
```

## Asynchronous Report Generation

TaskBoard includes an asynchronous business operation for generating a project summary report.

### Start report generation

`POST /api/projects/{id}/summary-report`

Response:

```json
{
  "asyncTaskId": "4d7e8147-0a50-44c7-a6b1-90dbf4187c07",
  "operationType": "PROJECT_SUMMARY_REPORT",
  "status": "SUBMITTED",
  "createdAt": "2026-04-14T12:00:00Z"
}
```

### Check task status

`GET /api/async-tasks/{asyncTaskId}`

The task lifecycle is:

`SUBMITTED -> RUNNING -> COMPLETED | FAILED`

### Report contents

The generated report contains:

- project id, name, and description
- report generation timestamp
- members count
- tasks count
- completed tasks count
- overdue tasks count
- unassigned tasks count
- high-priority tasks count
- nearest due date
- tasks grouped by status
- project members

## Concurrency Metrics

The endpoint below exposes async execution metrics and counters used for concurrency testing:

`GET /api/async-tasks/metrics`

Response fields:

- `submittedCount`
- `runningCount`
- `completedCount`
- `failedCount`
- `projectSummaryUnsafeCounter`
- `projectSummaryAtomicCounter`
- `raceConditionDetected`

`projectSummaryAtomicCounter` is updated with `AtomicInteger`.

`projectSummaryUnsafeCounter` uses a regular increment and is intentionally left non-thread-safe to demonstrate race conditions under concurrent load.

## API Overview

### Users

- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users`
- `PUT /api/users/{id}`
- `PATCH /api/users/{id}`
- `DELETE /api/users/{id}`

### Projects

- `GET /api/projects`
- `GET /api/projects/{id}`
- `POST /api/projects`
- `PUT /api/projects/{id}`
- `PATCH /api/projects/{id}`
- `DELETE /api/projects/{id}`

### Project Members

- `GET /api/projects/{id}/members`
- `GET /api/projects/{id}/members/{userId}`
- `POST /api/projects/{id}/members`
- `POST /api/projects/{id}/members/bulk`
- `PUT /api/projects/{id}/members/{userId}`
- `DELETE /api/projects/{id}/members/{userId}`

### Async Tasks

- `POST /api/projects/{id}/summary-report`
- `GET /api/async-tasks/{asyncTaskId}`
- `GET /api/async-tasks/metrics`

### Tasks

- `GET /api/tasks`
- `GET /api/tasks/{id}`
- `POST /api/tasks`
- `PUT /api/tasks/{id}`
- `PATCH /api/tasks/{id}`
- `DELETE /api/tasks/{id}`
- `GET /api/tasks/search`
- `GET /api/tasks/overdue`

### Tags

- `GET /api/tags`
- `POST /api/tags`
- `POST /api/tasks/{taskId}/tags/{tagId}`
- `DELETE /api/tasks/{taskId}/tags/{tagId}`

### Comments

- `GET /api/tasks/{taskId}/comments`
- `POST /api/tasks/{taskId}/comments`
- `PUT /api/comments/{id}`
- `DELETE /api/comments/{id}`

## Configuration

Create a `.env` file in the project root:

```properties
DB_PASSWORD=your_password
```

Default database settings are defined in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
```

## Running the Application

### Linux / macOS

```bash
./mvnw spring-boot:run
```

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

## OpenAPI

After startup:

- [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## Testing

Run unit tests:

```bash
./mvnw test
```

Generate JaCoCo report:

```bash
./mvnw verify
```

JaCoCo report path:

```text
target/site/jacoco/jacoco.xml
```

## Load Testing

The repository contains a JMeter test plan for asynchronous report submission:

- `load-tests/project-summary-report-loadtest.jmx`

## Logging

- configuration: `src/main/resources/logback-spring.xml`
- active log: `logs/taskboard.log`
- archived logs: `logs/archive`

Service execution time is additionally logged through AOP.

## Notes

- Authentication and authorization are not implemented
- Caches are in-memory
- The unsafe counter is included specifically for concurrency demonstration and is not intended to be used as a reliable business metric

## SonarCloud

[View project in SonarCloud](https://sonarcloud.io/summary/new_code?id=ykleyka_TaskBoard&branch=master)

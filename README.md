# Audit Trail Backend

A Spring Boot backend for a distributed audit trail system that captures high-volume event activity, publishes it through Kafka, persists it in MongoDB, and exposes REST APIs for querying recent and historical audit events.

> Project stack: **Spring Boot**, **Apache Kafka**, **Redis**, **MongoDB**, **Docker**, with a Flutter frontend intended to visualize and interact with the audit APIs.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Example Requests](#example-requests)
- [Testing](#testing)
- [Future Improvements](#future-improvements)

## Features

- Capture audit events from external services through REST APIs.
- Publish incoming events asynchronously using the Kafka producer-consumer model.
- Persist audit events in MongoDB for long-term storage and retrieval.
- Cache counters, service activity, and recent events in Redis for dashboard-style analytics.
- Query events by service, event type, actor, correlation ID, outcome, time range, and pagination parameters.
- Submit events individually or in batches of up to 100 events.
- Containerized local infrastructure for Kafka, Zookeeper, MongoDB, and Redis using Docker Compose.
- Designed to support a Flutter UI for visualizing audit events and backend analytics.

## Architecture

```text
Client / Flutter UI / Services
          |
          v
 Spring Boot REST API
          |
          v
 Kafka Producer  --->  Kafka Topic: audit-events  --->  Kafka Consumer
                                                       |
                                                       +--> MongoDB: audit_events
                                                       |
                                                       +--> Redis: stats + recent events cache
```

### Event Flow

1. A client emits an audit event using the REST API.
2. The backend enriches the request with a generated event ID, timestamp, correlation ID, and client IP.
3. The Kafka producer publishes the event to the configured Kafka topic.
4. The Kafka consumer receives the event asynchronously.
5. The consumer stores the event in MongoDB and updates Redis counters/recent-event caches.
6. Query and dashboard APIs read from MongoDB and Redis to serve the UI.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Backend | Java, Spring Boot |
| API | Spring Web, Jakarta Validation |
| Messaging | Apache Kafka, Spring Kafka |
| Database | MongoDB |
| Cache / Analytics | Redis |
| Local Infrastructure | Docker Compose |
| Frontend | Flutter |
| Build Tool | Maven |

## Project Structure

```text
.
├── docker-compose.yml
├── pom.xml
├── src/main/java/com/audit
│   ├── AuditTrailBackendApplication.java
│   ├── config
│   ├── consumer
│   ├── controller
│   ├── model
│   ├── producer
│   ├── repository
│   └── service
└── src/main/resources/application.properties
```

## Prerequisites

Make sure the following tools are installed:

- Java 17 or later
- Maven, or use the included Maven wrapper
- Docker and Docker Compose
- Git

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Harshit-0413/audit-trail-backend.git
cd audit-trail-backend
```

### 2. Start infrastructure services

```bash
docker compose up -d
```

This starts:

- Zookeeper on `2181`
- Kafka on `9092`
- MongoDB on `27017`
- Redis on `6379`

### 3. Run the backend

```bash
./mvnw spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

## Configuration

Default configuration is available in `src/main/resources/application.properties`.

| Property | Default | Purpose |
| --- | --- | --- |
| `server.port` | `8080` | Backend API port |
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/auditdb` | MongoDB connection URI |
| `spring.data.mongodb.database` | `auditdb` | MongoDB database name |
| `spring.data.redis.host` | `localhost` | Redis host |
| `spring.data.redis.port` | `6379` | Redis port |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker address |
| `audit.kafka.topic` | `audit-events` | Kafka topic for audit events |
| `audit.redis.stats-ttl-seconds` | `60` | Redis stats TTL setting |
| `audit.redis.recent-events-size` | `100` | Number of recent events retained in Redis |

## API Reference

Base URL:

```text
http://localhost:8080/api
```

### Emit an Audit Event

```http
POST /api/events
```

Publishes a single audit event to Kafka.

#### Request Body

```json
{
  "serviceName": "auth-service",
  "eventType": "USER_LOGIN",
  "actorId": "user-123",
  "resourceId": "session-456",
  "outcome": "SUCCESS",
  "correlationId": "req-789",
  "clientTimestamp": "2026-05-07T10:15:30Z",
  "metadata": {
    "device": "mobile",
    "method": "password"
  }
}
```

#### Response

```json
{
  "status": "accepted",
  "eventId": "generated-event-id",
  "correlationId": "req-789",
  "timestamp": "2026-05-07T10:15:31Z"
}
```

### Emit Events in Batch

```http
POST /api/events/batch
```

Publishes multiple audit events. Maximum batch size: `100` events.

#### Request Body

```json
[
  {
    "serviceName": "payment-service",
    "eventType": "PAYMENT_CREATED",
    "actorId": "user-123",
    "resourceId": "payment-001",
    "outcome": "SUCCESS"
  },
  {
    "serviceName": "payment-service",
    "eventType": "PAYMENT_FAILED",
    "actorId": "user-456",
    "resourceId": "payment-002",
    "outcome": "FAILURE"
  }
]
```

### Query Audit Events

```http
GET /api/events
```

Supports filtering and pagination.

#### Query Parameters

| Parameter | Required | Description |
| --- | --- | --- |
| `serviceName` | No | Filter by service name |
| `eventType` | No | Filter by event type |
| `outcome` | No | Filter by event outcome |
| `actorId` | No | Filter by actor/user ID |
| `correlationId` | No | Filter by correlation ID |
| `from` | No | Start timestamp in ISO-8601 format |
| `to` | No | End timestamp in ISO-8601 format |
| `page` | No | Page number, default `0` |
| `size` | No | Page size, default `20`, maximum `100` |

Example:

```http
GET /api/events?serviceName=auth-service&page=0&size=10
```

### Get Event by ID

```http
GET /api/events/{id}
```

Returns a single audit event by its generated ID.

### Get Dashboard Stats

```http
GET /api/dashboard/stats
```

Returns aggregated dashboard data such as total events, outcome counts, service activity, recent events, and generation timestamp.

## Example Requests

Emit an event:

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "auth-service",
    "eventType": "USER_LOGIN",
    "actorId": "user-123",
    "resourceId": "session-456",
    "outcome": "SUCCESS",
    "metadata": {
      "ip": "127.0.0.1",
      "source": "readme-example"
    }
  }'
```

Query events:

```bash
curl "http://localhost:8080/api/events?page=0&size=10"
```

Fetch dashboard statistics:

```bash
curl "http://localhost:8080/api/dashboard/stats"
```

## Testing

Run the test suite with:

```bash
./mvnw test
```
## Key Design Decisions

**202 Accepted instead of 201 Created**  
The event isn't in MongoDB yet when we respond - it's in Kafka.
202 is semantically correct for async processing.

**Manual Kafka acknowledgment**  
Offset is committed only after MongoDB confirms the write.
If the app crashes mid-way, Kafka redelivers on restart.
No audit event is ever silently lost.

**MongoDB for storage**  
Each service's metadata has a completely different shape.
payment-service events carry `amount`, `currency`, `gateway`.
auth-service events carry `userAgent`, `failReason`, `sessionId`.
Flexible documents handle this without schema migrations.

**Redis for dashboard stats**  
`INCR` on a Redis counter is sub-millisecond. Without Redis,
every dashboard load would trigger a MongoDB COUNT aggregation
across potentially millions of documents.

**Partition by serviceName**  
All events from the same service go to the same Kafka partition,
guaranteeing ordering within a service while different services
are processed in parallel by separate consumer threads.
## Future Improvements

- Add authentication and role-based access control for API access.
- Add OpenAPI/Swagger documentation.
- Add dead-letter topic handling for failed Kafka messages.
- Add advanced analytics by service, event type, and time windows.
- Add production-ready Dockerfile and deployment manifests.
- Add CI workflow for tests and build validation.

## Repository

- Backend: [audit-trail-backend](https://github.com/Harshit-0413/audit-trail-backend)

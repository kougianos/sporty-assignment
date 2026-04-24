# Sport Event Tracker

A Java-based microservice that tracks live sports events, periodically polls an external REST API for scores, and publishes score updates to Kafka. Includes a dead letter queue (DLQ) for failed message reprocessing.

## Prerequisites

- **Java 25**
- **Docker** (for Kafka / Zookeeper, and for Testcontainers during tests)
- **Maven 3.9+**

## Getting Started

### Quick Start (all services with one command)

```bash
chmod +x start.sh
./start.sh
```

This builds the project, starts Kafka via Docker Compose, and launches both services. Press `Ctrl+C` to stop everything.

### Manual Setup

#### 1. Start Kafka

```bash
docker compose up -d
```

This starts Zookeeper and a Kafka broker on port `9092`.

#### 2. Start the external dummy API (port 8081)

```bash
mvn -pl external-rest-dummy-api spring-boot:run
```

#### 3. Start the sport event tracker (port 8080)

```bash
mvn -pl sport-event-tracker spring-boot:run
```

### Running the Tests

Tests use **Testcontainers** (Kafka) and **WireMock** (external API mock). You only need Docker running.

```bash
mvn clean test -pl sport-event-tracker
```

## API Reference

### Update Event Status

```
POST /events/status
Content-Type: application/json

{
    "eventId": "1234",
    "status": "live"
}
```

The `eventId` field accepts both strings and numbers. The `status` field accepts:
- Strings: `"live"`, `"not live"`
- Booleans: `true` (live), `false` (not live)

**Response** (200 OK):
```json
{
    "eventId": "1234",
    "status": "live",
    "message": "Event 1234 status updated to LIVE"
}
```

**Response** (400 Bad Request: validation error):
```json
{
    "status": 400,
    "error": "Validation Failed",
    "message": "Request validation failed",
    "timestamp": "2026-04-25T00:00:00Z",
    "fieldErrors": {
        "eventId": "eventId must not be blank"
    }
}
```

### External Score API (Dummy)

```
GET http://localhost:8081/api/scores/{eventId}
```

**Response** (200 OK):
```json
{
    "eventId": "1234",
    "currentScore": "2:1"
}
```

### Kafka Topics

| Topic | Description |
|---|---|
| `sport-event-scores` | Score update messages published by the tracker |
| `sport-event-scores-dlq` | Dead letter queue for failed publish attempts |

**Score event message format:**
```json
{
    "eventId": "1234",
    "currentScore": "2:1",
    "timestamp": "2026-04-25T00:00:00Z"
}
```

## Project Structure

```
sporty-assignment/
├── docker-compose.yml              # Kafka + Zookeeper
├── start.sh                        # One-command startup script
├── pom.xml                         # Root POM (multi-module)
│
├── sport-event-tracker/            # Main microservice (port 8080)
│   └── src/main/java/com/sporty/tracker/
│       ├── config/                 # RestTemplate, Kafka topics, custom deserializers
│       ├── controller/             # REST controller (EventStatusController)
│       ├── dto/                    # Request/response records
│       ├── exception/              # Global error handler & error response
│       ├── model/                  # EventStatus enum
│       ├── service/                # Business logic, polling, Kafka producers, DLQ
│       └── SportEventTrackerApplication.java
│   └── src/test/java/             # Integration tests (Testcontainers + WireMock)
│
└── external-rest-dummy-api/        # Dummy external API (port 8081)
    └── src/main/java/com/sporty/external/
        ├── config/                 # RestTemplate config
        ├── controller/             # Score endpoint
        ├── dto/                    # Response records
        ├── scheduler/              # Event status simulator (sends requests to tracker)
        └── ExternalRestDummyApiApplication.java
```

## Design Decisions

- **In-memory event state**: Live event tracking uses a `ConcurrentHashMap`-backed set. This is sufficient for the scope of the assignment; a production, multi-pod system would use a distributed caching solution like Redis.

- **Scheduled polling**: A `@Scheduled` task iterates all live events every 10 seconds and calls the external API for each. Polling interval, retry delay, and retry multiplier are all configurable via `application.yml`.

- **Parallel polling with virtual threads**: Each live event is polled concurrently using Java virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`). This ensures polling time stays constant regardless of the number of live events: 1000 events take roughly the same wall-clock time as 1. The scheduler waits for all parallel fetches to complete before the next cycle.

- **Exponential backoff retry**: External API calls use Spring Retry with `@Retryable`: 3 attempts with configurable exponential backoff (default: 1s initial delay, 2x multiplier).

- **Dead Letter Queue**: If publishing a score event to Kafka fails, the message is sent to a DLQ topic (`sport-event-scores-dlq`). A `@KafkaListener` consumer automatically picks up DLQ messages and retries publishing to the main topic.

- **Flexible input parsing**: The `POST /events/status` endpoint accepts `eventId` as string or number, and `status` as a string (`"live"`, `"not live"`) or boolean (`true`/`false`), using custom Jackson deserializers.

- **Global error handling**: A `@RestControllerAdvice` handles validation errors, malformed requests, and unexpected exceptions with structured JSON error responses.

- **External API simulator**: The dummy API module includes a scheduler that sends randomized valid and invalid payloads to the tracker every 2 seconds, exercising both happy and error paths.

- **Test speed**: Integration tests override polling interval to 500ms and retry delay to 100ms for fast execution. All async assertions use Awaitility instead of `Thread.sleep`.

## Testing

Unit tests were intentionally omitted. Given the straightforward scope of the service, the preferred testing strategy is: real integration tests (zero mocks, real infrastructure via Testcontainers and WireMock) → integration tests with `@MockBean` components → unit tests (only when business logic becomes complex enough to warrant isolated testing). The current scope is fully covered by the first layer, all tests run against real Kafka (Testcontainers) and a real HTTP server (WireMock), with no mocked Spring beans.

12 integration tests covering:

- **Status updates**: accepting `live`/`not live` string, boolean, numeric event IDs
- **Validation**: blank event IDs, invalid status values
- **Polling & Kafka**: verifying score messages appear on the Kafka topic after polling
- **Stop polling**: verifying no external API calls after setting event to `not live`
- **Retry on failure**: verifying 3 attempts when external API returns 500
- **DLQ flow**: message published to DLQ → consumed → republished to main topic

## AI-Assisted Parts

GitHub Copilot was used to:
- Generate integration tests (Testcontainers + WireMock + Awaitility setup)
- Generate boilerplate code and initial project setup (POM files, application config, main classes)
- Generate DTOs (Java records with Lombok `@Builder`)
- Generate the external-rest-dummy-api module (score controller, event status simulator)
- Generate current documentation

All generated code was reviewed, validated, and adjusted manually.

## Tech Stack

- Java 25
- Spring Boot 3.4
- Maven (multi-module)
- Apache Kafka (via Docker)
- Spring Retry (exponential backoff)
- Spring Kafka (producer, consumer, DLQ)
- Lombok
- Testcontainers + WireMock + Awaitility (integration testing)

## Further Improvements

- Persist event state to a distributed cache (or even database) instead of in-memory storage.
- Add health check / readiness endpoints with Spring Actuator.
- Add metrics and monitoring (Micrometer / Prometheus).
- Add authentication to the status update endpoint.
- Containerize both services with Dockerfiles and add to `docker-compose.yml`.
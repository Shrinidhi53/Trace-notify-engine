# trace-notify-engine

Real-Time Notification Engine built with **Spring Boot 3.x (Java 21)**, **Apache Kafka (KRaft mode)**, **PostgreSQL**, and **WebSocket/STOMP**, featuring two distinctive capabilities:

- **Causality Tracing** – every notification records the full chain of steps it passed through (created → produced → consumed → pushed), queryable via a trace endpoint.
- **Contract Governance** – services must register a contract declaring allowed event types and a per-minute rate limit; events are validated **before** they reach Kafka, and violations are logged.

## Architecture

```
  Client (REST)                          Browser (WebSocket/STOMP)
       |                                          ^
       v                                          |
  NotificationController                   convertAndSendToUser
       |                                          |
       v                                          |
  NotificationService --(save)--> PostgreSQL      |
       |                                          |
       v                                          |
  NotificationProducer --[contract check]--> Kafka (notifications.inapp)
       |  (reject -> contract_violations)         |
       |                                          |
       v                                          |
  NotificationConsumer (@KafkaListener) ----------+
       |  retries 1s->2s->4s, then -> notifications.dlq -> DlqConsumer -> dlq_messages
       v
  CausalityService (records each step in notification_causality)
```

## Tech Stack

Java 21, Spring Boot 3.3, Spring Web/Security/Data JPA/Validation/WebSocket, Spring Kafka, Apache Kafka (KRaft, no Zookeeper), PostgreSQL 16, Flyway, Hibernate, JWT (jjwt) + BCrypt, Lombok, JUnit 5 + Mockito, Docker / docker-compose, Maven.

## Setup

```bash
cp .env.example .env          # adjust secrets (set a strong JWT_SECRET)
docker-compose up --build     # starts postgres, kafka (KRaft), and the app
```

The app listens on `http://localhost:8080`. Flyway creates the schema automatically on startup.

WebSocket test client: open `http://localhost:8080/test-client.html`, paste a JWT from `/auth/login`, and connect.

Run tests locally:

```bash
mvn test
```

## API

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | none | Register a new user |
| POST | `/auth/login` | none | Login, returns JWT |
| POST | `/api/notifications/send` | user | Trigger a notification (contract-checked → Kafka) |
| GET | `/api/notifications/me?page=0&size=10` | user | Paginated history |
| PATCH | `/api/notifications/{id}/read` | user | Mark as read |
| DELETE | `/api/notifications/{id}` | user | Delete |
| GET | `/notifications/{id}/trace` | user | Causality chain |
| GET | `/api/preferences` | user | Get preferences |
| PUT | `/api/preferences` | user | Toggle in-app notifications |
| POST | `/api/contracts/register` | admin | Register a service contract |
| GET | `/api/contracts` | admin | List contracts |
| GET | `/api/contracts/violations` | admin | List violations |
| GET | `/api/admin/violations` | admin | List violations |
| GET | `/api/admin/dlq` | admin | List DLQ messages |
| WS | `/ws?token=<JWT>` | JWT (query) | STOMP endpoint; subscribe `/user/queue/notifications` |

## Sample requests

**Register**
```json
POST /auth/register
{ "username": "alice", "email": "alice@example.com", "password": "secret123", "role": "USER" }
```
```json
{ "token": "eyJ...", "userId": "...", "username": "alice", "role": "USER" }
```

**Register a contract (admin) — required before sending**
```json
POST /api/contracts/register
{ "serviceName": "OrderService", "allowedEventTypes": ["ORDER_PLACED"], "maxFrequencyPerMinute": 60, "priorityLevel": "HIGH" }
```

**Send a notification**
```json
POST /api/notifications/send
{ "title": "Order placed", "message": "Order #42 confirmed", "type": "ORDER_PLACED", "priority": "HIGH", "serviceName": "OrderService" }
```

**Trace**
```json
GET /notifications/{id}/trace
{
  "notificationId": "...",
  "causality": [
    { "step": 1, "service": "OrderService", "event": "ORDER_PLACED", "timestamp": "..." },
    { "step": 2, "service": "OrderService", "event": "ORDER_PLACED", "timestamp": "..." },
    { "step": 3, "service": "NotificationService", "event": "EVENT_CONSUMED", "timestamp": "..." },
    { "step": 4, "service": "NotificationService", "event": "WS_PUSHED", "timestamp": "..." }
  ]
}
```

## Causality chain

Each time a notification moves through a stage, `CausalityService.record(...)` appends a row to `notification_causality` with an incrementing `step_order`. Stages: CREATED (service layer), PRODUCED (before Kafka publish), EVENT_CONSUMED (Kafka listener), WS_PUSHED (after WebSocket delivery). `GET /notifications/{id}/trace` returns the ordered list.

## Contract governance

`ContractValidationService` runs inside `NotificationProducer.publish(...)` **before** anything reaches Kafka. It checks: (1) the service is registered, (2) the event type is in `allowedEventTypes`, (3) the service is under its sliding-window per-minute rate limit (tracked in-memory per service). On failure it logs a `contract_violations` row and the request is rejected with HTTP 422. If a service is not registered, sending any event for it fails the contract check.

## Notes

- A user with `role: ADMIN` (set at registration) is required for `/api/contracts/**` and `/api/admin/**`.
- Kafka retries use exponential backoff (1s → 2s → 4s, 3 attempts); exhausted messages go to `notifications.dlq` and are persisted to `dlq_messages` for inspection.
- A Postman collection is in `postman/trace-notify-engine.postman_collection.json`.

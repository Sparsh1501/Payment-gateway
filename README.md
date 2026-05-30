# Payment Gateway Clone

A production-style payment gateway (à la Razorpay / PayU) built with **Java 17 + Spring Boot 3.3**.
It supports merchant onboarding, payment intents, hosted checkout, refunds, outbound webhook delivery
with retries, inbound provider webhooks, analytics, metrics, and a React dashboard.

> Providers run in **simulation mode** out of the box (placeholder keys), so the whole system is
> exercisable locally without live Stripe/Razorpay credentials. Supply real keys to hit the real APIs.

---

## Tech stack

| Concern            | Choice                                                        |
|--------------------|---------------------------------------------------------------|
| Language / runtime | Java 17, Spring Boot 3.3, Gradle (wrapper)                    |
| Database           | PostgreSQL 15 + Spring Data JPA / Hibernate, Flyway migrations |
| Cache / queue      | Redis 7 (idempotency cache, webhook retry sorted-set)         |
| Providers          | Stripe Java SDK, Razorpay Java SDK (adapter layer)            |
| Resilience         | Resilience4j circuit breakers + `CompletableFuture` async      |
| Auth               | Spring Security + JWT **and** API key/secret headers          |
| Async provider IO  | Spring WebFlux `WebClient`                                     |
| Observability      | Micrometer + Prometheus + Grafana                             |
| API docs           | springdoc OpenAPI 3 / Swagger UI                              |
| Frontend           | React (Vite) + Tailwind CSS + Recharts (`/frontend`)          |
| Tests              | JUnit 5 + Testcontainers (Postgres + Redis)                   |

---

## Project layout

```
src/main/java/com/paygateway/
  auth/        JWT service, API-key filter, security principal
  checkout/    hosted Thymeleaf checkout page controller
  config/      Security, WebClient, Async, OpenAPI, @ConfigurationProperties
  controller/  REST controllers (no business logic)
  dto/         request/response records + ApiResponse envelope
  entity/      JPA entities + enums
  exception/   custom exceptions + global handler
  metrics/     Micrometer instrumentation
  provider/    PaymentProvider interface + Stripe/Razorpay adapters + ProviderGateway
  repository/  Spring Data repositories
  service/     business logic (auth, payments, refunds, checkout, analytics, idempotency)
  webhook/     outbound engine (signing, retry queue, scheduler) + inbound receivers
  util/        HMAC, JSON, API credential helpers
src/main/resources/
  db/migration/  Flyway SQL
  templates/     checkout.html
  application.yml
frontend/        React Vite dashboard
docker/          postgres init, prometheus + grafana provisioning
docker-compose.yml
Dockerfile
postman/         Postman collection
```

---

## Quick start (Docker — 5 containers)

```bash
cp .env.example .env        # optionally edit secrets / provider keys
docker compose up --build
```

Spins up **app**, **postgres**, **redis**, **prometheus**, **grafana**. Flyway runs on startup.

| Service     | URL                                            |
|-------------|------------------------------------------------|
| API         | http://localhost:8080                          |
| Swagger UI  | http://localhost:8080/swagger-ui.html          |
| Health      | http://localhost:8080/actuator/health          |
| Prometheus  | http://localhost:9090                          |
| Grafana     | http://localhost:3000  (admin / admin)         |

## Run locally (without Docker)

You need Postgres + Redis running locally (or `docker compose up postgres redis`).

```bash
./gradlew build        # compiles, runs Testcontainers integration tests
./gradlew bootRun      # starts the API on :8080
```

### Frontend

```bash
cd frontend
npm install
npm run dev            # http://localhost:5173  (proxies /api -> :8080)
```

---

## Authentication

Two mechanisms are accepted on protected endpoints:

- `Authorization: Bearer <accessToken>` — 15 min access token, 7 day refresh token
- `X-API-Key: <key>` + `X-API-Secret: <secret>` — server-to-server

## Core API

| Method | Path                                          | Notes                              |
|--------|-----------------------------------------------|------------------------------------|
| POST   | `/api/v1/auth/register`                       | returns API key + secret           |
| POST   | `/api/v1/auth/login`                          | returns access + refresh tokens    |
| POST   | `/api/v1/auth/refresh`                         | new access token                   |
| POST   | `/api/v1/payment-intents`                     | `Idempotency-Key` header supported |
| GET    | `/api/v1/payment-intents/{id}`                |                                    |
| POST   | `/api/v1/payment-intents/{id}/confirm`        | charges via provider               |
| POST   | `/api/v1/payment-intents/{id}/cancel`         |                                    |
| POST   | `/api/v1/checkout/sessions`                   | returns hosted `/checkout/{id}` URL |
| GET    | `/checkout/{sessionId}`                       | branded Thymeleaf checkout page    |
| POST   | `/api/v1/refunds`                             | full or partial                    |
| POST   | `/api/v1/webhooks/endpoints`                  | register URL + events              |
| GET    | `/api/v1/webhooks/deliveries`                 | delivery log                       |
| POST   | `/api/v1/providers/stripe/webhook`            | inbound, signature verified        |
| POST   | `/api/v1/providers/razorpay/webhook`          | inbound, HMAC verified             |
| GET    | `/api/v1/analytics/summary`                   | volume, success rate, top currencies |
| GET    | `/api/v1/analytics/timeseries?days=7`         | daily revenue                      |

All responses use the envelope: `{ "success": ..., "data": ..., "error": ..., "timestamp": ... }`.

Import `postman/PaymentGateway.postman_collection.json` to run the full
register → login → create → confirm → webhook → refund flow (variables auto-captured).

---

## Key design notes

- **Idempotency** — `idempotency:{merchantId}:{key}` cached in Redis (24h TTL) with a unique
  DB constraint as a durable backstop. **Fails open**: if Redis is down, the check is skipped and a
  warning is logged.
- **Provider routing & fallback** — `ProviderGateway` runs each provider call async on a dedicated
  executor, wrapped in a Resilience4j circuit breaker. If the **Stripe** breaker is OPEN, new STRIPE
  charges are automatically re-routed to **Razorpay**.
- **Webhooks** — every payload is signed `HMAC-SHA256` (`X-Webhook-Signature`). Failed deliveries
  retry on a `1m → 5m → 30m → 2h → 24h` backoff stored in a Redis sorted-set, drained by a
  `@Scheduled` job every 30s, and marked permanently failed after 5 attempts.
- **Transactions** — payment state transitions are `@Transactional`; webhook fan-out never breaks the
  business transaction.
- **Metrics** — `payments.total`, `payments.amount.sum`, `provider.latency`, `webhooks.delivery.attempts`,
  `checkout.sessions.created` / `.completed` exported at `/actuator/prometheus`.

### Deviations worth calling out
- Dashboard auth stores JWTs in `localStorage` with an Axios refresh interceptor (the brief mentioned
  httpOnly cookies; the backend issues tokens in the response body, so token-in-storage is used).
- Provider SDK calls are guarded by a simulation mode when placeholder keys are configured.

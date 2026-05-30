# Contributing

Thanks for your interest in improving the Payment Gateway Clone!

## Getting started

```bash
git clone <your-fork-url>
cd payment-gateway
cp .env.example .env
docker compose up postgres redis   # or full: docker compose up --build
./gradlew build                    # compiles + runs Testcontainers tests (needs Docker)
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

## Ground rules

- **No business logic in controllers** — keep it in the `service` layer.
- All external config goes through `@ConfigurationProperties` (see `config/props`).
- Validate every request DTO with `@Valid` + Jakarta constraints.
- Payment/refund state transitions must stay `@Transactional`.
- Provider calls must be wrapped with structured logging and a circuit breaker.
- Never commit secrets. Only `*.env.example` files are tracked.

## Before opening a PR

1. `./gradlew build` is green (zero compile errors, tests pass).
2. `cd frontend && npm run build` is green.
3. Add/adjust tests for behavioural changes.
4. Keep commits focused and write a clear description of the "why".

## Branching & commits

- Branch off `main` (e.g. `feat/...`, `fix/...`, `chore/...`).
- Conventional, imperative commit messages are appreciated (`add`, `fix`, `update`, `refactor`).

CI (GitHub Actions) runs the backend build, frontend build, and Docker image build on every PR.

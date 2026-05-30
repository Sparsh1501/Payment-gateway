-- ============================================================
-- Payment Gateway - initial schema
-- ============================================================

CREATE TABLE merchants (
    id            UUID PRIMARY KEY,
    business_name VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    api_key       VARCHAR(255) NOT NULL UNIQUE,
    api_secret    VARCHAR(255) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE payment_intents (
    id                  UUID PRIMARY KEY,
    merchant_id         UUID NOT NULL REFERENCES merchants (id),
    amount              NUMERIC(19, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    provider            VARCHAR(20) NOT NULL,
    provider_payment_id VARCHAR(255),
    idempotency_key     VARCHAR(255),
    metadata            JSONB,
    failure_reason      VARCHAR(512),
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_intents_merchant ON payment_intents (merchant_id);
CREATE INDEX idx_payment_intents_status ON payment_intents (status);
CREATE UNIQUE INDEX uq_payment_intents_idem
    ON payment_intents (merchant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE checkout_sessions (
    id                UUID PRIMARY KEY,
    merchant_id       UUID NOT NULL REFERENCES merchants (id),
    payment_intent_id UUID NOT NULL REFERENCES payment_intents (id),
    success_url       VARCHAR(1024) NOT NULL,
    cancel_url        VARCHAR(1024) NOT NULL,
    expires_at        TIMESTAMPTZ NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    line_items        JSONB,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_checkout_sessions_merchant ON checkout_sessions (merchant_id);

CREATE TABLE refunds (
    id                UUID PRIMARY KEY,
    payment_intent_id UUID NOT NULL REFERENCES payment_intents (id),
    merchant_id       UUID NOT NULL REFERENCES merchants (id),
    amount            NUMERIC(19, 2) NOT NULL,
    reason            VARCHAR(512),
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider_refund_id VARCHAR(255),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refunds_payment_intent ON refunds (payment_intent_id);
CREATE INDEX idx_refunds_merchant ON refunds (merchant_id);

CREATE TABLE webhook_endpoints (
    id          UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants (id),
    url         VARCHAR(1024) NOT NULL,
    secret      VARCHAR(255) NOT NULL,
    events      TEXT[] NOT NULL DEFAULT '{}',
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_endpoints_merchant ON webhook_endpoints (merchant_id);

CREATE TABLE webhook_deliveries (
    id                  UUID PRIMARY KEY,
    webhook_endpoint_id UUID NOT NULL REFERENCES webhook_endpoints (id),
    event_type          VARCHAR(100) NOT NULL,
    payload             JSONB NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts            INT NOT NULL DEFAULT 0,
    next_retry_at       TIMESTAMPTZ,
    last_response_code  INT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_deliveries_endpoint ON webhook_deliveries (webhook_endpoint_id);
CREATE INDEX idx_webhook_deliveries_status ON webhook_deliveries (status);

CREATE TABLE transactions (
    id                UUID PRIMARY KEY,
    payment_intent_id UUID NOT NULL REFERENCES payment_intents (id),
    type              VARCHAR(20) NOT NULL,
    amount            NUMERIC(19, 2) NOT NULL,
    currency          VARCHAR(3) NOT NULL,
    fee               NUMERIC(19, 2) NOT NULL DEFAULT 0,
    net               NUMERIC(19, 2) NOT NULL DEFAULT 0,
    provider          VARCHAR(20) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_payment_intent ON transactions (payment_intent_id);
CREATE INDEX idx_transactions_created_at ON transactions (created_at);

-- ---------------------------------------------------------------
-- payments table
CREATE TABLE payments (
    id                       UUID PRIMARY KEY,
    order_id                 VARCHAR(64)  NOT NULL,
    customer_id              VARCHAR(64)  NOT NULL,
    amount_cents             BIGINT       NOT NULL CHECK (amount_cents > 0),
    currency                 CHAR(3)      NOT NULL DEFAULT 'USD',

    -- AUTHORIZING, AUTHORIZED, DECLINED, CAPTURING, CAPTURED, VOIDING, VOIDED,
    -- REFUNDING, REFUNDED, EXPIRED, NEEDS_REVIEW
    state                    VARCHAR(20)  NOT NULL,
    card_last4               CHAR(4)      NOT NULL,
    bank_authorization_id    VARCHAR(64),
    bank_capture_id          VARCHAR(64),
    bank_void_id             VARCHAR(64),
    bank_refund_id           VARCHAR(64),
    authorization_expires_at TIMESTAMPTZ,

    -- for the recovery job and debugging
    attempt_count            INT          NOT NULL DEFAULT 0,
    last_error_code          VARCHAR(50),

    -- timestamps for each transition
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    authorized_at            TIMESTAMPTZ,
    captured_at              TIMESTAMPTZ,
    voided_at                TIMESTAMPTZ,
    refunded_at              TIMESTAMPTZ
);

-- "payment status by order" and "history for a customer"
CREATE INDEX idx_payments_order_id    ON payments (order_id);
CREATE INDEX idx_payments_customer_id ON payments (customer_id);

-- recovery job: find payments stuck in an in-progress state
CREATE INDEX idx_payments_state_updated ON payments (state, updated_at);

-- Optional rule: only ONE active payment per order.
-- A new payment for the same order is allowed once earlier ones are DECLINED/VOIDED/REFUNDED/EXPIRED.
CREATE UNIQUE INDEX uq_payments_one_active_per_order
    ON payments (order_id)
    WHERE state NOT IN ('DECLINED', 'VOIDED', 'REFUNDED', 'EXPIRED');

-- ---------------------------------------------------------------
-- payment_events: history/audit trail.
CREATE TABLE payment_events (
    id                BIGSERIAL PRIMARY KEY,
    payment_id        UUID         NOT NULL REFERENCES payments (id),
    operation         VARCHAR(20)  NOT NULL,   -- AUTHORIZE, CAPTURE, VOID, REFUND, RECOVERY
    from_state        VARCHAR(20),
    to_state          VARCHAR(20),
    outcome           VARCHAR(20)  NOT NULL,   -- SUCCESS, PERMANENT_FAILURE, TRANSIENT_FAILURE
    error_code        VARCHAR(50),
    error_message     TEXT,
    bank_reference_id VARCHAR(64),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_events_payment_id ON payment_events (payment_id);

-- ---------------------------------------------------------------
-- idempotency_keys
CREATE TABLE idempotency_keys (
    idempotency_key  VARCHAR(255) PRIMARY KEY,
    operation        VARCHAR(20)  NOT NULL,    -- AUTHORIZE, CAPTURE, VOID, REFUND
    request_hash     CHAR(64)     NOT NULL,    -- SHA-256 of operation + payment id + body
    status           VARCHAR(15)  NOT NULL,    -- IN_PROGRESS or COMPLETED
    payment_id       UUID REFERENCES payments (id),
    response_status  INT,                      -- filled when COMPLETED
    response_body    TEXT,                     -- filled when COMPLETED
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at     TIMESTAMPTZ
);
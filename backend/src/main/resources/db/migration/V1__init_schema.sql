-- LabaLink schema v1.
-- Idempotent on purpose: it creates the schema on an empty database AND
-- upgrades a database that was created by Hibernate ddl-auto=update in
-- pre-1.0 builds (money columns were double precision, some columns missing).

-- ---------------------------------------------------------------- users
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)    NOT NULL,
    email           VARCHAR(254)   NOT NULL,
    password_hash   VARCHAR(100)   NOT NULL,
    role            VARCHAR(20)    NOT NULL DEFAULT 'CUSTOMER',
    auth_provider   VARCHAR(20)    NOT NULL DEFAULT 'LOCAL',
    wallet_balance  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    is_active       BOOLEAN        NOT NULL DEFAULT TRUE,
    token_version   INTEGER        NOT NULL DEFAULT 0,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN IF NOT EXISTS token_version INTEGER     NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at    TIMESTAMP;
ALTER TABLE users ALTER COLUMN wallet_balance TYPE NUMERIC(12, 2) USING ROUND(wallet_balance::NUMERIC, 2);
-- Legacy Google accounts stored a plain-text marker instead of a hash.
UPDATE users SET auth_provider = 'GOOGLE' WHERE password_hash = 'GOOGLE_AUTH_NO_PASSWORD';
UPDATE users SET email = LOWER(TRIM(email)) WHERE email <> LOWER(TRIM(email));

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email ON users (email);

-- ------------------------------------------------------------- machines
CREATE TABLE IF NOT EXISTS machines (
    id            BIGSERIAL PRIMARY KEY,
    machine_name  VARCHAR(100)   NOT NULL,
    machine_type  VARCHAR(20)    NOT NULL,
    status        VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE',
    qr_token      VARCHAR(64)    NOT NULL,
    hourly_rate   NUMERIC(10, 2) NOT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP
);

ALTER TABLE machines ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE machines ALTER COLUMN hourly_rate TYPE NUMERIC(10, 2) USING ROUND(hourly_rate::NUMERIC, 2);

CREATE UNIQUE INDEX IF NOT EXISTS ux_machines_qr_token ON machines (qr_token);

-- ------------------------------------------------------------- bookings
CREATE TABLE IF NOT EXISTS bookings (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT         NOT NULL REFERENCES users (id),
    machine_id        BIGINT         NOT NULL REFERENCES machines (id),
    status            VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    start_time        TIMESTAMP      NOT NULL,
    end_time          TIMESTAMP,
    duration_minutes  INTEGER        NOT NULL DEFAULT 60,
    total_price       NUMERIC(10, 2) NOT NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP
);

ALTER TABLE bookings ADD COLUMN IF NOT EXISTS duration_minutes INTEGER NOT NULL DEFAULT 60;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS updated_at       TIMESTAMP;
ALTER TABLE bookings ALTER COLUMN total_price TYPE NUMERIC(10, 2) USING ROUND(total_price::NUMERIC, 2);

CREATE INDEX IF NOT EXISTS ix_bookings_user_id    ON bookings (user_id);
CREATE INDEX IF NOT EXISTS ix_bookings_machine_id ON bookings (machine_id);
CREATE INDEX IF NOT EXISTS ix_bookings_status     ON bookings (status);

-- ------------------------------------------------------------- payments
CREATE TABLE IF NOT EXISTS payments (
    id                          BIGSERIAL PRIMARY KEY,
    user_id                     BIGINT         NOT NULL REFERENCES users (id),
    amount                      NUMERIC(12, 2) NOT NULL,
    currency                    VARCHAR(3)     NOT NULL DEFAULT 'php',
    stripe_payment_intent_id    VARCHAR(255),
    stripe_checkout_session_id  VARCHAR(255),
    provider_reference          VARCHAR(64),
    status                      VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at                  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP,
    paid_at                     TIMESTAMP
);

ALTER TABLE payments ADD COLUMN IF NOT EXISTS currency                   VARCHAR(3) NOT NULL DEFAULT 'php';
ALTER TABLE payments ADD COLUMN IF NOT EXISTS stripe_checkout_session_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS updated_at                 TIMESTAMP;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS paid_at                    TIMESTAMP;
ALTER TABLE payments ALTER COLUMN amount TYPE NUMERIC(12, 2) USING ROUND(amount::NUMERIC, 2);

CREATE INDEX IF NOT EXISTS ix_payments_user_id ON payments (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_intent
    ON payments (stripe_payment_intent_id) WHERE stripe_payment_intent_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_session
    ON payments (stripe_checkout_session_id) WHERE stripe_checkout_session_id IS NOT NULL;

-- ----------------------------------------------------------- email_logs
CREATE TABLE IF NOT EXISTS email_logs (
    id             BIGSERIAL PRIMARY KEY,
    to_email       VARCHAR(254)  NOT NULL,
    subject        VARCHAR(255)  NOT NULL,
    email_type     VARCHAR(40)   NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    error_message  VARCHAR(1000),
    sent_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE email_logs ALTER COLUMN error_message TYPE VARCHAR(1000);

-- ------------------------------------------------- integrity constraints
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_users_wallet_non_negative') THEN
        ALTER TABLE users ADD CONSTRAINT ck_users_wallet_non_negative CHECK (wallet_balance >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_machines_rate_positive') THEN
        ALTER TABLE machines ADD CONSTRAINT ck_machines_rate_positive CHECK (hourly_rate > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_bookings_price_non_negative') THEN
        ALTER TABLE bookings ADD CONSTRAINT ck_bookings_price_non_negative CHECK (total_price >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_payments_amount_positive') THEN
        ALTER TABLE payments ADD CONSTRAINT ck_payments_amount_positive CHECK (amount > 0);
    END IF;
END $$;

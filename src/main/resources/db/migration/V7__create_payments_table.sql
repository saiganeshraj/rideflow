-- RideFlow V7: Create payments table (Simulated payments)

CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ride_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50) NOT NULL DEFAULT 'SIMULATED',
    transaction_id VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_ride_id FOREIGN KEY (ride_id) REFERENCES rides (id) ON DELETE RESTRICT,
    CONSTRAINT fk_payments_customer_id FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_payments_amount CHECK (amount >= 0),
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_payments_ride_id ON payments (ride_id);
CREATE INDEX IF NOT EXISTS idx_payments_customer_id ON payments (customer_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments (status);
CREATE INDEX IF NOT EXISTS idx_payments_transaction_id ON payments (transaction_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_ride_success ON payments (ride_id) WHERE status = 'SUCCESS';

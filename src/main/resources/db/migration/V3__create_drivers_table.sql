-- RideFlow V3: Create drivers table

CREATE TABLE IF NOT EXISTS drivers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    license_number VARCHAR(50) NOT NULL,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_drivers_user_id UNIQUE (user_id),
    CONSTRAINT uk_drivers_license_number UNIQUE (license_number),
    CONSTRAINT fk_drivers_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_drivers_verification_status CHECK (verification_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_drivers_user_id ON drivers (user_id);
CREATE INDEX IF NOT EXISTS idx_drivers_license_number ON drivers (license_number);

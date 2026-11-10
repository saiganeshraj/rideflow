-- RideFlow V8: Create ride_offers table

CREATE TABLE IF NOT EXISTS ride_offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ride_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ride_offers_ride_id FOREIGN KEY (ride_id) REFERENCES rides (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ride_offers_driver_id FOREIGN KEY (driver_id) REFERENCES drivers (id) ON DELETE RESTRICT,
    CONSTRAINT chk_ride_offers_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_ride_offers_ride_id ON ride_offers (ride_id);
CREATE INDEX IF NOT EXISTS idx_ride_offers_driver_id ON ride_offers (driver_id);
CREATE INDEX IF NOT EXISTS idx_ride_offers_status ON ride_offers (status);
CREATE INDEX IF NOT EXISTS idx_ride_offers_expires_at ON ride_offers (expires_at);

-- Prevent sending duplicate offers for the same ride to the same driver
CREATE UNIQUE INDEX IF NOT EXISTS uq_ride_offers_ride_driver ON ride_offers (ride_id, driver_id);

-- Enforce at the database level that only one offer per ride can ever be in ACCEPTED status
CREATE UNIQUE INDEX IF NOT EXISTS uq_ride_offers_ride_accepted ON ride_offers (ride_id) WHERE status = 'ACCEPTED';

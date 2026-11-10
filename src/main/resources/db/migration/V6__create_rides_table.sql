-- RideFlow V6: Create rides table

CREATE TABLE IF NOT EXISTS rides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    driver_id UUID,
    pickup_latitude DOUBLE PRECISION NOT NULL,
    pickup_longitude DOUBLE PRECISION NOT NULL,
    dropoff_latitude DOUBLE PRECISION NOT NULL,
    dropoff_longitude DOUBLE PRECISION NOT NULL,
    pickup_address VARCHAR(255) NOT NULL,
    dropoff_address VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    estimated_fare NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rides_customer_id FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_rides_driver_id FOREIGN KEY (driver_id) REFERENCES drivers (id) ON DELETE SET NULL,
    CONSTRAINT chk_rides_pickup_latitude CHECK (pickup_latitude >= -90.0 AND pickup_latitude <= 90.0),
    CONSTRAINT chk_rides_pickup_longitude CHECK (pickup_longitude >= -180.0 AND pickup_longitude <= 180.0),
    CONSTRAINT chk_rides_dropoff_latitude CHECK (dropoff_latitude >= -90.0 AND dropoff_latitude <= 90.0),
    CONSTRAINT chk_rides_dropoff_longitude CHECK (dropoff_longitude >= -180.0 AND dropoff_longitude <= 180.0),
    CONSTRAINT chk_rides_estimated_fare CHECK (estimated_fare >= 0),
    CONSTRAINT chk_rides_status CHECK (status IN ('REQUESTED', 'ACCEPTED', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_rides_customer_id ON rides (customer_id);
CREATE INDEX IF NOT EXISTS idx_rides_driver_id ON rides (driver_id);
CREATE INDEX IF NOT EXISTS idx_rides_status ON rides (status);
CREATE INDEX IF NOT EXISTS idx_rides_created_at ON rides (created_at);

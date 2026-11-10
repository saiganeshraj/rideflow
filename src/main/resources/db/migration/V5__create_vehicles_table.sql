-- RideFlow V5: Create vehicles table

CREATE TABLE IF NOT EXISTS vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id UUID NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    make VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    registration_number VARCHAR(20) NOT NULL,
    color VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_vehicles_driver_id UNIQUE (driver_id),
    CONSTRAINT uk_vehicles_registration_number UNIQUE (registration_number),
    CONSTRAINT fk_vehicles_driver_id FOREIGN KEY (driver_id) REFERENCES drivers (id) ON DELETE CASCADE,
    CONSTRAINT chk_vehicles_vehicle_type CHECK (vehicle_type IN ('SEDAN', 'SUV', 'HATCHBACK', 'MOTORCYCLE', 'AUTO', 'VAN'))
);

CREATE INDEX IF NOT EXISTS idx_vehicles_driver_id ON vehicles (driver_id);
CREATE INDEX IF NOT EXISTS idx_vehicles_registration_number ON vehicles (registration_number);

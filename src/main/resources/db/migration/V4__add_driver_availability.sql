-- RideFlow V4: Add driver availability toggle

ALTER TABLE drivers ADD COLUMN IF NOT EXISTS available BOOLEAN NOT NULL DEFAULT FALSE;

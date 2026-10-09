-- RideFlow initial schema placeholder
-- Actual tables will be added in Phase 1 feature implementation.

CREATE TABLE IF NOT EXISTS flyway_init (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

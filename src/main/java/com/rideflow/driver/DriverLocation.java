package com.rideflow.driver;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable value object representing a driver's geographical location and trajectory.
 */
public class DriverLocation {

    private final UUID driverId;
    private final Double latitude;
    private final Double longitude;
    private final Double heading;
    private final Double speed;
    private final Instant updatedAt;

    @com.fasterxml.jackson.annotation.JsonCreator
    public DriverLocation(
            @com.fasterxml.jackson.annotation.JsonProperty("driverId") UUID driverId,
            @com.fasterxml.jackson.annotation.JsonProperty("latitude") Double latitude,
            @com.fasterxml.jackson.annotation.JsonProperty("longitude") Double longitude,
            @com.fasterxml.jackson.annotation.JsonProperty("heading") Double heading,
            @com.fasterxml.jackson.annotation.JsonProperty("speed") Double speed,
            @com.fasterxml.jackson.annotation.JsonProperty("updatedAt") Instant updatedAt) {
        this.driverId = Objects.requireNonNull(driverId, "driverId cannot be null");
        this.latitude = Objects.requireNonNull(latitude, "latitude cannot be null");
        this.longitude = Objects.requireNonNull(longitude, "longitude cannot be null");
        this.heading = heading;
        this.speed = speed;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getDriverId() {
        return driverId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getHeading() {
        return heading;
    }

    public Double getSpeed() {
        return speed;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DriverLocation that = (DriverLocation) o;
        return Objects.equals(driverId, that.driverId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(driverId);
    }

    @Override
    public String toString() {
        return "DriverLocation{" +
                "driverId=" + driverId +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", heading=" + heading +
                ", speed=" + speed +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

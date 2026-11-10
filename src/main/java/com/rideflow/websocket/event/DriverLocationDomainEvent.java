package com.rideflow.websocket.event;

import com.rideflow.websocket.dto.DriverLocationUpdateEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when an active driver's location updates.
 */
public class DriverLocationDomainEvent {

    private final UUID driverId;
    private final UUID rideId;
    private final Double latitude;
    private final Double longitude;
    private final Double heading;
    private final Double speed;
    private final Instant timestamp;

    public DriverLocationDomainEvent(UUID driverId, UUID rideId, Double latitude, Double longitude, Double heading, Double speed, Instant timestamp) {
        this.driverId = driverId;
        this.rideId = rideId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.heading = heading;
        this.speed = speed;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public DriverLocationUpdateEvent toDto() {
        return new DriverLocationUpdateEvent(driverId, rideId, latitude, longitude, heading, speed, timestamp);
    }

    public UUID getDriverId() {
        return driverId;
    }

    public UUID getRideId() {
        return rideId;
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

    public Instant getTimestamp() {
        return timestamp;
    }
}

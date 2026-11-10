package com.rideflow.websocket.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Real-time event payload published when a driver's geographical location updates during an active ride.
 */
public class DriverLocationUpdateEvent {

    private UUID driverId;
    private UUID rideId;
    private Double latitude;
    private Double longitude;
    private Double heading;
    private Double speed;
    private Instant timestamp;

    public DriverLocationUpdateEvent() {
    }

    public DriverLocationUpdateEvent(UUID driverId, UUID rideId, Double latitude, Double longitude, Double heading, Double speed, Instant timestamp) {
        this.driverId = driverId;
        this.rideId = rideId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.heading = heading;
        this.speed = speed;
        this.timestamp = timestamp;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public UUID getRideId() {
        return rideId;
    }

    public void setRideId(UUID rideId) {
        this.rideId = rideId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getHeading() {
        return heading;
    }

    public void setHeading(Double heading) {
        this.heading = heading;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}

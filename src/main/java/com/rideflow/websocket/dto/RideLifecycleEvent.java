package com.rideflow.websocket.dto;

import com.rideflow.ride.RideStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Real-time event payload published when a ride undergoes a lifecycle state transition.
 */
public class RideLifecycleEvent {

    private UUID rideId;
    private RideStatus status;
    private UUID customerId;
    private UUID driverId;
    private String message;
    private Instant timestamp;

    public RideLifecycleEvent() {
    }

    public RideLifecycleEvent(UUID rideId, RideStatus status, UUID customerId, UUID driverId, String message, Instant timestamp) {
        this.rideId = rideId;
        this.status = status;
        this.customerId = customerId;
        this.driverId = driverId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public UUID getRideId() {
        return rideId;
    }

    public void setRideId(UUID rideId) {
        this.rideId = rideId;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}

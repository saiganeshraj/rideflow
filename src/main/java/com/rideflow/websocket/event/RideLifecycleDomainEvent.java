package com.rideflow.websocket.event;

import com.rideflow.ride.RideStatus;
import com.rideflow.websocket.dto.RideLifecycleEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a ride transitions lifecycle state.
 */
public class RideLifecycleDomainEvent {

    private final UUID rideId;
    private final RideStatus status;
    private final UUID customerId;
    private final UUID driverId;
    private final String message;
    private final Instant timestamp;

    public RideLifecycleDomainEvent(UUID rideId, RideStatus status, UUID customerId, UUID driverId, String message, Instant timestamp) {
        this.rideId = rideId;
        this.status = status;
        this.customerId = customerId;
        this.driverId = driverId;
        this.message = message;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public RideLifecycleEvent toDto() {
        return new RideLifecycleEvent(rideId, status, customerId, driverId, message, timestamp);
    }

    public UUID getRideId() {
        return rideId;
    }

    public RideStatus getStatus() {
        return status;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}

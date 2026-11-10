package com.rideflow.ride;

/**
 * Enumeration representing the lifecycle states of a {@link Ride}.
 */
public enum RideStatus {
    /**
     * Ride has been requested by the customer and is awaiting driver acceptance.
     */
    REQUESTED,

    /**
     * A driver has accepted the ride request.
     */
    ACCEPTED,

    /**
     * Driver has arrived at the pickup location.
     */
    ARRIVED,

    /**
     * Trip is currently ongoing towards destination.
     */
    IN_PROGRESS,

    /**
     * Ride has concluded successfully.
     */
    COMPLETED,

    /**
     * Ride has been cancelled.
     */
    CANCELLED
}

package com.rideflow.dispatch;

/**
 * Enumeration representing the lifecycle states of a {@link RideOffer}.
 */
public enum RideOfferStatus {
    /**
     * Offer has been generated and dispatched to the driver; awaiting response.
     */
    PENDING,

    /**
     * Driver has accepted the ride offer and claimed the ride.
     */
    ACCEPTED,

    /**
     * Driver has actively rejected the ride offer.
     */
    REJECTED,

    /**
     * Offer validity duration elapsed without response.
     */
    EXPIRED,

    /**
     * Offer was cancelled because the ride was claimed by another driver or cancelled by the customer.
     */
    CANCELLED
}

package com.rideflow.websocket.event;

import com.rideflow.dispatch.RideOffer;
import com.rideflow.dispatch.RideOfferStatus;
import com.rideflow.websocket.dto.RideOfferEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a driver offer is generated or its status changes.
 */
public class RideOfferDomainEvent {

    private final UUID offerId;
    private final UUID rideId;
    private final UUID driverId;
    private final RideOfferStatus status;
    private final BigDecimal estimatedFare;
    private final String currency;
    private final Double pickupLatitude;
    private final Double pickupLongitude;
    private final String pickupAddress;
    private final String dropoffAddress;
    private final Instant expiresAt;
    private final Instant timestamp;

    public RideOfferDomainEvent(RideOffer offer) {
        this(
                offer.getId(),
                offer.getRide().getId(),
                offer.getDriver().getId(),
                offer.getStatus(),
                offer.getRide().getEstimatedFare(),
                offer.getRide().getCurrency(),
                offer.getRide().getPickupLatitude(),
                offer.getRide().getPickupLongitude(),
                offer.getRide().getPickupAddress(),
                offer.getRide().getDropoffAddress(),
                offer.getExpiresAt(),
                Instant.now()
        );
    }

    public RideOfferDomainEvent(UUID offerId, UUID rideId, UUID driverId, RideOfferStatus status,
                                BigDecimal estimatedFare, String currency,
                                Double pickupLatitude, Double pickupLongitude,
                                String pickupAddress, String dropoffAddress,
                                Instant expiresAt, Instant timestamp) {
        this.offerId = offerId;
        this.rideId = rideId;
        this.driverId = driverId;
        this.status = status;
        this.estimatedFare = estimatedFare;
        this.currency = currency;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.pickupAddress = pickupAddress;
        this.dropoffAddress = dropoffAddress;
        this.expiresAt = expiresAt;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public RideOfferEvent toDto() {
        return new RideOfferEvent(
                offerId, rideId, driverId, status, estimatedFare, currency,
                pickupLatitude, pickupLongitude, pickupAddress, dropoffAddress,
                expiresAt, timestamp
        );
    }

    public UUID getOfferId() {
        return offerId;
    }

    public UUID getRideId() {
        return rideId;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public RideOfferStatus getStatus() {
        return status;
    }

    public BigDecimal getEstimatedFare() {
        return estimatedFare;
    }

    public String getCurrency() {
        return currency;
    }

    public Double getPickupLatitude() {
        return pickupLatitude;
    }

    public Double getPickupLongitude() {
        return pickupLongitude;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}

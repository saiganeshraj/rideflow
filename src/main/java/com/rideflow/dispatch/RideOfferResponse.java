package com.rideflow.dispatch;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a ride offer sent to a driver.
 */
public class RideOfferResponse {

    private UUID id;
    private UUID rideId;
    private UUID driverId;
    private RideOfferStatus status;
    private BigDecimal estimatedFare;
    private String currency;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private Double dropoffLatitude;
    private Double dropoffLongitude;
    private String pickupAddress;
    private String dropoffAddress;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    public RideOfferResponse() {
    }

    public RideOfferResponse(UUID id, UUID rideId, UUID driverId, RideOfferStatus status,
                             BigDecimal estimatedFare, String currency,
                             Double pickupLatitude, Double pickupLongitude,
                             Double dropoffLatitude, Double dropoffLongitude,
                             String pickupAddress, String dropoffAddress,
                             Instant expiresAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.rideId = rideId;
        this.driverId = driverId;
        this.status = status;
        this.estimatedFare = estimatedFare;
        this.currency = currency;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.dropoffLatitude = dropoffLatitude;
        this.dropoffLongitude = dropoffLongitude;
        this.pickupAddress = pickupAddress;
        this.dropoffAddress = dropoffAddress;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static RideOfferResponse from(RideOffer offer) {
        if (offer == null) {
            return null;
        }
        return new RideOfferResponse(
                offer.getId(),
                offer.getRide() != null ? offer.getRide().getId() : null,
                offer.getDriver() != null ? offer.getDriver().getId() : null,
                offer.getStatus(),
                offer.getRide() != null ? offer.getRide().getEstimatedFare() : null,
                offer.getRide() != null ? offer.getRide().getCurrency() : null,
                offer.getRide() != null ? offer.getRide().getPickupLatitude() : null,
                offer.getRide() != null ? offer.getRide().getPickupLongitude() : null,
                offer.getRide() != null ? offer.getRide().getDropoffLatitude() : null,
                offer.getRide() != null ? offer.getRide().getDropoffLongitude() : null,
                offer.getRide() != null ? offer.getRide().getPickupAddress() : null,
                offer.getRide() != null ? offer.getRide().getDropoffAddress() : null,
                offer.getExpiresAt(),
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRideId() {
        return rideId;
    }

    public void setRideId(UUID rideId) {
        this.rideId = rideId;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public RideOfferStatus getStatus() {
        return status;
    }

    public void setStatus(RideOfferStatus status) {
        this.status = status;
    }

    public BigDecimal getEstimatedFare() {
        return estimatedFare;
    }

    public void setEstimatedFare(BigDecimal estimatedFare) {
        this.estimatedFare = estimatedFare;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(Double pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public Double getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(Double pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public Double getDropoffLatitude() {
        return dropoffLatitude;
    }

    public void setDropoffLatitude(Double dropoffLatitude) {
        this.dropoffLatitude = dropoffLatitude;
    }

    public Double getDropoffLongitude() {
        return dropoffLongitude;
    }

    public void setDropoffLongitude(Double dropoffLongitude) {
        this.dropoffLongitude = dropoffLongitude;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

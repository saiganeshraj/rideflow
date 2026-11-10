package com.rideflow.websocket.dto;

import com.rideflow.dispatch.RideOfferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Real-time event payload published when a driver offer is generated or state changed.
 */
public class RideOfferEvent {

    private UUID offerId;
    private UUID rideId;
    private UUID driverId;
    private RideOfferStatus status;
    private BigDecimal estimatedFare;
    private String currency;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupAddress;
    private String dropoffAddress;
    private Instant expiresAt;
    private Instant timestamp;

    public RideOfferEvent() {
    }

    public RideOfferEvent(UUID offerId, UUID rideId, UUID driverId, RideOfferStatus status,
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
        this.timestamp = timestamp;
    }

    public UUID getOfferId() {
        return offerId;
    }

    public void setOfferId(UUID offerId) {
        this.offerId = offerId;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}

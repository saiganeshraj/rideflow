package com.rideflow.ride;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Public response representation of a Ride.
 * Does not expose passwords, hashes, JWTs, or internal credentials.
 */
public class RideResponse {

    private UUID id;
    private UUID customerId;
    private UUID driverId;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private Double dropoffLatitude;
    private Double dropoffLongitude;
    private String pickupAddress;
    private String dropoffAddress;
    private RideStatus status;
    private BigDecimal estimatedFare;
    private String currency;
    private Instant createdAt;
    private Instant updatedAt;

    public RideResponse() {
    }

    public RideResponse(UUID id, UUID customerId, UUID driverId,
                        Double pickupLatitude, Double pickupLongitude,
                        Double dropoffLatitude, Double dropoffLongitude,
                        String pickupAddress, String dropoffAddress,
                        RideStatus status, BigDecimal estimatedFare, String currency,
                        Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.driverId = driverId;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.dropoffLatitude = dropoffLatitude;
        this.dropoffLongitude = dropoffLongitude;
        this.pickupAddress = pickupAddress;
        this.dropoffAddress = dropoffAddress;
        this.status = status;
        this.estimatedFare = estimatedFare;
        this.currency = currency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static RideResponse from(Ride ride) {
        if (ride == null) {
            return null;
        }
        return new RideResponse(
                ride.getId(),
                ride.getCustomer() != null ? ride.getCustomer().getId() : null,
                ride.getDriver() != null ? ride.getDriver().getId() : null,
                ride.getPickupLatitude(),
                ride.getPickupLongitude(),
                ride.getDropoffLatitude(),
                ride.getDropoffLongitude(),
                ride.getPickupAddress(),
                ride.getDropoffAddress(),
                ride.getStatus(),
                ride.getEstimatedFare(),
                ride.getCurrency(),
                ride.getCreatedAt(),
                ride.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
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

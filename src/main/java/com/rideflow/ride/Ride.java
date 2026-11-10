package com.rideflow.ride;

import com.rideflow.driver.Driver;
import com.rideflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a ride booking in the RideFlow platform.
 * Tracks customer request, assigned driver, geographical coordinates,
 * address information, fare estimates, and lifecycle status.
 */
@Entity
@Table(name = "rides")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column(name = "pickup_latitude", nullable = false)
    private Double pickupLatitude;

    @Column(name = "pickup_longitude", nullable = false)
    private Double pickupLongitude;

    @Column(name = "dropoff_latitude", nullable = false)
    private Double dropoffLatitude;

    @Column(name = "dropoff_longitude", nullable = false)
    private Double dropoffLongitude;

    @Column(name = "pickup_address", nullable = false, length = 255)
    private String pickupAddress;

    @Column(name = "dropoff_address", nullable = false, length = 255)
    private String dropoffAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RideStatus status = RideStatus.REQUESTED;

    @Column(name = "estimated_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedFare;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Ride() {
    }

    public Ride(User customer, Double pickupLatitude, Double pickupLongitude,
                Double dropoffLatitude, Double dropoffLongitude,
                String pickupAddress, String dropoffAddress,
                BigDecimal estimatedFare, String currency) {
        this.customer = customer;
        setPickupLatitude(pickupLatitude);
        setPickupLongitude(pickupLongitude);
        setDropoffLatitude(dropoffLatitude);
        setDropoffLongitude(dropoffLongitude);
        this.pickupAddress = pickupAddress != null ? pickupAddress.trim() : null;
        this.dropoffAddress = dropoffAddress != null ? dropoffAddress.trim() : null;
        setEstimatedFare(estimatedFare);
        this.currency = currency != null ? currency.trim().toUpperCase() : "INR";
        this.status = RideStatus.REQUESTED;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.currency == null || this.currency.isBlank()) {
            this.currency = "INR";
        } else {
            this.currency = this.currency.trim().toUpperCase();
        }
        if (this.status == null) {
            this.status = RideStatus.REQUESTED;
        }
        validateCoordinates();
        validateFare();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        if (this.currency != null) {
            this.currency = this.currency.trim().toUpperCase();
        }
        validateCoordinates();
        validateFare();
    }

    private void validateCoordinates() {
        if (pickupLatitude != null && (!Double.isFinite(pickupLatitude) || pickupLatitude < -90.0 || pickupLatitude > 90.0)) {
            throw new IllegalArgumentException("Pickup latitude must be between -90.0 and 90.0, was: " + pickupLatitude);
        }
        if (pickupLongitude != null && (!Double.isFinite(pickupLongitude) || pickupLongitude < -180.0 || pickupLongitude > 180.0)) {
            throw new IllegalArgumentException("Pickup longitude must be between -180.0 and 180.0, was: " + pickupLongitude);
        }
        if (dropoffLatitude != null && (!Double.isFinite(dropoffLatitude) || dropoffLatitude < -90.0 || dropoffLatitude > 90.0)) {
            throw new IllegalArgumentException("Dropoff latitude must be between -90.0 and 90.0, was: " + dropoffLatitude);
        }
        if (dropoffLongitude != null && (!Double.isFinite(dropoffLongitude) || dropoffLongitude < -180.0 || dropoffLongitude > 180.0)) {
            throw new IllegalArgumentException("Dropoff longitude must be between -180.0 and 180.0, was: " + dropoffLongitude);
        }
    }

    private void validateFare() {
        if (estimatedFare != null && estimatedFare.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Estimated fare cannot be negative, was: " + estimatedFare);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public Double getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(Double pickupLatitude) {
        if (pickupLatitude != null && (!Double.isFinite(pickupLatitude) || pickupLatitude < -90.0 || pickupLatitude > 90.0)) {
            throw new IllegalArgumentException("Pickup latitude must be between -90.0 and 90.0, was: " + pickupLatitude);
        }
        this.pickupLatitude = pickupLatitude;
    }

    public Double getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(Double pickupLongitude) {
        if (pickupLongitude != null && (!Double.isFinite(pickupLongitude) || pickupLongitude < -180.0 || pickupLongitude > 180.0)) {
            throw new IllegalArgumentException("Pickup longitude must be between -180.0 and 180.0, was: " + pickupLongitude);
        }
        this.pickupLongitude = pickupLongitude;
    }

    public Double getDropoffLatitude() {
        return dropoffLatitude;
    }

    public void setDropoffLatitude(Double dropoffLatitude) {
        if (dropoffLatitude != null && (!Double.isFinite(dropoffLatitude) || dropoffLatitude < -90.0 || dropoffLatitude > 90.0)) {
            throw new IllegalArgumentException("Dropoff latitude must be between -90.0 and 90.0, was: " + dropoffLatitude);
        }
        this.dropoffLatitude = dropoffLatitude;
    }

    public Double getDropoffLongitude() {
        return dropoffLongitude;
    }

    public void setDropoffLongitude(Double dropoffLongitude) {
        if (dropoffLongitude != null && (!Double.isFinite(dropoffLongitude) || dropoffLongitude < -180.0 || dropoffLongitude > 180.0)) {
            throw new IllegalArgumentException("Dropoff longitude must be between -180.0 and 180.0, was: " + dropoffLongitude);
        }
        this.dropoffLongitude = dropoffLongitude;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress != null ? pickupAddress.trim() : null;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress != null ? dropoffAddress.trim() : null;
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
        if (estimatedFare != null && estimatedFare.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Estimated fare cannot be negative, was: " + estimatedFare);
        }
        this.estimatedFare = estimatedFare;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency != null ? currency.trim().toUpperCase() : "INR";
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ride ride = (Ride) o;
        return id != null && Objects.equals(id, ride.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Ride{" +
                "id=" + id +
                ", customerId=" + (customer != null ? customer.getId() : null) +
                ", driverId=" + (driver != null ? driver.getId() : null) +
                ", pickupLatitude=" + pickupLatitude +
                ", pickupLongitude=" + pickupLongitude +
                ", dropoffLatitude=" + dropoffLatitude +
                ", dropoffLongitude=" + dropoffLongitude +
                ", pickupAddress='" + pickupAddress + '\'' +
                ", dropoffAddress='" + dropoffAddress + '\'' +
                ", status=" + status +
                ", estimatedFare=" + estimatedFare +
                ", currency='" + currency + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

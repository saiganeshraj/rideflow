package com.rideflow.driver;

import com.rideflow.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a driver in the RideFlow platform.
 * Associated with a single {@link User} account and holds verification details.
 */
@Entity
@Table(name = "drivers")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "available", nullable = false)
    private boolean available = false;

    @OneToOne(mappedBy = "driver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Vehicle vehicle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Driver() {
    }

    public Driver(User user, String licenseNumber) {
        this.user = user;
        this.licenseNumber = licenseNumber != null ? licenseNumber.trim() : null;
        this.verificationStatus = VerificationStatus.PENDING;
        this.available = false;
    }

    public Driver(User user, String licenseNumber, VerificationStatus verificationStatus) {
        this.user = user;
        this.licenseNumber = licenseNumber != null ? licenseNumber.trim() : null;
        this.verificationStatus = verificationStatus != null ? verificationStatus : VerificationStatus.PENDING;
        this.available = false;
    }

    public Driver(User user, String licenseNumber, VerificationStatus verificationStatus, boolean available) {
        this.user = user;
        this.licenseNumber = licenseNumber != null ? licenseNumber.trim() : null;
        this.verificationStatus = verificationStatus != null ? verificationStatus : VerificationStatus.PENDING;
        this.available = available;
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
        if (this.verificationStatus == null) {
            this.verificationStatus = VerificationStatus.PENDING;
        }
        if (this.licenseNumber != null) {
            this.licenseNumber = this.licenseNumber.trim();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        if (this.licenseNumber != null) {
            this.licenseNumber = this.licenseNumber.trim();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber != null ? licenseNumber.trim() : null;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus != null ? verificationStatus : VerificationStatus.PENDING;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
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
        Driver driver = (Driver) o;
        return id != null && Objects.equals(id, driver.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Driver{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", licenseNumber='" + licenseNumber + '\'' +
                ", verificationStatus=" + verificationStatus +
                ", available=" + available +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

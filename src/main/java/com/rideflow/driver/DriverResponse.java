package com.rideflow.driver;

import java.time.Instant;
import java.util.UUID;

/**
 * Public response representation of a driver profile.
 * Does not expose passwords, hashes, JWTs, or unrelated internal data.
 */
public class DriverResponse {

    private UUID id;
    private UUID userId;
    private String licenseNumber;
    private VerificationStatus verificationStatus;
    private boolean available;
    private VehicleResponse vehicle;
    private Instant createdAt;
    private Instant updatedAt;

    public DriverResponse() {
    }

    public DriverResponse(UUID id, UUID userId, String licenseNumber,
                          VerificationStatus verificationStatus, Instant createdAt, Instant updatedAt) {
        this(id, userId, licenseNumber, verificationStatus, false, null, createdAt, updatedAt);
    }

    public DriverResponse(UUID id, UUID userId, String licenseNumber,
                          VerificationStatus verificationStatus, boolean available,
                          Instant createdAt, Instant updatedAt) {
        this(id, userId, licenseNumber, verificationStatus, available, null, createdAt, updatedAt);
    }

    public DriverResponse(UUID id, UUID userId, String licenseNumber,
                          VerificationStatus verificationStatus, boolean available,
                          VehicleResponse vehicle,
                          Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.licenseNumber = licenseNumber;
        this.verificationStatus = verificationStatus;
        this.available = available;
        this.vehicle = vehicle;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DriverResponse from(Driver driver) {
        return from(driver, driver != null ? driver.getVehicle() : null);
    }

    public static DriverResponse from(Driver driver, Vehicle vehicle) {
        if (driver == null) {
            return null;
        }
        Vehicle resolvedVehicle = vehicle != null ? vehicle : driver.getVehicle();
        return new DriverResponse(
                driver.getId(),
                driver.getUser().getId(),
                driver.getLicenseNumber(),
                driver.getVerificationStatus(),
                driver.isAvailable(),
                resolvedVehicle != null ? VehicleResponse.from(resolvedVehicle) : null,
                driver.getCreatedAt(),
                driver.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public VehicleResponse getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleResponse vehicle) {
        this.vehicle = vehicle;
    }
}

package com.rideflow.driver;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for driver profile registration.
 * Accepts only the driving license number; user identity is derived from the security context.
 */
public class DriverRegistrationRequest {

    @NotBlank(message = "License number is required")
    @Size(min = 5, max = 50, message = "License number must be between 5 and 50 characters")
    private String licenseNumber;

    public DriverRegistrationRequest() {
    }

    public DriverRegistrationRequest(String licenseNumber) {
        this.licenseNumber = licenseNumber != null ? licenseNumber.trim() : null;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber != null ? licenseNumber.trim() : null;
    }
}

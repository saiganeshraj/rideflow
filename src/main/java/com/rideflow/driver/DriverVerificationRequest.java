package com.rideflow.driver;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for administrator driver verification.
 * Accepts only APPROVED or REJECTED statuses.
 */
@Schema(description = "Request payload for updating driver verification status")
public class DriverVerificationRequest {

    @NotNull(message = "Verification status is required")
    @JsonAlias({"status", "verificationStatus"})
    @Schema(description = "Target verification status (APPROVED or REJECTED)", example = "APPROVED", requiredMode = Schema.RequiredMode.REQUIRED)
    private VerificationStatus status;

    public DriverVerificationRequest() {
    }

    public DriverVerificationRequest(VerificationStatus status) {
        this.status = status;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public VerificationStatus getVerificationStatus() {
        return status;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.status = verificationStatus;
    }
}

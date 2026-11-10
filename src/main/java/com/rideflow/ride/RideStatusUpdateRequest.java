package com.rideflow.ride;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating ride status during lifecycle transitions.
 */
@Schema(description = "Request payload for updating ride status")
public class RideStatusUpdateRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Target ride status", example = "ACCEPTED")
    private RideStatus status;

    public RideStatusUpdateRequest() {
    }

    public RideStatusUpdateRequest(RideStatus status) {
        this.status = status;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }
}

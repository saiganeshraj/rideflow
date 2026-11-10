package com.rideflow.driver;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating driver availability status.
 * Accepts only the available boolean flag.
 */
@Schema(description = "Request payload for updating driver availability")
public class DriverAvailabilityRequest {

    @NotNull(message = "Availability status is required")
    @Schema(description = "Availability status of the driver", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean available;

    public DriverAvailabilityRequest() {
    }

    public DriverAvailabilityRequest(Boolean available) {
        this.available = available;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}

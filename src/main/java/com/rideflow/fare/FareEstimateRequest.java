package com.rideflow.fare;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for calculating fare estimate between two coordinates.
 */
@Schema(description = "Request payload for fare estimation")
public class FareEstimateRequest {

    @NotNull(message = "Pickup latitude is required")
    @DecimalMin(value = "-90.0", message = "Pickup latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", message = "Pickup latitude must be between -90.0 and 90.0")
    @Schema(description = "Pickup latitude", example = "12.9716", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    @DecimalMin(value = "-180.0", message = "Pickup longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", message = "Pickup longitude must be between -180.0 and 180.0")
    @Schema(description = "Pickup longitude", example = "77.5946", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double pickupLongitude;

    @NotNull(message = "Dropoff latitude is required")
    @DecimalMin(value = "-90.0", message = "Dropoff latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", message = "Dropoff latitude must be between -90.0 and 90.0")
    @Schema(description = "Dropoff latitude", example = "12.9352", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double dropoffLatitude;

    @NotNull(message = "Dropoff longitude is required")
    @DecimalMin(value = "-180.0", message = "Dropoff longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", message = "Dropoff longitude must be between -180.0 and 180.0")
    @Schema(description = "Dropoff longitude", example = "77.6245", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double dropoffLongitude;

    public FareEstimateRequest() {
    }

    public FareEstimateRequest(Double pickupLatitude, Double pickupLongitude,
                               Double dropoffLatitude, Double dropoffLongitude) {
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.dropoffLatitude = dropoffLatitude;
        this.dropoffLongitude = dropoffLongitude;
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
}

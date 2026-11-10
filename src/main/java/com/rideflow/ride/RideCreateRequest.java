package com.rideflow.ride;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new ride request.
 * Customer identity is derived exclusively from the authenticated principal.
 */
@Schema(description = "Request payload for requesting a ride")
public class RideCreateRequest {

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

    @NotBlank(message = "Pickup address is required")
    @Size(max = 255, message = "Pickup address must not exceed 255 characters")
    @Schema(description = "Pickup street address", example = "MG Road, Bangalore", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pickupAddress;

    @NotBlank(message = "Dropoff address is required")
    @Size(max = 255, message = "Dropoff address must not exceed 255 characters")
    @Schema(description = "Dropoff street address", example = "Koramangala, Bangalore", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dropoffAddress;

    public RideCreateRequest() {
    }

    public RideCreateRequest(Double pickupLatitude, Double pickupLongitude,
                             Double dropoffLatitude, Double dropoffLongitude,
                             String pickupAddress, String dropoffAddress) {
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.dropoffLatitude = dropoffLatitude;
        this.dropoffLongitude = dropoffLongitude;
        this.pickupAddress = pickupAddress;
        this.dropoffAddress = dropoffAddress;
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

}

package com.rideflow.driver;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating driver real-time geographical coordinates.
 */
@Schema(description = "Request payload for updating driver geographical location")
public class DriverLocationUpdateRequest {

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90.0 and 90.0")
    @Schema(description = "Current latitude coordinate", example = "12.9716", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180.0 and 180.0")
    @Schema(description = "Current longitude coordinate", example = "77.5946", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double longitude;

    @DecimalMin(value = "0.0", message = "Heading must be between 0.0 and 360.0 degrees")
    @DecimalMax(value = "360.0", message = "Heading must be between 0.0 and 360.0 degrees")
    @Schema(description = "Direction of travel in degrees (0.0 - 360.0)", example = "180.0")
    private Double heading;

    @DecimalMin(value = "0.0", message = "Speed cannot be negative")
    @Schema(description = "Current speed in km/h", example = "45.5")
    private Double speed;

    public DriverLocationUpdateRequest() {
    }

    public DriverLocationUpdateRequest(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public DriverLocationUpdateRequest(Double latitude, Double longitude, Double heading, Double speed) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.heading = heading;
        this.speed = speed;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getHeading() {
        return heading;
    }

    public void setHeading(Double heading) {
        this.heading = heading;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }
}

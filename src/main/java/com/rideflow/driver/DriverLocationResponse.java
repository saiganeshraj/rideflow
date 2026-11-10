package com.rideflow.driver;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Public response representing a driver's reported geographical location.
 */
@Schema(description = "Response representing driver geographical location")
public class DriverLocationResponse {

    @Schema(description = "UUID of the driver", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID driverId;

    @Schema(description = "Current latitude coordinate", example = "12.9716")
    private Double latitude;

    @Schema(description = "Current longitude coordinate", example = "77.5946")
    private Double longitude;

    @Schema(description = "Direction of travel in degrees", example = "180.0")
    private Double heading;

    @Schema(description = "Current speed in km/h", example = "45.5")
    private Double speed;

    @Schema(description = "Timestamp when location was reported")
    private Instant updatedAt;

    public DriverLocationResponse() {
    }

    public DriverLocationResponse(UUID driverId, Double latitude, Double longitude, Double heading, Double speed, Instant updatedAt) {
        this.driverId = driverId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.heading = heading;
        this.speed = speed;
        this.updatedAt = updatedAt;
    }

    public static DriverLocationResponse from(DriverLocation location) {
        if (location == null) {
            return null;
        }
        return new DriverLocationResponse(
                location.getDriverId(),
                location.getLatitude(),
                location.getLongitude(),
                location.getHeading(),
                location.getSpeed(),
                location.getUpdatedAt()
        );
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

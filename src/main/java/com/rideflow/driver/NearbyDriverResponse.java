package com.rideflow.driver;

import java.time.Instant;
import java.util.UUID;

/**
 * Public response representation of a nearby eligible driver discovered in proximity search.
 */
public class NearbyDriverResponse {

    private UUID driverId;
    private double distanceKm;
    private Double latitude;
    private Double longitude;
    private Double heading;
    private Double speed;
    private VehicleResponse vehicle;
    private Instant locationUpdatedAt;

    public NearbyDriverResponse() {
    }

    public NearbyDriverResponse(UUID driverId, double distanceKm, Double latitude, Double longitude,
                                Double heading, Double speed, VehicleResponse vehicle, Instant locationUpdatedAt) {
        this.driverId = driverId;
        this.distanceKm = distanceKm;
        this.latitude = latitude;
        this.longitude = longitude;
        this.heading = heading;
        this.speed = speed;
        this.vehicle = vehicle;
        this.locationUpdatedAt = locationUpdatedAt;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
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

    public VehicleResponse getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleResponse vehicle) {
        this.vehicle = vehicle;
    }

    public Instant getLocationUpdatedAt() {
        return locationUpdatedAt;
    }

    public void setLocationUpdatedAt(Instant locationUpdatedAt) {
        this.locationUpdatedAt = locationUpdatedAt;
    }
}

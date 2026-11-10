package com.rideflow.dispatch;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request payload for dispatching a ride request to nearby eligible drivers.
 */
public class RideDispatchRequest {

    @DecimalMin(value = "0.1", message = "Radius must be at least 0.1 km")
    @DecimalMax(value = "20.0", message = "Radius cannot exceed 20.0 km")
    private Double radiusKm;

    private String vehicleType;

    @Min(value = 1, message = "maxOffers must be at least 1")
    @Max(value = 20, message = "maxOffers cannot exceed 20")
    private Integer maxOffers;

    public RideDispatchRequest() {
    }

    public RideDispatchRequest(Double radiusKm, String vehicleType, Integer maxOffers) {
        this.radiusKm = radiusKm;
        this.vehicleType = vehicleType;
        this.maxOffers = maxOffers;
    }

    public Double getRadiusKm() {
        return radiusKm;
    }

    public void setRadiusKm(Double radiusKm) {
        this.radiusKm = radiusKm;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Integer getMaxOffers() {
        return maxOffers;
    }

    public void setMaxOffers(Integer maxOffers) {
        this.maxOffers = maxOffers;
    }
}

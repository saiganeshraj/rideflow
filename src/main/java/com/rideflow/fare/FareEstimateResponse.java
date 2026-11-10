package com.rideflow.fare;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Response payload containing calculated distance and estimated fare.
 */
@Schema(description = "Response payload for fare estimation")
public class FareEstimateResponse {

    @Schema(description = "Calculated direct distance in kilometers", example = "5.82")
    private double distanceKm;

    @Schema(description = "Estimated fare amount", example = "137.30")
    private BigDecimal estimatedFare;

    @Schema(description = "Currency of the estimated fare", example = "INR")
    private String currency;

    public FareEstimateResponse() {
    }

    public FareEstimateResponse(double distanceKm, BigDecimal estimatedFare, String currency) {
        this.distanceKm = distanceKm;
        this.estimatedFare = estimatedFare;
        this.currency = currency;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public BigDecimal getEstimatedFare() {
        return estimatedFare;
    }

    public void setEstimatedFare(BigDecimal estimatedFare) {
        this.estimatedFare = estimatedFare;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}

package com.rideflow.fare;

import com.rideflow.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service providing distance calculation and fare estimation.
 * <p>
 * Distance is computed via the Haversine great-circle formula assuming a spherical Earth (radius 6371.0 km).
 * Fare calculation formula:
 * <pre>
 *   fare = max(minimumFare, baseFare + (distanceKm * perKmRate))
 * </pre>
 * Values are rounded to 2 decimal places using {@link RoundingMode#HALF_UP}.
 */
@Service
public class FareService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final String CURRENCY = "INR";

    private final BigDecimal baseFare;
    private final BigDecimal perKmRate;
    private final BigDecimal minimumFare;

    public FareService(
            @Value("${rideflow.fare.base-fare:50.00}") BigDecimal baseFare,
            @Value("${rideflow.fare.per-km-rate:15.00}") BigDecimal perKmRate,
            @Value("${rideflow.fare.minimum-fare:50.00}") BigDecimal minimumFare) {
        this.baseFare = baseFare;
        this.perKmRate = perKmRate;
        this.minimumFare = minimumFare;
    }

    /**
     * Calculates great-circle distance between two coordinates in kilometers using Haversine formula.
     *
     * @param lat1 latitude of first point
     * @param lon1 longitude of first point
     * @param lat2 latitude of second point
     * @param lon2 longitude of second point
     * @return distance in kilometers rounded to two decimal places
     */
    public double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        double distance = EARTH_RADIUS_KM * c;
        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Estimates fare for the provided estimate request using the configured default currency.
     *
     * @param request the fare estimate request
     * @return fare estimate response
     */
    public FareEstimateResponse estimateFare(FareEstimateRequest request) {
        if (request == null || request.getPickupLatitude() == null || request.getPickupLongitude() == null
                || request.getDropoffLatitude() == null || request.getDropoffLongitude() == null) {
            throw new BadRequestException("All coordinate parameters are required for fare estimation");
        }
        return estimateFare(
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                request.getDropoffLatitude(),
                request.getDropoffLongitude()
        );
    }

    /**
     * Estimates fare for specified coordinates using the configured default currency.
     *
     * @param pickupLat  pickup latitude
     * @param pickupLon  pickup longitude
     * @param dropoffLat dropoff latitude
     * @param dropoffLon dropoff longitude
     * @return fare estimate response
     */
    public FareEstimateResponse estimateFare(double pickupLat, double pickupLon,
                                             double dropoffLat, double dropoffLon) {
        validateCoordinates(pickupLat, pickupLon, dropoffLat, dropoffLon);

        double distanceKm = calculateDistanceKm(pickupLat, pickupLon, dropoffLat, dropoffLon);
        BigDecimal distanceCost = perKmRate.multiply(BigDecimal.valueOf(distanceKm));
        BigDecimal calculated = baseFare.add(distanceCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalFare = calculated.max(minimumFare);

        return new FareEstimateResponse(distanceKm, finalFare, CURRENCY);
    }

    private void validateCoordinates(double pickupLat, double pickupLon, double dropoffLat, double dropoffLon) {
        validateLatitude(pickupLat, "Pickup latitude");
        validateLongitude(pickupLon, "Pickup longitude");
        validateLatitude(dropoffLat, "Dropoff latitude");
        validateLongitude(dropoffLon, "Dropoff longitude");

        if (Double.compare(pickupLat, dropoffLat) == 0 && Double.compare(pickupLon, dropoffLon) == 0) {
            throw new BadRequestException("Pickup and dropoff coordinates must be different");
        }
    }

    private void validateLatitude(double latitude, String label) {
        if (!Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            throw new BadRequestException(label + " must be between -90.0 and 90.0");
        }
    }

    private void validateLongitude(double longitude, String label) {
        if (!Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            throw new BadRequestException(label + " must be between -180.0 and 180.0");
        }
    }

    public BigDecimal getBaseFare() {
        return baseFare;
    }

    public BigDecimal getPerKmRate() {
        return perKmRate;
    }

    public BigDecimal getMinimumFare() {
        return minimumFare;
    }

    public String getDefaultCurrency() {
        return CURRENCY;
    }
}

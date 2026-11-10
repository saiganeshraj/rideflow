package com.rideflow.fare;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FareServiceTest {

    private FareService fareService;

    @BeforeEach
    void setUp() {
        fareService = new FareService(
                new BigDecimal("50.00"),
                new BigDecimal("15.00"),
                new BigDecimal("50.00")
        );
    }

    @Test
    void testCalculateDistanceKmKnownLocations() {
        // MG Road to Koramangala, Bangalore (~5.8 km)
        double distance = fareService.calculateDistanceKm(12.9716, 77.5946, 12.9352, 77.6245);
        assertTrue(distance > 5.0 && distance < 6.5, "Distance should be approximately 5.8 km, was: " + distance);
    }

    @Test
    void testCalculateDistanceKmIdenticalCoordinates() {
        double distance = fareService.calculateDistanceKm(12.9716, 77.5946, 12.9716, 77.5946);
        assertEquals(0.0, distance, 0.001);
    }

    @Test
    void testEstimateFareWithMinimumFareEnforced() {
        FareEstimateResponse response = fareService.estimateFare(12.9716, 77.5946, 12.97160001, 77.5946);
        assertEquals(new BigDecimal("50.00"), response.getEstimatedFare());
        assertEquals("INR", response.getCurrency());
    }

    @Test
    void testEstimateFareWithDistanceBasedPricing() {
        // Distance ~ 10 km: base 50 + (10 * 15) = 200.00
        FareEstimateRequest request = new FareEstimateRequest(12.9716, 77.5946, 12.9000, 77.5500);
        FareEstimateResponse response = fareService.estimateFare(request);

        assertTrue(response.getDistanceKm() > 8.0);
        BigDecimal expectedMin = new BigDecimal("50.00").add(new BigDecimal("15.00").multiply(BigDecimal.valueOf(response.getDistanceKm())));
        assertEquals(expectedMin.setScale(2, java.math.RoundingMode.HALF_UP), response.getEstimatedFare());
        assertEquals("INR", response.getCurrency());
    }

    @Test
    void testEstimateFareRoundsMoneyHalfUp() {
        FareService roundingService = new FareService(
                new BigDecimal("10.00"),
                new BigDecimal("1.005"),
                new BigDecimal("0.00")
        );

        FareEstimateResponse response = roundingService.estimateFare(0.0, 0.0, 0.0, 0.008993216059187304);

        assertEquals(1.0, response.getDistanceKm());
        assertEquals(new BigDecimal("11.01"), response.getEstimatedFare());
        assertEquals("INR", response.getCurrency());
    }

    @Test
    void testEstimateFareRejectsIdenticalCoordinates() {
        assertThrows(com.rideflow.common.exception.BadRequestException.class, () ->
                fareService.estimateFare(12.9716, 77.5946, 12.9716, 77.5946));
    }

    @Test
    void testEstimateFareRejectsInvalidCoordinates() {
        assertThrows(com.rideflow.common.exception.BadRequestException.class, () ->
                fareService.estimateFare(91.0, 77.5946, 12.9352, 77.6245));
        assertThrows(com.rideflow.common.exception.BadRequestException.class, () ->
                fareService.estimateFare(12.9716, Double.NaN, 12.9352, 77.6245));
        assertThrows(com.rideflow.common.exception.BadRequestException.class, () ->
                fareService.estimateFare(12.9716, Double.POSITIVE_INFINITY, 12.9352, 77.6245));
        assertThrows(com.rideflow.common.exception.BadRequestException.class, () ->
                fareService.estimateFare(new FareEstimateRequest(null, 77.5946, 12.9352, 77.6245)));
    }
}

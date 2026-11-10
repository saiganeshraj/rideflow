package com.rideflow.driver;

import com.rideflow.common.exception.BadRequestException;
import com.rideflow.fare.FareService;
import com.rideflow.ride.RideRepository;
import com.rideflow.ride.RideStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for discovering eligible nearby drivers in real-time.
 * Evaluates geographic proximity, location freshness, driver availability,
 * verification approval, vehicle registration, and active ride conflicts.
 */
@Service
public class DriverDiscoveryService {

    public static final double DEFAULT_RADIUS_KM = 5.0;
    public static final double MAX_RADIUS_KM = 20.0;
    public static final Duration MAX_LOCATION_AGE = Duration.ofMinutes(10);

    private static final Set<RideStatus> ACTIVE_RIDE_STATUSES = Set.of(
            RideStatus.ACCEPTED,
            RideStatus.ARRIVED,
            RideStatus.IN_PROGRESS
    );

    private final DriverLocationStore driverLocationStore;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final RideRepository rideRepository;
    private final FareService fareService;

    public DriverDiscoveryService(DriverLocationStore driverLocationStore,
                                  DriverRepository driverRepository,
                                  VehicleRepository vehicleRepository,
                                  RideRepository rideRepository,
                                  FareService fareService) {
        this.driverLocationStore = driverLocationStore;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.rideRepository = rideRepository;
        this.fareService = fareService;
    }

    /**
     * Finds eligible nearby drivers within a given radius of the pickup coordinates.
     *
     * @param latitude pickup latitude (required, -90.0 to 90.0)
     * @param longitude pickup longitude (required, -180.0 to 180.0)
     * @param radiusKm optional radius in kilometers (default 5.0 km, max 20.0 km)
     * @param vehicleType optional vehicle type filter
     * @return list of nearby drivers sorted by distance ascending
     */
    @Transactional(readOnly = true)
    public List<NearbyDriverResponse> findNearbyDrivers(Double latitude, Double longitude, Double radiusKm, String vehicleType) {
        validateCoordinates(latitude, longitude);
        double effectiveRadius = validateAndResolveRadius(radiusKm);
        VehicleType parsedVehicleType = parseVehicleType(vehicleType);

        Instant cutoff = Instant.now().minus(MAX_LOCATION_AGE);

        Collection<DriverLocation> locations = driverLocationStore.findLocationsNear(latitude, longitude, effectiveRadius);
        if (locations == null || locations.isEmpty()) {
            return Collections.emptyList();
        }

        List<NearbyDriverResponse> nearbyDrivers = new ArrayList<>();

        for (DriverLocation loc : locations) {
            if (loc == null || loc.getDriverId() == null || loc.getLatitude() == null || loc.getLongitude() == null) {
                continue;
            }

            // Freshness check: updated within the last 10 minutes
            if (loc.getUpdatedAt() == null || loc.getUpdatedAt().isBefore(cutoff)) {
                continue;
            }

            // Distance calculation using FareService Haversine formula
            double distanceKm = fareService.calculateDistanceKm(latitude, longitude, loc.getLatitude(), loc.getLongitude());
            if (distanceKm > effectiveRadius) {
                continue;
            }

            // Driver eligibility check
            Optional<Driver> driverOpt = driverRepository.findById(loc.getDriverId());
            if (driverOpt.isEmpty()) {
                continue;
            }
            Driver driver = driverOpt.get();

            // Approved and available check
            if (driver.getVerificationStatus() != VerificationStatus.APPROVED || !driver.isAvailable()) {
                continue;
            }

            // Registered vehicle check
            Vehicle vehicle = driver.getVehicle();
            if (vehicle == null) {
                vehicle = vehicleRepository.findByDriverId(driver.getId()).orElse(null);
            }
            if (vehicle == null) {
                continue;
            }

            // Optional vehicle type filter
            if (parsedVehicleType != null && vehicle.getVehicleType() != parsedVehicleType) {
                continue;
            }

            // Active ride check: no active ride with ACCEPTED, ARRIVED, or IN_PROGRESS
            boolean hasActiveRide = rideRepository.existsByDriverIdAndStatusIn(driver.getId(), ACTIVE_RIDE_STATUSES);
            if (hasActiveRide) {
                continue;
            }

            nearbyDrivers.add(new NearbyDriverResponse(
                    driver.getId(),
                    distanceKm,
                    loc.getLatitude(),
                    loc.getLongitude(),
                    loc.getHeading(),
                    loc.getSpeed(),
                    VehicleResponse.from(vehicle),
                    loc.getUpdatedAt()
            ));
        }

        // Sort results by distance ascending, secondary sort by driverId for stability
        nearbyDrivers.sort(Comparator.comparingDouble(NearbyDriverResponse::getDistanceKm)
                .thenComparing(r -> r.getDriverId().toString()));

        return nearbyDrivers;
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || !Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            throw new BadRequestException("Latitude must be between -90.0 and 90.0");
        }
        if (longitude == null || !Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            throw new BadRequestException("Longitude must be between -180.0 and 180.0");
        }
    }

    private double validateAndResolveRadius(Double radiusKm) {
        if (radiusKm == null) {
            return DEFAULT_RADIUS_KM;
        }
        if (!Double.isFinite(radiusKm) || radiusKm <= 0.0 || radiusKm > MAX_RADIUS_KM) {
            throw new BadRequestException("Radius must be greater than 0 and at most 20 km");
        }
        return radiusKm;
    }

    private VehicleType parseVehicleType(String vehicleType) {
        if (vehicleType == null || vehicleType.trim().isEmpty()) {
            return null;
        }
        try {
            return VehicleType.valueOf(vehicleType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid vehicle type: " + vehicleType);
        }
    }
}

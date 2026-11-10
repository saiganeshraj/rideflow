package com.rideflow.driver;

import com.rideflow.common.exception.BadRequestException;
import com.rideflow.common.exception.DuplicateResourceException;
import com.rideflow.common.exception.ResourceNotFoundException;
import com.rideflow.security.UserPrincipal;
import com.rideflow.user.Role;
import com.rideflow.user.User;
import com.rideflow.user.UserRepository;
import com.rideflow.ride.Ride;
import com.rideflow.ride.RideRepository;
import com.rideflow.ride.RideStatus;
import com.rideflow.websocket.event.DriverLocationDomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service managing driver profile lifecycle and business operations.
 */
@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverLocationStore driverLocationStore;
    private final RideRepository rideRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DriverService(DriverRepository driverRepository,
                         UserRepository userRepository,
                         VehicleRepository vehicleRepository,
                         DriverLocationStore driverLocationStore,
                         RideRepository rideRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverLocationStore = driverLocationStore;
        this.rideRepository = rideRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Registers a new driver profile associated with the specified user.
     * Always sets verification status to PENDING.
     * Rejects duplicate user associations and duplicate license numbers with 409 Conflict.
     *
     * @param userId the ID of the authenticated user
     * @param request the registration request
     * @return safe DriverResponse
     */
    @Transactional
    public DriverResponse registerDriver(UUID userId, DriverRegistrationRequest request) {
        if (driverRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException("Driver profile already exists for this user");
        }

        String normalizedLicense = request.getLicenseNumber() != null ? request.getLicenseNumber().trim() : null;
        if (driverRepository.existsByLicenseNumber(normalizedLicense)) {
            throw new DuplicateResourceException("Driver license number is already registered: " + normalizedLicense);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Driver driver = new Driver(user, normalizedLicense, VerificationStatus.PENDING);
        Driver saved = driverRepository.save(driver);

        return DriverResponse.from(saved);
    }

    /**
     * Retrieves the driver profile associated with the specified user.
     *
     * @param userId the ID of the authenticated user
     * @return safe DriverResponse
     * @throws ResourceNotFoundException if no driver profile exists for the user
     */
    @Transactional(readOnly = true)
    public DriverResponse getCurrentDriverProfile(UUID userId) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for user: " + userId));
        Vehicle vehicle = driver.getVehicle();
        if (vehicle == null) {
            vehicle = vehicleRepository.findByDriverId(driver.getId()).orElse(null);
        }
        return DriverResponse.from(driver, vehicle);
    }

    /**
     * Updates the availability status of the driver associated with the specified user.
     * Only drivers whose verification status is APPROVED may become available.
     * Drivers may mark themselves unavailable regardless of verification status.
     *
     * @param userId the ID of the authenticated user
     * @param request the availability update request
     * @return safe DriverResponse
     * @throws ResourceNotFoundException if no driver profile exists for the user
     * @throws BadRequestException if an unapproved driver attempts to become available
     */
    @Transactional
    public DriverResponse updateAvailability(UUID userId, DriverAvailabilityRequest request) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for user: " + userId));

        if (request == null || request.getAvailable() == null) {
            throw new BadRequestException("Availability status is required");
        }

        if (Boolean.TRUE.equals(request.getAvailable()) && driver.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new BadRequestException("Driver must have APPROVED verification status to become available");
        }

        driver.setAvailable(request.getAvailable());
        Driver saved = driverRepository.save(driver);

        return DriverResponse.from(saved);
    }

    /**
     * Updates the verification status of a driver by their driver ID (admin operation).
     * Acceptable statuses are only APPROVED or REJECTED.
     * When rejected, the driver availability is forced to false.
     * When approved, availability remains false unless explicitly enabled by the driver.
     *
     * @param driverId the UUID of the driver to verify
     * @param request the verification request containing the new status
     * @return safe DriverResponse
     * @throws ResourceNotFoundException if driver with driverId does not exist
     * @throws BadRequestException if the status is missing, invalid, or unsupported
     */
    @Transactional
    public DriverResponse verifyDriver(UUID driverId, DriverVerificationRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new BadRequestException("Verification status is required");
        }

        VerificationStatus newStatus = request.getStatus();
        if (newStatus != VerificationStatus.APPROVED && newStatus != VerificationStatus.REJECTED) {
            throw new BadRequestException("Verification status must be either APPROVED or REJECTED");
        }

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));

        driver.setVerificationStatus(newStatus);
        if (newStatus == VerificationStatus.REJECTED) {
            driver.setAvailable(false);
        }

        Driver saved = driverRepository.save(driver);
        return DriverResponse.from(saved);
    }

    /**
     * Registers vehicle details for the authenticated driver.
     * Enforces that each driver can only have one registered vehicle and
     * registration numbers are unique across all vehicles.
     *
     * @param userId the ID of the authenticated user
     * @param request the vehicle registration request
     * @return safe VehicleResponse
     * @throws ResourceNotFoundException if driver profile does not exist
     * @throws DuplicateResourceException if driver already has a vehicle or registration number is taken
     */
    @Transactional
    public VehicleResponse registerVehicle(UUID userId, VehicleRegistrationRequest request) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for user: " + userId));

        if (vehicleRepository.existsByDriverId(driver.getId())) {
            throw new DuplicateResourceException("Vehicle already registered for this driver");
        }

        String normalizedReg = request.getRegistrationNumber() != null ? request.getRegistrationNumber().trim().toUpperCase() : null;
        if (vehicleRepository.existsByRegistrationNumber(normalizedReg)) {
            throw new DuplicateResourceException("Vehicle registration number is already registered: " + normalizedReg);
        }

        Vehicle vehicle = new Vehicle(
                driver,
                request.getVehicleType(),
                request.getMake(),
                request.getModel(),
                normalizedReg,
                request.getColor()
        );

        Vehicle saved = vehicleRepository.save(vehicle);
        driver.setVehicle(saved);
        return VehicleResponse.from(saved);
    }

    /**
     * Retrieves vehicle details for the authenticated driver.
     *
     * @param userId the ID of the authenticated user
     * @return safe VehicleResponse
     * @throws ResourceNotFoundException if driver profile or vehicle is not found
     */
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleByUserId(UUID userId) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for user: " + userId));

        Vehicle vehicle = vehicleRepository.findByDriverId(driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found for driver: " + driver.getId()));

        return VehicleResponse.from(vehicle);
    }

    /**
     * Updates or registers vehicle details for the authenticated driver.
     * Enforces uniqueness for the registration number across other drivers' vehicles.
     *
     * @param userId the ID of the authenticated user
     * @param request the vehicle update request
     * @return safe VehicleResponse
     * @throws ResourceNotFoundException if driver profile is not found
     * @throws DuplicateResourceException if registration number is taken by another vehicle
     */
    @Transactional
    public VehicleResponse updateVehicle(UUID userId, VehicleRegistrationRequest request) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for user: " + userId));

        String normalizedReg = request.getRegistrationNumber() != null ? request.getRegistrationNumber().trim().toUpperCase() : null;

        java.util.Optional<Vehicle> existingByReg = vehicleRepository.findByRegistrationNumber(normalizedReg);
        if (existingByReg.isPresent() && !existingByReg.get().getDriver().getId().equals(driver.getId())) {
            throw new DuplicateResourceException("Vehicle registration number is already registered: " + normalizedReg);
        }

        Vehicle vehicle = vehicleRepository.findByDriverId(driver.getId())
                .orElseGet(() -> new Vehicle(driver, request.getVehicleType(), request.getMake(), request.getModel(), normalizedReg, request.getColor()));

        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setRegistrationNumber(normalizedReg);
        vehicle.setColor(request.getColor());

        Vehicle saved = vehicleRepository.save(vehicle);
        driver.setVehicle(saved);
        return VehicleResponse.from(saved);
    }

    /**
     * Updates the geographical location of the authenticated driver.
     * Only drivers with DRIVER role, an existing driver profile, and APPROVED verification status may report location.
     *
     * @param principal the authenticated driver principal
     * @param request the location update request
     * @return safe DriverLocationResponse
     * @throws AccessDeniedException if caller does not have DRIVER role
     * @throws ResourceNotFoundException if driver profile does not exist
     * @throws BadRequestException if driver is not APPROVED, or coordinates/metrics are invalid
     */
    @Transactional
    public DriverLocationResponse updateLocation(UserPrincipal principal, DriverLocationUpdateRequest request) {
        if (principal == null || principal.getRole() != Role.DRIVER) {
            throw new AccessDeniedException("Access is denied: caller must have DRIVER role");
        }

        if (request == null) {
            throw new BadRequestException("Location update request cannot be null");
        }

        Driver driver = driverRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for user: " + principal.getId()));

        if (driver.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new BadRequestException("Driver must have APPROVED verification status to update location");
        }

        validateCoordinates(request.getLatitude(), request.getLongitude());
        if (request.getHeading() != null && (request.getHeading() < 0.0 || request.getHeading() > 360.0)) {
            throw new BadRequestException("Heading must be between 0.0 and 360.0 degrees");
        }
        if (request.getSpeed() != null && request.getSpeed() < 0.0) {
            throw new BadRequestException("Speed cannot be negative");
        }

        DriverLocation location = new DriverLocation(
                driver.getId(),
                request.getLatitude(),
                request.getLongitude(),
                request.getHeading(),
                request.getSpeed(),
                Instant.now()
        );

        driverLocationStore.saveLocation(location);

        if (eventPublisher != null && rideRepository != null) {
            List<Ride> activeRides = rideRepository.findByDriverIdAndStatusIn(
                    driver.getId(),
                    List.of(RideStatus.ACCEPTED, RideStatus.ARRIVED, RideStatus.IN_PROGRESS)
            );
            UUID activeRideId = activeRides.isEmpty() ? null : activeRides.get(0).getId();
            eventPublisher.publishEvent(new DriverLocationDomainEvent(
                    driver.getId(),
                    activeRideId,
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getHeading(),
                    location.getSpeed(),
                    location.getUpdatedAt()
            ));
        }

        return DriverLocationResponse.from(location);
    }

    /**
     * Retrieves the latest geographical location of the authenticated driver.
     *
     * @param principal the authenticated driver principal
     * @return safe DriverLocationResponse
     * @throws AccessDeniedException if caller does not have DRIVER role
     * @throws ResourceNotFoundException if driver profile or location is not found
     */
    @Transactional(readOnly = true)
    public DriverLocationResponse getCurrentLocation(UserPrincipal principal) {
        if (principal == null || principal.getRole() != Role.DRIVER) {
            throw new AccessDeniedException("Access is denied: caller must have DRIVER role");
        }

        Driver driver = driverRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for user: " + principal.getId()));

        return driverLocationStore.getLocation(driver.getId())
                .map(DriverLocationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found for driver: " + driver.getId()));
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || !Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            throw new BadRequestException("Latitude must be between -90.0 and 90.0");
        }
        if (longitude == null || !Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            throw new BadRequestException("Longitude must be between -180.0 and 180.0");
        }
    }
}

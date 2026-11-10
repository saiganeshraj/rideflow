package com.rideflow.driver;

import com.rideflow.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controller exposing endpoints for driver profile operations.
 */
@RestController
@RequestMapping("/api/v1/drivers")
@Tag(name = "Drivers", description = "Driver profile and registration operations")
public class DriverController {

    private final DriverService driverService;
    private final DriverDiscoveryService driverDiscoveryService;

    public DriverController(DriverService driverService, DriverDiscoveryService driverDiscoveryService) {
        this.driverService = driverService;
        this.driverDiscoveryService = driverDiscoveryService;
    }

    /**
     * Registers a new driver profile for the authenticated user.
     * User identity is strictly derived from the authenticated security context.
     *
     * @param principal the authenticated user principal from the security context
     * @param request the registration request containing only the driving license number
     * @return safe DriverResponse with status PENDING
     */
    @PostMapping({"", "/register"})
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register driver profile",
            description = "Registers a new driver profile for the authenticated user with initial status PENDING",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public DriverResponse registerDriver(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DriverRegistrationRequest request) {
        return driverService.registerDriver(principal.getId(), request);
    }

    /**
     * Retrieves the driver profile of the currently authenticated user.
     * Identity is strictly derived from the authenticated security principal.
     *
     * @param principal the authenticated user principal from the security context
     * @return safe DriverResponse
     */
    @GetMapping("/me")
    @Operation(
            summary = "Get current driver profile",
            description = "Returns the driver profile for the authenticated user identified by the Bearer token",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public DriverResponse getMyDriverProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return driverService.getCurrentDriverProfile(principal.getId());
    }

    /**
     * Updates the availability status of the currently authenticated driver.
     * Identity is strictly derived from the authenticated security principal.
     * Only approved drivers may become available.
     *
     * @param principal the authenticated user principal from the security context
     * @param request the availability update request
     * @return updated safe DriverResponse
     */
    @PatchMapping("/me/availability")
    @Operation(
            summary = "Update driver availability",
            description = "Updates the availability status for the authenticated driver. Only approved drivers may become available.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public DriverResponse updateAvailability(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DriverAvailabilityRequest request) {
        return driverService.updateAvailability(principal.getId(), request);
    }

    /**
     * Registers vehicle details for the currently authenticated driver.
     * Identity is strictly derived from the authenticated security principal.
     *
     * @param principal the authenticated user principal from the security context
     * @param request the vehicle registration request
     * @return safe VehicleResponse
     */
    @PostMapping("/me/vehicle")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register vehicle details",
            description = "Registers vehicle details for the authenticated driver.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public VehicleResponse registerVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VehicleRegistrationRequest request) {
        return driverService.registerVehicle(principal.getId(), request);
    }

    /**
     * Retrieves vehicle details for the currently authenticated driver.
     * Identity is strictly derived from the authenticated security principal.
     *
     * @param principal the authenticated user principal from the security context
     * @return safe VehicleResponse
     */
    @GetMapping("/me/vehicle")
    @Operation(
            summary = "Get current driver vehicle details",
            description = "Returns the registered vehicle details for the authenticated driver.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public VehicleResponse getMyVehicle(@AuthenticationPrincipal UserPrincipal principal) {
        return driverService.getVehicleByUserId(principal.getId());
    }

    /**
     * Updates vehicle details for the currently authenticated driver.
     * Identity is strictly derived from the authenticated security principal.
     *
     * @param principal the authenticated user principal from the security context
     * @param request the vehicle update request
     * @return safe VehicleResponse
     */
    @PutMapping("/me/vehicle")
    @Operation(
            summary = "Update driver vehicle details",
            description = "Updates the registered vehicle details for the authenticated driver.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public VehicleResponse updateVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VehicleRegistrationRequest request) {
        return driverService.updateVehicle(principal.getId(), request);
    }

    /**
     * Updates the geographical location of the currently authenticated driver.
     * Identity is strictly derived from the authenticated security principal.
     * Only approved drivers may update location.
     *
     * @param principal the authenticated user principal from the security context
     * @param request the location update request
     * @return safe DriverLocationResponse
     */
    @PutMapping("/me/location")
    @Operation(
            summary = "Update driver location",
            description = "Updates the current geographical coordinates, heading, and speed of the authenticated driver.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public DriverLocationResponse updateLocation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DriverLocationUpdateRequest request) {
        return driverService.updateLocation(principal, request);
    }

    /**
     * Retrieves the latest reported geographical location of the authenticated driver.
     *
     * @param principal the authenticated user principal from the security context
     * @return safe DriverLocationResponse
     */
    @GetMapping("/me/location")
    @Operation(
            summary = "Get current driver location",
            description = "Retrieves the latest reported geographical coordinates of the authenticated driver.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public DriverLocationResponse getCurrentLocation(@AuthenticationPrincipal UserPrincipal principal) {
        return driverService.getCurrentLocation(principal);
    }

    /**
     * Discovers eligible nearby drivers around specified pickup coordinates within an optional radius.
     *
     * @param latitude pickup latitude (required, between -90.0 and 90.0)
     * @param longitude pickup longitude (required, between -180.0 and 180.0)
     * @param radiusKm optional radius in kilometers (default 5.0 km, max 20.0 km)
     * @param vehicleType optional vehicle type filter
     * @return list of nearby drivers sorted by distance ascending
     */
    @GetMapping("/nearby")
    @Operation(
            summary = "Find nearby drivers",
            description = "Discovers approved, available drivers with registered vehicles and fresh locations within specified radius",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<NearbyDriverResponse> getNearbyDrivers(
            @RequestParam(name = "latitude") Double latitude,
            @RequestParam(name = "longitude") Double longitude,
            @RequestParam(name = "radiusKm", required = false) Double radiusKm,
            @RequestParam(name = "vehicleType", required = false) String vehicleType) {
        return driverDiscoveryService.findNearbyDrivers(latitude, longitude, radiusKm, vehicleType);
    }
}

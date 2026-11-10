package com.rideflow.admin;

import com.rideflow.driver.DriverResponse;
import com.rideflow.driver.DriverService;
import com.rideflow.driver.DriverVerificationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller exposing administrative operations for driver profiles.
 * Restricted to users with the ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/admin/drivers")
@Tag(name = "Admin - Drivers", description = "Administrative driver verification and management operations")
public class AdminDriverController {

    private final DriverService driverService;

    public AdminDriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    /**
     * Updates the verification status of a driver profile (APPROVED or REJECTED).
     * Accessible only by administrators.
     *
     * @param driverId UUID of the driver to verify
     * @param request verification request containing the new status
     * @return safe DriverResponse
     */
    @PatchMapping("/{driverId}/verification")
    @Operation(
            summary = "Verify driver profile",
            description = "Updates the verification status of a driver (APPROVED or REJECTED). Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public DriverResponse updateDriverVerification(
            @PathVariable UUID driverId,
            @Valid @RequestBody DriverVerificationRequest request) {
        return driverService.verifyDriver(driverId, request);
    }
}

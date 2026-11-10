package com.rideflow.ride;

import com.rideflow.fare.FareEstimateRequest;
import com.rideflow.fare.FareEstimateResponse;
import com.rideflow.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.rideflow.payment.PaymentResponse;
import com.rideflow.payment.PaymentService;
import com.rideflow.payment.PaymentSimulationRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller exposing endpoints for customer ride requests and fare estimation.
 */
@RestController
@RequestMapping("/api/v1/rides")
@Tag(name = "Rides", description = "Ride booking and fare estimation operations")
public class RideController {

    private final RideService rideService;
    private final PaymentService paymentService;

    public RideController(RideService rideService, PaymentService paymentService) {
        this.rideService = rideService;
        this.paymentService = paymentService;
    }

    /**
     * Calculates distance and estimated fare between pickup and dropoff coordinates.
     * Requires an authenticated caller.
     *
     * @param principal the authenticated user principal from the security context
     * @param request   the fare estimate request containing coordinates
     * @return fare estimate response with distance, fare, and currency
     */
    @PostMapping("/fare-estimate")
    @Operation(
            summary = "Estimate ride fare",
            description = "Calculates estimated distance and fare between pickup and dropoff coordinates",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public FareEstimateResponse estimateFare(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FareEstimateRequest request) {
        return rideService.estimateFare(request);
    }

    /**
     * Creates a new ride request for the currently authenticated customer.
     * Customer identity is derived exclusively from the security context.
     * Initial status is REQUESTED with an unassigned driver.
     *
     * @param principal the authenticated user principal from the security context
     * @param request   the ride creation request
     * @return safe RideResponse with status REQUESTED
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Request a ride",
            description = "Creates a new ride request for the authenticated customer with status REQUESTED",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public RideResponse requestRide(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RideCreateRequest request) {
        return rideService.requestRide(principal.getId(), request);
    }

    /**
     * Accepts a ride in REQUESTED status by an approved and available driver.
     *
     * @param rideId    the ID of the ride
     * @param principal the authenticated driver principal
     * @return safe RideResponse with status ACCEPTED
     */
    @RequestMapping(value = "/{rideId}/accept", method = {RequestMethod.PATCH, RequestMethod.POST})
    @Operation(
            summary = "Accept ride request",
            description = "Allows an approved, available driver to accept a ride in REQUESTED status",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public RideResponse acceptRide(
            @PathVariable UUID rideId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return rideService.acceptRide(rideId, principal);
    }

    /**
     * Marks driver arrival at the pickup location (transition to ARRIVED).
     *
     * @param rideId    the ID of the ride
     * @param principal the authenticated driver principal
     * @return safe RideResponse with status ARRIVED
     */
    @RequestMapping(value = {"/{rideId}/arrive", "/{rideId}/arrived"}, method = {RequestMethod.PATCH, RequestMethod.POST})
    @Operation(
            summary = "Mark driver arrival",
            description = "Allows the assigned driver to mark arrival at the pickup location (transition to ARRIVED)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public RideResponse markArrived(
            @PathVariable UUID rideId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return rideService.markArrived(rideId, principal);
    }

    /**
     * Starts the trip, changing ride status to IN_PROGRESS.
     *
     * @param rideId    the ID of the ride
     * @param principal the authenticated driver principal
     * @return safe RideResponse with status IN_PROGRESS
     */
    @RequestMapping(value = {"/{rideId}/start", "/{rideId}/in-progress"}, method = {RequestMethod.PATCH, RequestMethod.POST})
    @Operation(
            summary = "Start ride trip",
            description = "Allows the assigned driver to start the trip (transition to IN_PROGRESS)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public RideResponse startRide(
            @PathVariable UUID rideId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return rideService.startRide(rideId, principal);
    }

    /**
     * Completes the trip, changing ride status to COMPLETED.
     *
     * @param rideId    the ID of the ride
     * @param principal the authenticated driver principal
     * @return safe RideResponse with status COMPLETED
     */
    @RequestMapping(value = {"/{rideId}/complete", "/{rideId}/completed"}, method = {RequestMethod.PATCH, RequestMethod.POST})
    @Operation(
            summary = "Complete ride trip",
            description = "Allows the assigned driver to complete the trip (transition to COMPLETED)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public RideResponse completeRide(
            @PathVariable UUID rideId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return rideService.completeRide(rideId, principal);
    }

    /**
     * Updates ride status through lifecycle state transitions.
     *
     * @param rideId    the ID of the ride
     * @param principal the authenticated driver principal
     * @param request   the status update request
     * @return safe RideResponse
     */
    @RequestMapping(value = "/{rideId}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @Operation(
            summary = "Update ride status",
            description = "Updates the ride status through lifecycle state transitions",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public RideResponse updateRideStatus(
            @PathVariable UUID rideId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RideStatusUpdateRequest request) {
        return rideService.updateRideStatus(rideId, principal, request);
    }

    /**
     * Cancels a ride request by the owning customer.
     * Allowed only while in REQUESTED or ACCEPTED status.
     *
     * @param rideId    the ID of the ride to cancel
     * @param principal the authenticated customer principal
     * @return safe RideResponse with status CANCELLED
     */
    @RequestMapping(value = "/{rideId}/cancel", method = {RequestMethod.PATCH, RequestMethod.POST})
    @Operation(
            summary = "Cancel ride",
            description = "Allows the customer who requested the ride to cancel it while in REQUESTED or ACCEPTED status",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public RideResponse cancelRide(
            @PathVariable UUID rideId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return rideService.cancelRide(rideId, principal);
    }

    /**
     * Retrieves paginated ride history for the authenticated caller.
     * Customers see rides they requested; drivers see rides assigned to their driver profile.
     *
     * @param principal the authenticated caller principal
     * @param pageable  pagination parameters
     * @return paginated ride history
     */
    @GetMapping("/my-rides")
    @Operation(
            summary = "Get my ride history",
            description = "Returns paginated ride history for the authenticated customer or driver",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public Page<RideResponse> getMyRides(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return rideService.getMyRides(principal, pageable);
    }

    /**
     * Simulates payment for the specified ride.
     *
     * @param rideId    the ID of the completed ride
     * @param principal the authenticated customer principal
     * @param request   optional simulation parameters
     * @return safe PaymentResponse
     */
    @PostMapping({"/{rideId}/payment", "/{rideId}/payments"})
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Simulate payment for ride",
            description = "Simulates payment processing for the completed ride.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public PaymentResponse simulateRidePayment(
            @PathVariable UUID rideId,
            @AuthenticationPrincipal UserPrincipal principal,
            @jakarta.validation.Valid @RequestBody(required = false) PaymentSimulationRequest request) {
        return paymentService.simulatePayment(rideId, principal, request);
    }

    /**
     * Retrieves simulated payment status for the specified ride.
     *
     * @param rideId    the ID of the ride
     * @param principal the authenticated customer principal
     * @return safe PaymentResponse
     */
    @GetMapping({"/{rideId}/payment", "/{rideId}/payments"})
    @Operation(
            summary = "Get ride payment status",
            description = "Retrieves the payment status for the completed ride.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public PaymentResponse getRidePayment(
            @PathVariable UUID rideId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.getPaymentByRideId(rideId, principal);
    }
}


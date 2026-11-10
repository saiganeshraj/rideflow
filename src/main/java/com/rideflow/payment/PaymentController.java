package com.rideflow.payment;

import com.rideflow.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller exposing endpoints for simulated ride payment operations and status inquiries.
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Simulated ride payment operations")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Simulates payment processing for a completed ride.
     * Ride ID must be supplied in the request body.
     *
     * @param principal the authenticated customer principal
     * @param request   simulation request details containing rideId
     * @return safe PaymentResponse
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Simulate payment for a ride",
            description = "Simulates payment processing for an eligible COMPLETED ride. Stores no real financial details.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public PaymentResponse simulatePayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @jakarta.validation.Valid @RequestBody(required = false) PaymentSimulationRequest request) {
        UUID rideId = request != null ? request.getRideId() : null;
        return paymentService.simulatePayment(rideId, principal, request);
    }

    /**
     * Simulates payment processing for a completed ride identified by path variable.
     *
     * @param rideId    the ID of the completed ride
     * @param principal the authenticated customer principal
     * @param request   optional simulation request details
     * @return safe PaymentResponse
     */
    @PostMapping("/ride/{rideId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Simulate payment by ride ID",
            description = "Simulates payment processing for the specified COMPLETED ride.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public PaymentResponse simulatePaymentForRide(
            @PathVariable UUID rideId,
            @AuthenticationPrincipal UserPrincipal principal,
            @jakarta.validation.Valid @RequestBody(required = false) PaymentSimulationRequest request) {
        return paymentService.simulatePayment(rideId, principal, request);
    }

    /**
     * Retrieves the latest simulated payment status for a specific ride.
     *
     * @param rideId    the ID of the ride
     * @param principal the authenticated customer principal
     * @return safe PaymentResponse
     */
    @GetMapping("/ride/{rideId}")
    @Operation(
            summary = "Get payment status for a ride",
            description = "Retrieves the payment status for the specified ride. Only owning customer may view.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public PaymentResponse getPaymentByRideId(
            @PathVariable UUID rideId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.getPaymentByRideId(rideId, principal);
    }

    /**
     * Retrieves simulated payment details by payment ID.
     *
     * @param paymentId the ID of the payment
     * @param principal the authenticated customer principal
     * @return safe PaymentResponse
     */
    @GetMapping("/{paymentId}")
    @Operation(
            summary = "Get payment details by ID",
            description = "Retrieves payment details by payment ID. Only owning customer may view.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public PaymentResponse getPaymentById(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.getPaymentById(paymentId, principal);
    }
}

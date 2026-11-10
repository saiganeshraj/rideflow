package com.rideflow.payment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Request payload for simulating payment processing for a completed ride.
 * Clearly labeled as simulation: accepts no card numbers, CVVs, or real financial credentials.
 */
@Schema(description = "Request payload for simulating ride payment")
public class PaymentSimulationRequest {

    @Schema(description = "ID of the completed ride to pay for (optional if provided in URL path)", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID rideId;

    @Schema(description = "Simulate payment failure if true (default: false)", example = "false")
    private Boolean simulateFailure = false;

    @jakarta.validation.constraints.Size(max = 50, message = "Payment method must not exceed 50 characters")
    @Schema(description = "Simulated payment method label (e.g., SIMULATED_UPI, SIMULATED_CARD)", example = "SIMULATED")
    private String paymentMethod = "SIMULATED";

    @Schema(description = "Explicit target payment status (SUCCESS or FAILED)", example = "SUCCESS")
    private PaymentStatus status;

    public PaymentSimulationRequest() {
    }

    public PaymentSimulationRequest(UUID rideId) {
        this.rideId = rideId;
        this.simulateFailure = false;
        this.paymentMethod = "SIMULATED";
    }

    public PaymentSimulationRequest(UUID rideId, Boolean simulateFailure) {
        this.rideId = rideId;
        this.simulateFailure = simulateFailure;
        this.paymentMethod = "SIMULATED";
    }

    public PaymentSimulationRequest(UUID rideId, Boolean simulateFailure, String paymentMethod) {
        this.rideId = rideId;
        this.simulateFailure = simulateFailure;
        this.paymentMethod = paymentMethod;
    }

    public UUID getRideId() {
        return rideId;
    }

    public void setRideId(UUID rideId) {
        this.rideId = rideId;
    }

    public Boolean getSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(Boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
}

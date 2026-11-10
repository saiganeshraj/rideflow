package com.rideflow.payment;

import com.rideflow.common.exception.BadRequestException;
import com.rideflow.common.exception.DuplicateResourceException;
import com.rideflow.common.exception.ResourceNotFoundException;
import com.rideflow.ride.Ride;
import com.rideflow.ride.RideRepository;
import com.rideflow.ride.RideStatus;
import com.rideflow.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service managing simulated ride payment transactions.
 * Clearly labeled as simulation: does not integrate real payment gateways or collect sensitive financial details.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RideRepository rideRepository;

    public PaymentService(PaymentRepository paymentRepository, RideRepository rideRepository) {
        this.paymentRepository = paymentRepository;
        this.rideRepository = rideRepository;
    }

    /**
     * Simulates payment processing for a completed ride.
     * Only the customer who owns the ride may initiate payment.
     * Rides must be in COMPLETED status to be eligible.
     * Prevents duplicate successful payments using pessimistic locking and uniqueness checks.
     *
     * @param rideId    the ID of the ride to pay for
     * @param principal the authenticated customer principal
     * @param request   simulation request details
     * @return safe PaymentResponse
     */
    @Transactional
    public PaymentResponse simulatePayment(UUID rideId, UserPrincipal principal, PaymentSimulationRequest request) {
        if (principal == null) {
            throw new AccessDeniedException("Access is denied: insufficient permissions");
        }

        if (rideId != null && request != null && request.getRideId() != null && !rideId.equals(request.getRideId())) {
            throw new BadRequestException("Ride ID in path does not match ride ID in request body");
        }

        UUID resolvedRideId = (rideId != null) ? rideId : (request != null ? request.getRideId() : null);
        if (resolvedRideId == null) {
            throw new BadRequestException("Ride ID is required for payment simulation");
        }
        final UUID targetRideId = resolvedRideId;

        if (request != null && request.getStatus() == PaymentStatus.PENDING) {
            throw new BadRequestException("Simulated payment status cannot be PENDING; must be SUCCESS or FAILED");
        }

        if (request != null && request.getPaymentMethod() != null && request.getPaymentMethod().trim().length() > 50) {
            throw new BadRequestException("Payment method must not exceed 50 characters");
        }

        Ride ride = rideRepository.findByIdForUpdate(targetRideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + targetRideId));

        if (!ride.getCustomer().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Only the customer who requested the ride may process payment");
        }

        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new BadRequestException("Payment can only be simulated for rides in COMPLETED status. Current status: " + ride.getStatus());
        }

        if (paymentRepository.existsByRideIdAndStatus(targetRideId, PaymentStatus.SUCCESS)) {
            throw new DuplicateResourceException("Payment has already been successfully processed for ride: " + targetRideId);
        }

        PaymentStatus status = (request != null && (Boolean.TRUE.equals(request.getSimulateFailure()) || request.getStatus() == PaymentStatus.FAILED))
                ? PaymentStatus.FAILED
                : PaymentStatus.SUCCESS;

        String paymentMethod = (request != null && request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank())
                ? request.getPaymentMethod().trim()
                : "SIMULATED";

        String transactionId = "SIM-" + (status == PaymentStatus.SUCCESS ? "TXN-" : "FAIL-") + UUID.randomUUID();

        Payment payment = new Payment(
                ride,
                ride.getCustomer(),
                ride.getEstimatedFare(),
                ride.getCurrency(),
                status,
                paymentMethod,
                transactionId
        );

        Payment saved = paymentRepository.save(payment);
        return PaymentResponse.from(saved);
    }

    /**
     * Retrieves the latest payment status for a specific ride.
     * Only the customer who owns the ride may view its payment details.
     *
     * @param rideId    the ID of the ride
     * @param principal the authenticated customer principal
     * @return safe PaymentResponse
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByRideId(UUID rideId, UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Access is denied: insufficient permissions");
        }

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));

        if (!ride.getCustomer().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Only the customer who requested the ride may view payment details");
        }

        Payment payment = paymentRepository.findTopByRideIdOrderByCreatedAtDesc(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for ride: " + rideId));

        return PaymentResponse.from(payment);
    }

    /**
     * Retrieves payment details by payment ID.
     * Only the customer who owns the payment may view it.
     *
     * @param paymentId the ID of the payment
     * @param principal the authenticated customer principal
     * @return safe PaymentResponse
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId, UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Access is denied: insufficient permissions");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (!payment.getCustomer().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Only the customer who requested the ride may view payment details");
        }

        return PaymentResponse.from(payment);
    }
}

package com.rideflow.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link Payment} entities.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByRideId(UUID rideId);

    Optional<Payment> findTopByRideIdOrderByCreatedAtDesc(UUID rideId);

    Optional<Payment> findByRideIdAndStatus(UUID rideId, PaymentStatus status);

    boolean existsByRideIdAndStatus(UUID rideId, PaymentStatus status);

    List<Payment> findByCustomerId(UUID customerId);
}

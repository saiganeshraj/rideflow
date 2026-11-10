package com.rideflow.ride;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link Ride} entities.
 */
@Repository
public interface RideRepository extends JpaRepository<Ride, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Ride r WHERE r.id = :id")
    Optional<Ride> findByIdForUpdate(@Param("id") UUID id);

    Page<Ride> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Ride> findByDriverId(UUID driverId, Pageable pageable);

    List<Ride> findByCustomerId(UUID customerId);

    List<Ride> findByDriverId(UUID driverId);

    List<Ride> findByStatus(RideStatus status);

    List<Ride> findByCustomerIdAndStatus(UUID customerId, RideStatus status);

    List<Ride> findByDriverIdAndStatus(UUID driverId, RideStatus status);

    boolean existsByDriverIdAndStatusIn(UUID driverId, java.util.Collection<RideStatus> statuses);

    List<Ride> findByDriverIdAndStatusIn(UUID driverId, java.util.Collection<RideStatus> statuses);

    @Query("SELECT r FROM Ride r JOIN FETCH r.customer LEFT JOIN FETCH r.driver d LEFT JOIN FETCH d.user WHERE r.id = :id")
    Optional<Ride> findByIdWithCustomerAndDriver(@Param("id") UUID id);
}

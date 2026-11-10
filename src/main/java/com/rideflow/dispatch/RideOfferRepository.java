package com.rideflow.dispatch;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link RideOffer} entities.
 */
@Repository
public interface RideOfferRepository extends JpaRepository<RideOffer, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM RideOffer o WHERE o.id = :id")
    Optional<RideOffer> findByIdForUpdate(@Param("id") UUID id);

    List<RideOffer> findByDriverIdAndStatus(UUID driverId, RideOfferStatus status);

    List<RideOffer> findByRideId(UUID rideId);

    List<RideOffer> findByRideIdAndStatus(UUID rideId, RideOfferStatus status);

    boolean existsByRideIdAndDriverId(UUID rideId, UUID driverId);

    @Modifying
    @Query("UPDATE RideOffer o SET o.status = :newStatus, o.updatedAt = :now WHERE o.ride.id = :rideId AND o.id != :excludedOfferId AND o.status = 'PENDING'")
    int cancelOtherPendingOffersForRide(@Param("rideId") UUID rideId,
                                        @Param("excludedOfferId") UUID excludedOfferId,
                                        @Param("newStatus") RideOfferStatus newStatus,
                                        @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RideOffer o SET o.status = 'EXPIRED', o.updatedAt = :now WHERE o.status = 'PENDING' AND o.expiresAt < :now")
    int expirePendingOffers(@Param("now") Instant now);
}

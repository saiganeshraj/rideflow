package com.rideflow.dispatch;

import com.rideflow.common.exception.BadRequestException;
import com.rideflow.common.exception.ResourceNotFoundException;
import com.rideflow.common.exception.UnauthorizedException;
import com.rideflow.driver.Driver;
import com.rideflow.driver.DriverDiscoveryService;
import com.rideflow.driver.DriverRepository;
import com.rideflow.driver.NearbyDriverResponse;
import com.rideflow.ride.Ride;
import com.rideflow.ride.RideRepository;
import com.rideflow.ride.RideStatus;
import com.rideflow.security.UserPrincipal;
import com.rideflow.user.Role;
import com.rideflow.websocket.event.RideLifecycleDomainEvent;
import com.rideflow.websocket.event.RideOfferDomainEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service orchestrating the ride dispatch workflow, managing ride offer creation,
 * expiration, driver acceptance, rejection, dispatch recovery, and concurrency guarantees.
 */
@Service
public class DispatchService {

    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final RideOfferRepository rideOfferRepository;
    private final DriverDiscoveryService driverDiscoveryService;
    private final ApplicationEventPublisher eventPublisher;

    private long offerExpirySeconds;
    private int batchSize;
    private int maxRetries;
    private boolean autoReDispatch;

    public DispatchService(RideRepository rideRepository,
                           DriverRepository driverRepository,
                           RideOfferRepository rideOfferRepository,
                           DriverDiscoveryService driverDiscoveryService,
                           ApplicationEventPublisher eventPublisher,
                           @Value("${rideflow.dispatch.offer-expiry-seconds:60}") long offerExpirySeconds,
                           @Value("${rideflow.dispatch.batch-size:5}") int batchSize,
                           @Value("${rideflow.dispatch.max-retries:3}") int maxRetries,
                           @Value("${rideflow.dispatch.auto-re-dispatch:true}") boolean autoReDispatch) {
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
        this.rideOfferRepository = rideOfferRepository;
        this.driverDiscoveryService = driverDiscoveryService;
        this.eventPublisher = eventPublisher;
        this.offerExpirySeconds = offerExpirySeconds;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
        this.autoReDispatch = autoReDispatch;
    }

    public void setOfferExpirySeconds(long offerExpirySeconds) {
        this.offerExpirySeconds = offerExpirySeconds;
    }

    public long getOfferExpirySeconds() {
        return offerExpirySeconds;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setAutoReDispatch(boolean autoReDispatch) {
        this.autoReDispatch = autoReDispatch;
    }

    public boolean isAutoReDispatch() {
        return autoReDispatch;
    }

    /**
     * Dispatches a ride in REQUESTED status by generating offers for eligible nearby drivers.
     * Customers can only dispatch their own rides.
     *
     * @param rideId    the ID of the ride to dispatch
     * @param principal authenticated user principal
     * @param request   optional dispatch parameters (radiusKm, vehicleType, maxOffers)
     * @return dispatch response detailing offer generation results
     */
    @Transactional
    public RideDispatchResponse dispatchRide(UUID rideId, UserPrincipal principal, RideDispatchRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required to dispatch ride");
        }

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));

        if (!ride.getCustomer().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Customers can only dispatch their own rides");
        }

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new BadRequestException("Ride cannot be dispatched because it is in status: " + ride.getStatus());
        }

        double radiusKm = (request != null && request.getRadiusKm() != null)
                ? request.getRadiusKm()
                : DriverDiscoveryService.DEFAULT_RADIUS_KM;

        String vehicleType = (request != null) ? request.getVehicleType() : null;
        int maxOffers = (request != null && request.getMaxOffers() != null) ? request.getMaxOffers() : 5;

        List<NearbyDriverResponse> nearbyDrivers = driverDiscoveryService.findNearbyDrivers(
                ride.getPickupLatitude(),
                ride.getPickupLongitude(),
                radiusKm,
                vehicleType
        );

        if (nearbyDrivers.isEmpty()) {
            return new RideDispatchResponse(
                    rideId,
                    "NO_DRIVERS_AVAILABLE",
                    0,
                    "No eligible drivers found within search radius",
                    Instant.now()
            );
        }

        List<RideOffer> createdOffers = new ArrayList<>();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofSeconds(offerExpirySeconds));

        int count = 0;
        for (NearbyDriverResponse candidate : nearbyDrivers) {
            if (count >= maxOffers) {
                break;
            }

            Driver driver = driverRepository.findById(candidate.getDriverId()).orElse(null);
            if (driver == null) {
                continue;
            }

            // A driver cannot be offered their own ride request
            if (driver.getUser().getId().equals(ride.getCustomer().getId())) {
                continue;
            }

            // Avoid duplicate active offers for the same ride and driver
            if (rideOfferRepository.existsByRideIdAndDriverId(ride.getId(), driver.getId())) {
                continue;
            }

            RideOffer offer = new RideOffer(ride, driver, expiresAt);
            RideOffer savedOffer = rideOfferRepository.save(offer);
            createdOffers.add(savedOffer);
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new RideOfferDomainEvent(savedOffer));
            }
            count++;
        }

        if (createdOffers.isEmpty()) {
            return new RideDispatchResponse(
                    rideId,
                    "NO_DRIVERS_AVAILABLE",
                    0,
                    "No eligible drivers found within search radius",
                    Instant.now()
            );
        }

        return new RideDispatchResponse(
                rideId,
                "DISPATCHED",
                createdOffers.size(),
                "Ride dispatched to " + createdOffers.size() + " nearby driver(s)",
                now
        );
    }

    /**
     * Retrieves all pending ride offers addressed to the authenticated driver.
     * Stale offers exceeding validity are transitioned to EXPIRED.
     *
     * @param principal authenticated driver principal
     * @return list of pending ride offers
     */
    @Transactional
    public List<RideOfferResponse> getDriverPendingOffers(UserPrincipal principal) {
        Driver driver = getAuthenticatedDriver(principal);

        List<RideOffer> offers = rideOfferRepository.findByDriverIdAndStatus(driver.getId(), RideOfferStatus.PENDING);
        Instant now = Instant.now();
        List<RideOfferResponse> activeOffers = new ArrayList<>();

        for (RideOffer offer : offers) {
            if (offer.isExpired(now)) {
                offer.setStatus(RideOfferStatus.EXPIRED);
                rideOfferRepository.save(offer);
                if (eventPublisher != null) {
                    eventPublisher.publishEvent(new RideOfferDomainEvent(offer));
                }
                if (autoReDispatch) {
                    recoverDispatch(offer.getRide().getId());
                }
            } else {
                activeOffers.add(RideOfferResponse.from(offer));
            }
        }

        return activeOffers;
    }

    /**
     * Accepts a pending ride offer. Acquires pessimistic write lock on the Ride
     * to guarantee that at most one driver claims the ride concurrently.
     *
     * @param offerId   the ID of the offer
     * @param principal authenticated driver principal
     * @return accepted ride offer response
     */
    @Transactional
    public RideOfferResponse acceptOffer(UUID offerId, UserPrincipal principal) {
        Driver driver = getAuthenticatedDriver(principal);

        RideOffer unvalidated = rideOfferRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride offer not found with id: " + offerId));

        if (!unvalidated.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("Driver can only accept offers addressed to them");
        }

        // Lock Ride first to prevent concurrent acceptance races
        Ride ride = rideRepository.findByIdForUpdate(unvalidated.getRide().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + unvalidated.getRide().getId()));

        // Lock RideOffer
        RideOffer offer = rideOfferRepository.findByIdForUpdate(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride offer not found with id: " + offerId));

        if (offer.getStatus() != RideOfferStatus.PENDING) {
            throw new BadRequestException("Offer cannot be accepted because it is in status: " + offer.getStatus());
        }

        Instant now = Instant.now();
        if (offer.isExpired(now)) {
            offer.setStatus(RideOfferStatus.EXPIRED);
            rideOfferRepository.save(offer);
            throw new BadRequestException("Ride offer has expired");
        }

        if (ride.getStatus() != RideStatus.REQUESTED || ride.getDriver() != null) {
            offer.setStatus(RideOfferStatus.CANCELLED);
            rideOfferRepository.save(offer);
            throw new BadRequestException("Ride is no longer available (current status: " + ride.getStatus() + ")");
        }

        // Atomic transition
        offer.setStatus(RideOfferStatus.ACCEPTED);
        RideOffer acceptedOffer = rideOfferRepository.save(offer);

        ride.setDriver(driver);
        ride.setStatus(RideStatus.ACCEPTED);
        rideRepository.save(ride);

        // Cancel all remaining pending offers for this ride
        rideOfferRepository.cancelOtherPendingOffersForRide(ride.getId(), offer.getId(), RideOfferStatus.CANCELLED, now);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new RideLifecycleDomainEvent(
                    ride.getId(), RideStatus.ACCEPTED, ride.getCustomer().getId(), driver.getId(),
                    "Ride accepted via offer", now
            ));
            eventPublisher.publishEvent(new RideOfferDomainEvent(acceptedOffer));
        }

        return RideOfferResponse.from(acceptedOffer);
    }

    /**
     * Rejects a pending ride offer addressed to the authenticated driver.
     * When auto re-dispatch is enabled, safely attempts recovery if no active offers remain.
     *
     * @param offerId   the ID of the offer
     * @param principal authenticated driver principal
     * @param request   optional rejection reason
     * @return rejected ride offer response
     */
    @Transactional
    public RideOfferResponse rejectOffer(UUID offerId, UserPrincipal principal, RideOfferRejectRequest request) {
        Driver driver = getAuthenticatedDriver(principal);

        RideOffer offer = rideOfferRepository.findByIdForUpdate(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride offer not found with id: " + offerId));

        if (!offer.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("Driver can only reject offers addressed to them");
        }

        if (offer.getStatus() != RideOfferStatus.PENDING) {
            throw new BadRequestException("Offer cannot be rejected because it is in status: " + offer.getStatus());
        }

        Instant now = Instant.now();
        if (offer.isExpired(now)) {
            offer.setStatus(RideOfferStatus.EXPIRED);
            rideOfferRepository.save(offer);
            throw new BadRequestException("Ride offer has expired");
        }

        offer.setStatus(RideOfferStatus.REJECTED);
        RideOffer rejectedOffer = rideOfferRepository.save(offer);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new RideOfferDomainEvent(rejectedOffer));
        }

        if (autoReDispatch) {
            recoverDispatch(offer.getRide().getId());
        }

        return RideOfferResponse.from(rejectedOffer);
    }

    /**
     * Explicitly expires all pending offers whose validity duration has elapsed.
     * Triggers dispatch recovery for affected rides when auto re-dispatch is active.
     *
     * @return count of expired offers
     */
    @Transactional
    public int expirePendingOffers() {
        Instant now = Instant.now();
        List<RideOffer> expired = rideOfferRepository.findAll().stream()
                .filter(o -> o.getStatus() == RideOfferStatus.PENDING && o.isExpired(now))
                .toList();

        Set<UUID> affectedRideIds = new HashSet<>();
        for (RideOffer o : expired) {
            o.setStatus(RideOfferStatus.EXPIRED);
            rideOfferRepository.save(o);
            affectedRideIds.add(o.getRide().getId());
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new RideOfferDomainEvent(o));
            }
        }

        if (autoReDispatch) {
            for (UUID rideId : affectedRideIds) {
                recoverDispatch(rideId);
            }
        }

        return expired.size();
    }

    /**
     * Attempts dispatch recovery for a ride whose previous offer(s) have expired or been rejected.
     * Safely determines whether more eligible drivers should be offered the ride, prevents duplicate
     * offers to the same driver, respects configured retry limits and batch size, and serializes
     * concurrent recovery attempts via pessimistic row locking on the Ride.
     *
     * @param rideId ID of the ride to recover
     * @return result detailing recovery outcome
     */
    @Transactional
    public DispatchRecoveryResult recoverDispatch(UUID rideId) {
        if (rideId == null) {
            return new DispatchRecoveryResult(null, "INVALID_ID", 0, 0, "Ride ID cannot be null");
        }

        // Lock the Ride row to serialize concurrent recovery / acceptance execution
        Ride ride = rideRepository.findByIdForUpdate(rideId).orElse(null);
        if (ride == null) {
            return new DispatchRecoveryResult(rideId, "RIDE_NOT_FOUND", 0, 0, "Ride not found with id: " + rideId);
        }

        if (ride.getStatus() != RideStatus.REQUESTED || ride.getDriver() != null) {
            return new DispatchRecoveryResult(rideId, "RIDE_NOT_AVAILABLE", 0, 0,
                    "Ride is no longer awaiting dispatch (status: " + ride.getStatus() + ")");
        }

        Instant now = Instant.now();
        List<RideOffer> allOffers = rideOfferRepository.findByRideId(rideId);

        // Check for any currently active (non-expired) pending offers
        boolean hasActivePendingOffer = false;
        for (RideOffer o : allOffers) {
            if (o.getStatus() == RideOfferStatus.PENDING) {
                if (o.isExpired(now)) {
                    o.setStatus(RideOfferStatus.EXPIRED);
                    rideOfferRepository.save(o);
                    if (eventPublisher != null) {
                        eventPublisher.publishEvent(new RideOfferDomainEvent(o));
                    }
                } else {
                    hasActivePendingOffer = true;
                }
            }
        }

        if (hasActivePendingOffer) {
            return new DispatchRecoveryResult(rideId, "PENDING_OFFERS_EXIST", 0, 0,
                    "Ride still has pending offer(s) awaiting driver response");
        }

        // Prevent repeated offers to any driver who has previously received an offer for this ride
        Set<UUID> previouslyOfferedDriverIds = allOffers.stream()
                .map(o -> o.getDriver().getId())
                .collect(Collectors.toSet());

        int effectiveBatchSize = Math.max(1, this.batchSize);
        // Calculate retry attempt count: each round of offers counts as a dispatch round
        int retryAttempt = Math.max(1, allOffers.size() / effectiveBatchSize);

        if (retryAttempt > maxRetries) {
            return new DispatchRecoveryResult(rideId, "RETRY_LIMIT_EXCEEDED", 0, retryAttempt,
                    "Dispatch retry limit of " + maxRetries + " exceeded");
        }

        List<NearbyDriverResponse> nearbyDrivers = driverDiscoveryService.findNearbyDrivers(
                ride.getPickupLatitude(),
                ride.getPickupLongitude(),
                DriverDiscoveryService.DEFAULT_RADIUS_KM,
                null
        );

        List<Driver> candidatesToOffer = new ArrayList<>();
        for (NearbyDriverResponse candidate : nearbyDrivers) {
            if (previouslyOfferedDriverIds.contains(candidate.getDriverId())) {
                continue;
            }

            Driver driver = driverRepository.findById(candidate.getDriverId()).orElse(null);
            if (driver == null) {
                continue;
            }

            if (driver.getUser().getId().equals(ride.getCustomer().getId())) {
                continue;
            }

            candidatesToOffer.add(driver);
            if (candidatesToOffer.size() >= effectiveBatchSize) {
                break;
            }
        }

        if (candidatesToOffer.isEmpty()) {
            return new DispatchRecoveryResult(rideId, "EXHAUSTED_DRIVERS", 0, retryAttempt,
                    "No further eligible drivers available within search radius");
        }

        Instant expiresAt = now.plus(Duration.ofSeconds(offerExpirySeconds));
        List<RideOffer> newOffers = new ArrayList<>();
        for (Driver driver : candidatesToOffer) {
            RideOffer offer = new RideOffer(ride, driver, expiresAt);
            RideOffer saved = rideOfferRepository.save(offer);
            newOffers.add(saved);
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new RideOfferDomainEvent(saved));
            }
        }

        return new DispatchRecoveryResult(rideId, "RE_DISPATCHED", newOffers.size(), retryAttempt,
                "Ride re-dispatched to " + newOffers.size() + " driver(s) (retry " + retryAttempt + " of " + maxRetries + ")");
    }

    private Driver getAuthenticatedDriver(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (principal.getRole() != Role.DRIVER) {
            throw new AccessDeniedException("Access denied: driver role required");
        }
        return driverRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for user: " + principal.getId()));
    }
}

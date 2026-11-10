package com.rideflow.ride;

import com.rideflow.common.exception.BadRequestException;
import com.rideflow.common.exception.ResourceNotFoundException;
import com.rideflow.driver.Driver;
import com.rideflow.driver.DriverRepository;
import com.rideflow.driver.VerificationStatus;
import com.rideflow.fare.FareEstimateRequest;
import com.rideflow.fare.FareEstimateResponse;
import com.rideflow.fare.FareService;
import com.rideflow.security.UserPrincipal;
import com.rideflow.user.Role;
import com.rideflow.user.User;
import com.rideflow.user.UserRepository;
import com.rideflow.websocket.event.RideLifecycleDomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service managing customer ride requests, fare estimation operations,
 * and driver ride lifecycle state transitions.
 */
@Service
public class RideService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id",
            "status",
            "estimatedFare",
            "currency",
            "createdAt",
            "updatedAt",
            "pickupLatitude",
            "pickupLongitude",
            "dropoffLatitude",
            "dropoffLongitude",
            "pickupAddress",
            "dropoffAddress"
    );

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final FareService fareService;
    private final ApplicationEventPublisher eventPublisher;

    public RideService(RideRepository rideRepository,
                       UserRepository userRepository,
                       DriverRepository driverRepository,
                       FareService fareService,
                       ApplicationEventPublisher eventPublisher) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.fareService = fareService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Estimates the distance and fare between specified pickup and dropoff coordinates.
     *
     * @param request the fare estimate request
     * @return fare estimate response
     */
    public FareEstimateResponse estimateFare(FareEstimateRequest request) {
        return fareService.estimateFare(request);
    }

    /**
     * Creates and persists a new ride request for the authenticated customer.
     * The customer identity is derived solely from the authenticated principal.
     * The ride is created with status REQUESTED and unassigned driver.
     *
     * @param customerId ID of the authenticated customer
     * @param request    ride creation request details
     * @return safe RideResponse
     * @throws ResourceNotFoundException if customer is not found
     */
    @Transactional
    public RideResponse requestRide(UUID customerId, RideCreateRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        FareEstimateResponse estimate = fareService.estimateFare(
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                request.getDropoffLatitude(),
                request.getDropoffLongitude()
        );

        Ride ride = new Ride(
                customer,
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                request.getDropoffLatitude(),
                request.getDropoffLongitude(),
                request.getPickupAddress(),
                request.getDropoffAddress(),
                estimate.getEstimatedFare(),
                estimate.getCurrency()
        );

        Ride saved = rideRepository.save(ride);
        return RideResponse.from(saved);
    }

    /**
     * Accepts a ride in REQUESTED status by an approved and available driver.
     * Acquires a pessimistic write lock to protect against concurrent claims.
     *
     * @param rideId    the ID of the ride to accept
     * @param principal the authenticated driver principal
     * @return safe RideResponse with status ACCEPTED and driver profile assigned
     * @throws AccessDeniedException     if caller is not a driver
     * @throws ResourceNotFoundException if driver profile or ride is not found
     * @throws BadRequestException       if driver is unapproved/unavailable, or ride is not REQUESTED
     */
    @Transactional
    public RideResponse acceptRide(UUID rideId, UserPrincipal principal) {
        Driver driver = getAuthenticatedDriver(principal);

        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));

        if (driver.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new BadRequestException("Driver must have APPROVED verification status to accept a ride");
        }

        if (!driver.isAvailable()) {
            throw new BadRequestException("Driver must be available to accept a ride");
        }

        if (ride.getStatus() != RideStatus.REQUESTED || ride.getDriver() != null) {
            throw new BadRequestException("Ride cannot be accepted because it is in status: " + ride.getStatus());
        }

        if (ride.getCustomer().getId().equals(driver.getUser().getId())) {
            throw new BadRequestException("Driver cannot accept their own ride request");
        }

        ride.setDriver(driver);
        ride.setStatus(RideStatus.ACCEPTED);
        Ride saved = rideRepository.save(ride);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new RideLifecycleDomainEvent(
                    saved.getId(), RideStatus.ACCEPTED, saved.getCustomer().getId(), driver.getId(),
                    "Ride accepted by driver", Instant.now()
            ));
        }
        return RideResponse.from(saved);
    }

    /**
     * Marks the ride as ARRIVED by the assigned driver.
     *
     * @param rideId    the ID of the ride
     * @param principal the authenticated driver principal
     * @return safe RideResponse with status ARRIVED
     * @throws AccessDeniedException     if caller is not the assigned driver
     * @throws ResourceNotFoundException if driver profile or ride is not found
     * @throws BadRequestException       if ride is not in ACCEPTED status
     */
    @Transactional
    public RideResponse markArrived(UUID rideId, UserPrincipal principal) {
        Driver driver = getAuthenticatedDriver(principal);

        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));

        validateRideOwnership(ride, driver);

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new BadRequestException("Ride cannot be marked ARRIVED because it is in status: " + ride.getStatus());
        }

        ride.setStatus(RideStatus.ARRIVED);
        Ride saved = rideRepository.save(ride);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new RideLifecycleDomainEvent(
                    saved.getId(), RideStatus.ARRIVED, saved.getCustomer().getId(), driver.getId(),
                    "Driver arrived at pickup location", Instant.now()
            ));
        }
        return RideResponse.from(saved);
    }

    /**
     * Starts the trip, changing ride status to IN_PROGRESS by the assigned driver.
     *
     * @param rideId    the ID of the ride
     * @param principal the authenticated driver principal
     * @return safe RideResponse with status IN_PROGRESS
     * @throws AccessDeniedException     if caller is not the assigned driver
     * @throws ResourceNotFoundException if driver profile or ride is not found
     * @throws BadRequestException       if ride is not in ARRIVED status
     */
    @Transactional
    public RideResponse startRide(UUID rideId, UserPrincipal principal) {
        Driver driver = getAuthenticatedDriver(principal);

        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));

        validateRideOwnership(ride, driver);

        if (ride.getStatus() != RideStatus.ARRIVED) {
            throw new BadRequestException("Ride cannot be started because it is in status: " + ride.getStatus());
        }

        ride.setStatus(RideStatus.IN_PROGRESS);
        Ride saved = rideRepository.save(ride);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new RideLifecycleDomainEvent(
                    saved.getId(), RideStatus.IN_PROGRESS, saved.getCustomer().getId(), driver.getId(),
                    "Ride started", Instant.now()
            ));
        }
        return RideResponse.from(saved);
    }

    /**
     * Completes the trip, changing ride status to COMPLETED by the assigned driver.
     * Does not automatically alter driver availability status.
     *
     * @param rideId    the ID of the ride
     * @param principal the authenticated driver principal
     * @return safe RideResponse with status COMPLETED
     * @throws AccessDeniedException     if caller is not the assigned driver
     * @throws ResourceNotFoundException if driver profile or ride is not found
     * @throws BadRequestException       if ride is not in IN_PROGRESS status
     */
    @Transactional
    public RideResponse completeRide(UUID rideId, UserPrincipal principal) {
        Driver driver = getAuthenticatedDriver(principal);

        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));

        validateRideOwnership(ride, driver);

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new BadRequestException("Ride cannot be completed because it is in status: " + ride.getStatus());
        }

        ride.setStatus(RideStatus.COMPLETED);
        Ride saved = rideRepository.save(ride);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new RideLifecycleDomainEvent(
                    saved.getId(), RideStatus.COMPLETED, saved.getCustomer().getId(), driver.getId(),
                    "Ride completed", Instant.now()
            ));
        }
        return RideResponse.from(saved);
    }

    /**
     * Updates ride status according to requested target status in the lifecycle.
     *
     * @param rideId    the ID of the ride
     * @param principal the authenticated user principal
     * @param request   the status update request
     * @return safe RideResponse
     */
    @Transactional
    public RideResponse updateRideStatus(UUID rideId, UserPrincipal principal, RideStatusUpdateRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new BadRequestException("Status is required");
        }

        return switch (request.getStatus()) {
            case ACCEPTED -> acceptRide(rideId, principal);
            case ARRIVED -> markArrived(rideId, principal);
            case IN_PROGRESS -> startRide(rideId, principal);
            case COMPLETED -> completeRide(rideId, principal);
            case CANCELLED -> cancelRide(rideId, principal);
            default -> throw new BadRequestException("Unsupported status transition: " + request.getStatus());
        };
    }

    /**
     * Cancels a ride by the requesting customer.
     * Allowed only while the ride is in REQUESTED or ACCEPTED status.
     * Acquires a pessimistic write lock to prevent race conditions with driver acceptance or progression.
     *
     * @param rideId    the ID of the ride to cancel
     * @param principal the authenticated customer principal
     * @return safe RideResponse with status CANCELLED
     * @throws AccessDeniedException     if caller is not the owning customer
     * @throws ResourceNotFoundException if ride is not found
     * @throws BadRequestException       if ride is in ARRIVED, IN_PROGRESS, COMPLETED, or CANCELLED status
     */
    @Transactional
    public RideResponse cancelRide(UUID rideId, UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Access is denied: insufficient permissions");
        }

        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));

        if (!ride.getCustomer().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Only the customer who requested the ride may cancel it");
        }

        if (ride.getStatus() != RideStatus.REQUESTED && ride.getStatus() != RideStatus.ACCEPTED) {
            throw new BadRequestException("Ride cannot be cancelled in status: " + ride.getStatus());
        }

        ride.setStatus(RideStatus.CANCELLED);
        Ride saved = rideRepository.save(ride);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new RideLifecycleDomainEvent(
                    saved.getId(), RideStatus.CANCELLED, saved.getCustomer().getId(),
                    saved.getDriver() != null ? saved.getDriver().getId() : null,
                    "Ride cancelled", Instant.now()
            ));
        }
        return RideResponse.from(saved);
    }

    public static final int MAX_PAGE_SIZE = 50;
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Retrieves paginated ride history for the authenticated caller.
     * Customers see only rides they requested; drivers see only rides assigned to their profile.
     *
     * @param principal the authenticated caller principal
     * @param pageable  pagination request parameters
     * @return page of safe RideResponse objects
     */
    @Transactional(readOnly = true)
    public Page<RideResponse> getMyRides(UserPrincipal principal, Pageable pageable) {
        if (principal == null) {
            throw new AccessDeniedException("Access is denied: insufficient permissions");
        }

        Pageable safePageable = clampPageable(pageable);

        if (principal.getRole() == Role.DRIVER) {
            return driverRepository.findByUserId(principal.getId())
                    .map(driver -> rideRepository.findByDriverId(driver.getId(), safePageable).map(RideResponse::from))
                    .orElseGet(() -> Page.empty(safePageable));
        }

        // CUSTOMER (and default)
        return rideRepository.findByCustomerId(principal.getId(), safePageable).map(RideResponse::from);
    }

    private Pageable clampPageable(Pageable pageable) {
        Sort defaultSort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE, defaultSort);
        }
        int page = Math.max(0, pageable.getPageNumber());
        int size = pageable.getPageSize() > 0 ? Math.min(pageable.getPageSize(), MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;

        Sort safeSort = defaultSort;
        if (pageable.getSort().isSorted()) {
            List<Sort.Order> validOrders = new ArrayList<>();
            for (Sort.Order order : pageable.getSort()) {
                if (ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                    validOrders.add(order);
                }
            }
            if (!validOrders.isEmpty()) {
                safeSort = Sort.by(validOrders);
            }
        }
        return PageRequest.of(page, size, safeSort);
    }

    /**
     * Retrieves and verifies the driver profile associated with the authenticated principal.
     */
    private Driver getAuthenticatedDriver(UserPrincipal principal) {
        if (principal == null || principal.getRole() != Role.DRIVER) {
            throw new AccessDeniedException("Access is denied: insufficient permissions");
        }
        return driverRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for user: " + principal.getId()));
    }

    /**
     * Asserts that the authenticated driver is the one assigned to the ride.
     */
    private void validateRideOwnership(Ride ride, Driver driver) {
        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("Driver is not assigned to this ride");
        }
    }
}


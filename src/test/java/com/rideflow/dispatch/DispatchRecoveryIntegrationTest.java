package com.rideflow.dispatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.driver.Driver;
import com.rideflow.driver.DriverLocation;
import com.rideflow.driver.DriverLocationStore;
import com.rideflow.driver.DriverRepository;
import com.rideflow.driver.Vehicle;
import com.rideflow.driver.VehicleRepository;
import com.rideflow.driver.VehicleType;
import com.rideflow.driver.VerificationStatus;
import com.rideflow.ride.Ride;
import com.rideflow.ride.RideRepository;
import com.rideflow.ride.RideStatus;
import com.rideflow.security.JwtTokenProvider;
import com.rideflow.security.UserPrincipal;
import com.rideflow.user.Role;
import com.rideflow.user.User;
import com.rideflow.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DispatchRecoveryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private RideOfferRepository rideOfferRepository;

    @Autowired
    private DriverLocationStore driverLocationStore;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private ObjectMapper objectMapper;

    private User customerUser;
    private String customerToken;

    // Bangalore reference: Majestic (12.9772, 77.5708)
    private static final double PICKUP_LAT = 12.9772;
    private static final double PICKUP_LON = 77.5708;
    private static final double DROPOFF_LAT = 12.9352;
    private static final double DROPOFF_LON = 77.6245;

    @BeforeEach
    void setUp() {
        customerUser = userRepository.save(new User(
                "Customer Alex",
                "alex." + UUID.randomUUID().toString().substring(0, 8) + "@example.com",
                "+919800000001",
                passwordEncoder.encode("secret123"),
                Role.CUSTOMER
        ));
        customerToken = jwtTokenProvider.generateToken(customerUser);

        // Reset dispatch service defaults for tests
        dispatchService.setBatchSize(1);
        dispatchService.setMaxRetries(3);
        dispatchService.setAutoReDispatch(true);
        dispatchService.setOfferExpirySeconds(60);
    }

    @AfterEach
    void tearDown() {
        rideOfferRepository.deleteAll();
        rideRepository.deleteAll();
        vehicleRepository.deleteAll();
        driverRepository.deleteAll();
        userRepository.deleteAll();
        driverLocationStore.clear();

        // Restore default batch size
        dispatchService.setBatchSize(5);
        dispatchService.setMaxRetries(3);
        dispatchService.setAutoReDispatch(true);
    }

    private User createDriverUser(String prefix) {
        return userRepository.save(new User(
                "Driver " + prefix,
                prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@example.com",
                "+9197000" + UUID.randomUUID().toString().substring(0, 5),
                passwordEncoder.encode("secret123"),
                Role.DRIVER
        ));
    }

    private Driver createEligibleDriver(User user, double lat, double lon) {
        Driver driver = driverRepository.save(new Driver(
                user,
                "DL-" + UUID.randomUUID().toString().substring(0, 8),
                VerificationStatus.APPROVED,
                true
        ));

        vehicleRepository.save(new Vehicle(
                driver,
                VehicleType.SEDAN,
                "Honda",
                "City",
                "KA-02-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(),
                "Silver"
        ));

        driverLocationStore.saveLocation(new DriverLocation(
                driver.getId(),
                lat,
                lon,
                90.0,
                30.0,
                Instant.now()
        ));

        return driver;
    }

    private Ride createRequestedRide(User customer) {
        Ride ride = new Ride(
                customer,
                PICKUP_LAT,
                PICKUP_LON,
                DROPOFF_LAT,
                DROPOFF_LON,
                "Majestic Station, Bangalore",
                "Koramangala, Bangalore",
                BigDecimal.valueOf(180.00),
                "INR"
        );
        return rideRepository.save(ride);
    }

    // ==================== 1. Rejection & Auto Re-dispatch ====================

    @Test
    void testRecovery_onOfferRejected_reDispatchesToNextEligibleDriver() throws Exception {
        User user1 = createDriverUser("d1");
        Driver driver1 = createEligibleDriver(user1, 12.9800, 77.5720); // ~0.35 km

        User user2 = createDriverUser("d2");
        Driver driver2 = createEligibleDriver(user2, 12.9850, 77.5750); // ~1.0 km

        Ride ride = createRequestedRide(customerUser);

        // Initial dispatch with batchSize = 1 -> Driver 1 gets offer
        dispatchService.dispatchRide(ride.getId(), UserPrincipal.create(customerUser), new RideDispatchRequest(5.0, null, 1));

        List<RideOffer> d1Offers = rideOfferRepository.findByDriverIdAndStatus(driver1.getId(), RideOfferStatus.PENDING);
        assertEquals(1, d1Offers.size());
        UUID d1OfferId = d1Offers.get(0).getId();

        // Driver 2 does not have an offer yet
        assertTrue(rideOfferRepository.findByDriverIdAndStatus(driver2.getId(), RideOfferStatus.PENDING).isEmpty());

        // Driver 1 rejects the offer
        String driver1Token = jwtTokenProvider.generateToken(user1);
        RideOfferRejectRequest rejectRequest = new RideOfferRejectRequest("Car breakdown");

        mockMvc.perform(post("/api/v1/offers/" + d1OfferId + "/reject")
                        .header("Authorization", "Bearer " + driver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(d1OfferId.toString())))
                .andExpect(jsonPath("$.status", is("REJECTED")));

        // Auto re-dispatch should now have created an offer for Driver 2
        List<RideOffer> d2Offers = rideOfferRepository.findByDriverIdAndStatus(driver2.getId(), RideOfferStatus.PENDING);
        assertEquals(1, d2Offers.size());
        assertEquals(ride.getId(), d2Offers.get(0).getRide().getId());

        // Driver 2 retrieves their new pending offer
        String driver2Token = jwtTokenProvider.generateToken(user2);
        mockMvc.perform(get("/api/v1/drivers/me/offers")
                        .header("Authorization", "Bearer " + driver2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].driverId", is(driver2.getId().toString())));
    }

    // ==================== 2. Expiration & Auto Re-dispatch ====================

    @Test
    void testRecovery_onOfferExpired_reDispatchesToNextEligibleDriver() {
        User user1 = createDriverUser("d1");
        Driver driver1 = createEligibleDriver(user1, 12.9800, 77.5720);

        User user2 = createDriverUser("d2");
        Driver driver2 = createEligibleDriver(user2, 12.9850, 77.5750);

        Ride ride = createRequestedRide(customerUser);

        // Initial dispatch -> Driver 1 gets offer
        dispatchService.dispatchRide(ride.getId(), UserPrincipal.create(customerUser), new RideDispatchRequest(5.0, null, 1));

        List<RideOffer> d1Offers = rideOfferRepository.findByDriverIdAndStatus(driver1.getId(), RideOfferStatus.PENDING);
        assertEquals(1, d1Offers.size());
        RideOffer d1Offer = d1Offers.get(0);

        // Manually simulate expiration
        d1Offer.setExpiresAt(Instant.now().minusSeconds(10));
        rideOfferRepository.save(d1Offer);

        // Expire pending offers
        int expiredCount = dispatchService.expirePendingOffers();
        assertEquals(1, expiredCount);

        // Driver 1's offer is now EXPIRED
        RideOffer updatedD1 = rideOfferRepository.findById(d1Offer.getId()).orElseThrow();
        assertEquals(RideOfferStatus.EXPIRED, updatedD1.getStatus());

        // Driver 2 should now have an offer from recovery dispatch
        List<RideOffer> d2Offers = rideOfferRepository.findByDriverIdAndStatus(driver2.getId(), RideOfferStatus.PENDING);
        assertEquals(1, d2Offers.size());
        assertEquals(driver2.getId(), d2Offers.get(0).getDriver().getId());
    }

    // ==================== 3. Driver Exhaustion ====================

    @Test
    void testRecovery_exhaustedDrivers_stopsWhenNoMoreDriversAvailable() {
        User user1 = createDriverUser("d1");
        Driver driver1 = createEligibleDriver(user1, 12.9800, 77.5720);

        Ride ride = createRequestedRide(customerUser);

        // Only 1 driver available; dispatch
        dispatchService.dispatchRide(ride.getId(), UserPrincipal.create(customerUser), new RideDispatchRequest(5.0, null, 1));

        RideOffer offer = rideOfferRepository.findByDriverIdAndStatus(driver1.getId(), RideOfferStatus.PENDING).get(0);
        offer.setStatus(RideOfferStatus.REJECTED);
        rideOfferRepository.save(offer);

        // Attempt recovery
        DispatchRecoveryResult result = dispatchService.recoverDispatch(ride.getId());

        assertEquals("EXHAUSTED_DRIVERS", result.getStatus());
        assertEquals(0, result.getOffersCreated());

        // Total offers for ride remains 1
        assertEquals(1, rideOfferRepository.findByRideId(ride.getId()).size());
    }

    // ==================== 4. Retry Limits ====================

    @Test
    void testRecovery_retryLimits_stopsWhenMaxRetriesReached() {
        dispatchService.setMaxRetries(2); // Initial + 2 retries max

        List<Driver> drivers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            User u = createDriverUser("d" + i);
            drivers.add(createEligibleDriver(u, 12.9800 + (i * 0.005), 77.5720 + (i * 0.005)));
        }

        Ride ride = createRequestedRide(customerUser);

        // Attempt 0: Initial dispatch -> Driver 0 gets offer
        dispatchService.dispatchRide(ride.getId(), UserPrincipal.create(customerUser), new RideDispatchRequest(5.0, null, 1));
        assertEquals(1, rideOfferRepository.findByRideId(ride.getId()).size());

        // Driver 0 rejects -> Retry 1 triggers -> Driver 1 gets offer
        RideOffer o0 = rideOfferRepository.findByDriverIdAndStatus(drivers.get(0).getId(), RideOfferStatus.PENDING).get(0);
        o0.setStatus(RideOfferStatus.REJECTED);
        rideOfferRepository.save(o0);
        DispatchRecoveryResult r1 = dispatchService.recoverDispatch(ride.getId());
        assertEquals("RE_DISPATCHED", r1.getStatus());
        assertEquals(2, rideOfferRepository.findByRideId(ride.getId()).size());

        // Driver 1 rejects -> Retry 2 triggers -> Driver 2 gets offer
        RideOffer o1 = rideOfferRepository.findByDriverIdAndStatus(drivers.get(1).getId(), RideOfferStatus.PENDING).get(0);
        o1.setStatus(RideOfferStatus.REJECTED);
        rideOfferRepository.save(o1);
        DispatchRecoveryResult r2 = dispatchService.recoverDispatch(ride.getId());
        assertEquals("RE_DISPATCHED", r2.getStatus());
        assertEquals(3, rideOfferRepository.findByRideId(ride.getId()).size());

        // Driver 2 rejects -> Retry limit (2) exceeded! Should NOT offer Driver 3
        RideOffer o2 = rideOfferRepository.findByDriverIdAndStatus(drivers.get(2).getId(), RideOfferStatus.PENDING).get(0);
        o2.setStatus(RideOfferStatus.REJECTED);
        rideOfferRepository.save(o2);
        DispatchRecoveryResult r3 = dispatchService.recoverDispatch(ride.getId());
        assertEquals("RETRY_LIMIT_EXCEEDED", r3.getStatus());
        assertEquals(0, r3.getOffersCreated());

        // Total offers remains 3 (0, 1, 2)
        assertEquals(3, rideOfferRepository.findByRideId(ride.getId()).size());
    }

    // ==================== 5. Duplicate Prevention ====================

    @Test
    void testRecovery_duplicatePrevention_neverOffersSameDriverTwice() {
        User user1 = createDriverUser("d1");
        Driver driver1 = createEligibleDriver(user1, 12.9800, 77.5720);

        User user2 = createDriverUser("d2");
        Driver driver2 = createEligibleDriver(user2, 12.9850, 77.5750);

        Ride ride = createRequestedRide(customerUser);

        // Initial dispatch
        dispatchService.dispatchRide(ride.getId(), UserPrincipal.create(customerUser), new RideDispatchRequest(5.0, null, 1));
        // Driver 1 rejects
        RideOffer o1 = rideOfferRepository.findByDriverIdAndStatus(driver1.getId(), RideOfferStatus.PENDING).get(0);
        o1.setStatus(RideOfferStatus.REJECTED);
        rideOfferRepository.save(o1);

        // Re-dispatch to Driver 2
        dispatchService.recoverDispatch(ride.getId());
        // Driver 2 rejects
        RideOffer o2 = rideOfferRepository.findByDriverIdAndStatus(driver2.getId(), RideOfferStatus.PENDING).get(0);
        o2.setStatus(RideOfferStatus.REJECTED);
        rideOfferRepository.save(o2);

        // Another recovery attempt: should NOT re-offer Driver 1 or Driver 2
        DispatchRecoveryResult r = dispatchService.recoverDispatch(ride.getId());
        assertEquals("EXHAUSTED_DRIVERS", r.getStatus());

        List<RideOffer> allOffers = rideOfferRepository.findByRideId(ride.getId());
        Set<UUID> uniqueDrivers = new HashSet<>();
        for (RideOffer off : allOffers) {
            assertTrue(uniqueDrivers.add(off.getDriver().getId()), "Driver should only be offered once");
        }
    }

    // ==================== 6. Concurrent Dispatch Recovery ====================

    @Test
    void testRecovery_concurrentRecovery_handlesRacesSafelyWithoutMultipleOffers() throws InterruptedException {
        User user1 = createDriverUser("d1");
        Driver driver1 = createEligibleDriver(user1, 12.9800, 77.5720);

        User user2 = createDriverUser("d2");
        Driver driver2 = createEligibleDriver(user2, 12.9850, 77.5750);

        Ride ride = createRequestedRide(customerUser);

        // Initial dispatch
        dispatchService.dispatchRide(ride.getId(), UserPrincipal.create(customerUser), new RideDispatchRequest(5.0, null, 1));
        RideOffer initialOffer = rideOfferRepository.findByDriverIdAndStatus(driver1.getId(), RideOfferStatus.PENDING).get(0);
        initialOffer.setStatus(RideOfferStatus.REJECTED);
        rideOfferRepository.save(initialOffer);

        // 8 concurrent threads race to execute recoverDispatch on the same ride
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger redispatchSuccessCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    DispatchRecoveryResult res = dispatchService.recoverDispatch(ride.getId());
                    if ("RE_DISPATCHED".equals(res.getStatus())) {
                        redispatchSuccessCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed);
        // Exactly ONE thread must have succeeded in re-dispatching to Driver 2
        assertEquals(1, redispatchSuccessCount.get());

        // Total offers for ride is exactly 2 (initial to Driver 1, recovery to Driver 2)
        List<RideOffer> allOffers = rideOfferRepository.findByRideId(ride.getId());
        assertEquals(2, allOffers.size());
    }

    // ==================== 7. Accepted Ride Aborts Recovery ====================

    @Test
    void testRecovery_abortsWhenRideAlreadyAccepted() {
        User user1 = createDriverUser("d1");
        Driver driver1 = createEligibleDriver(user1, 12.9800, 77.5720);

        Ride ride = createRequestedRide(customerUser);
        ride.setDriver(driver1);
        ride.setStatus(RideStatus.ACCEPTED);
        rideRepository.save(ride);

        DispatchRecoveryResult result = dispatchService.recoverDispatch(ride.getId());
        assertEquals("RIDE_NOT_AVAILABLE", result.getStatus());
        assertEquals(0, result.getOffersCreated());
    }
}

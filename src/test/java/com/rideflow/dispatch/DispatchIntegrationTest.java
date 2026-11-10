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
import com.rideflow.fare.FareEstimateResponse;
import com.rideflow.fare.FareService;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DispatchIntegrationTest {

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
    }

    @AfterEach
    void tearDown() {
        rideOfferRepository.deleteAll();
        rideRepository.deleteAll();
        vehicleRepository.deleteAll();
        driverRepository.deleteAll();
        userRepository.deleteAll();
        driverLocationStore.clear();
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

    private Driver createEligibleDriver(User user, double lat, double lon, VehicleType vehicleType) {
        Driver driver = driverRepository.save(new Driver(
                user,
                "DL-" + UUID.randomUUID().toString().substring(0, 8),
                VerificationStatus.APPROVED,
                true
        ));

        vehicleRepository.save(new Vehicle(
                driver,
                vehicleType,
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

    @Test
    void testDispatchRide_success_createsOffersForEligibleNearbyDrivers() throws Exception {
        User user1 = createDriverUser("d1");
        Driver driver1 = createEligibleDriver(user1, 12.9800, 77.5720, VehicleType.SEDAN); // ~0.35 km

        User user2 = createDriverUser("d2");
        Driver driver2 = createEligibleDriver(user2, 12.9850, 77.5750, VehicleType.SEDAN); // ~1.0 km

        Ride ride = createRequestedRide(customerUser);

        RideDispatchRequest dispatchRequest = new RideDispatchRequest(5.0, null, 5);

        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/dispatch")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dispatchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId", is(ride.getId().toString())))
                .andExpect(jsonPath("$.status", is("DISPATCHED")))
                .andExpect(jsonPath("$.offersCount", is(2)));

        // Check Driver 1 can retrieve offer
        String driver1Token = jwtTokenProvider.generateToken(user1);
        mockMvc.perform(get("/api/v1/drivers/me/offers")
                        .header("Authorization", "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].driverId", is(driver1.getId().toString())))
                .andExpect(jsonPath("$[0].rideId", is(ride.getId().toString())))
                .andExpect(jsonPath("$[0].status", is("PENDING")))
                .andExpect(jsonPath("$[0].pickupAddress", is("Majestic Station, Bangalore")));

        // Check Driver 2 can retrieve offer
        String driver2Token = jwtTokenProvider.generateToken(user2);
        mockMvc.perform(get("/api/v1/drivers/me/offers")
                        .header("Authorization", "Bearer " + driver2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].driverId", is(driver2.getId().toString())));
    }

    @Test
    void testAcceptOffer_success_assignsDriverAndCancelsOtherOffers() throws Exception {
        User user1 = createDriverUser("d1");
        Driver driver1 = createEligibleDriver(user1, 12.9800, 77.5720, VehicleType.SEDAN);

        User user2 = createDriverUser("d2");
        Driver driver2 = createEligibleDriver(user2, 12.9850, 77.5750, VehicleType.SEDAN);

        Ride ride = createRequestedRide(customerUser);

        // Dispatch to both
        dispatchService.dispatchRide(ride.getId(), UserPrincipal.create(customerUser), null);

        List<RideOffer> d1Offers = rideOfferRepository.findByDriverIdAndStatus(driver1.getId(), RideOfferStatus.PENDING);
        assertEquals(1, d1Offers.size());
        UUID d1OfferId = d1Offers.get(0).getId();

        String driver1Token = jwtTokenProvider.generateToken(user1);

        // Driver 1 accepts
        mockMvc.perform(post("/api/v1/offers/" + d1OfferId + "/accept")
                        .header("Authorization", "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(d1OfferId.toString())))
                .andExpect(jsonPath("$.status", is("ACCEPTED")));

        // Verify Ride is now ACCEPTED and assigned to Driver 1
        Ride updatedRide = rideRepository.findById(ride.getId()).orElseThrow();
        assertEquals(RideStatus.ACCEPTED, updatedRide.getStatus());
        assertEquals(driver1.getId(), updatedRide.getDriver().getId());

        // Verify Driver 2's offer is now CANCELLED
        List<RideOffer> d2Offers = rideOfferRepository.findByRideId(ride.getId());
        for (RideOffer off : d2Offers) {
            if (off.getDriver().getId().equals(driver2.getId())) {
                assertEquals(RideOfferStatus.CANCELLED, off.getStatus());
            }
        }

        // Driver 2 sees 0 pending offers
        String driver2Token = jwtTokenProvider.generateToken(user2);
        mockMvc.perform(get("/api/v1/drivers/me/offers")
                        .header("Authorization", "Bearer " + driver2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testRejectOffer_success_marksOfferRejected() throws Exception {
        User user1 = createDriverUser("d1");
        Driver driver1 = createEligibleDriver(user1, 12.9800, 77.5720, VehicleType.SEDAN);

        Ride ride = createRequestedRide(customerUser);
        dispatchService.dispatchRide(ride.getId(), UserPrincipal.create(customerUser), null);

        List<RideOffer> offers = rideOfferRepository.findByDriverIdAndStatus(driver1.getId(), RideOfferStatus.PENDING);
        assertEquals(1, offers.size());
        UUID offerId = offers.get(0).getId();

        String driver1Token = jwtTokenProvider.generateToken(user1);
        RideOfferRejectRequest rejectRequest = new RideOfferRejectRequest("Too far from home");

        mockMvc.perform(post("/api/v1/offers/" + offerId + "/reject")
                        .header("Authorization", "Bearer " + driver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(offerId.toString())))
                .andExpect(jsonPath("$.status", is("REJECTED")));

        // Ride remains in REQUESTED status
        Ride updatedRide = rideRepository.findById(ride.getId()).orElseThrow();
        assertEquals(RideStatus.REQUESTED, updatedRide.getStatus());
        assertEquals(null, updatedRide.getDriver());
    }

    @Test
    void testOfferExpiration_cannotBeAcceptedWhenExpired() throws Exception {
        User user1 = createDriverUser("d1");
        Driver driver1 = createEligibleDriver(user1, 12.9800, 77.5720, VehicleType.SEDAN);

        Ride ride = createRequestedRide(customerUser);

        // Manually create an already-expired offer
        RideOffer expiredOffer = new RideOffer(
                ride,
                driver1,
                Instant.now().minusSeconds(10)
        );
        expiredOffer = rideOfferRepository.save(expiredOffer);

        String driver1Token = jwtTokenProvider.generateToken(user1);

        // Accept should fail with 400 Bad Request
        mockMvc.perform(post("/api/v1/offers/" + expiredOffer.getId() + "/accept")
                        .header("Authorization", "Bearer " + driver1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        // Driver sees 0 pending offers (expired offer is swept and marked EXPIRED)
        mockMvc.perform(get("/api/v1/drivers/me/offers")
                        .header("Authorization", "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Offer is now marked EXPIRED in DB
        RideOffer reloaded = rideOfferRepository.findById(expiredOffer.getId()).orElseThrow();
        assertEquals(RideOfferStatus.EXPIRED, reloaded.getStatus());
    }

    @Test
    void testDispatchRide_noEligibleDrivers_handlesGracefully() throws Exception {
        Ride ride = createRequestedRide(customerUser);

        // No drivers available
        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/dispatch")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId", is(ride.getId().toString())))
                .andExpect(jsonPath("$.status", is("NO_DRIVERS_AVAILABLE")))
                .andExpect(jsonPath("$.offersCount", is(0)));

        // Ride remains in REQUESTED status
        Ride reloaded = rideRepository.findById(ride.getId()).orElseThrow();
        assertEquals(RideStatus.REQUESTED, reloaded.getStatus());
    }

    @Test
    void testUnauthorizedAccess_enforcesOwnershipAndRoles() throws Exception {
        User user1 = createDriverUser("d1");
        Driver driver1 = createEligibleDriver(user1, 12.9800, 77.5720, VehicleType.SEDAN);

        User user2 = createDriverUser("d2");
        Driver driver2 = createEligibleDriver(user2, 12.9850, 77.5750, VehicleType.SEDAN);

        Ride ride = createRequestedRide(customerUser);
        dispatchService.dispatchRide(ride.getId(), UserPrincipal.create(customerUser), null);

        List<RideOffer> d1Offers = rideOfferRepository.findByDriverIdAndStatus(driver1.getId(), RideOfferStatus.PENDING);
        UUID d1OfferId = d1Offers.get(0).getId();

        // 1. Another customer tries to dispatch the ride -> 403 Forbidden
        User anotherCustomer = userRepository.save(new User(
                "Another Cust", "other@example.com", "+919811111111", passwordEncoder.encode("secret"), Role.CUSTOMER
        ));
        String otherCustomerToken = jwtTokenProvider.generateToken(anotherCustomer);
        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/dispatch")
                        .header("Authorization", "Bearer " + otherCustomerToken))
                .andExpect(status().isForbidden());

        // 2. Driver 2 tries to accept Driver 1's offer -> 403 Forbidden
        String driver2Token = jwtTokenProvider.generateToken(user2);
        mockMvc.perform(post("/api/v1/offers/" + d1OfferId + "/accept")
                        .header("Authorization", "Bearer " + driver2Token))
                .andExpect(status().isForbidden());

        // 3. Driver 2 tries to reject Driver 1's offer -> 403 Forbidden
        mockMvc.perform(post("/api/v1/offers/" + d1OfferId + "/reject")
                        .header("Authorization", "Bearer " + driver2Token))
                .andExpect(status().isForbidden());

        // 4. Customer tries to accept an offer -> 403 Forbidden
        mockMvc.perform(post("/api/v1/offers/" + d1OfferId + "/accept")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // 5. Unauthenticated request -> 401 Unauthorized
        mockMvc.perform(post("/api/v1/offers/" + d1OfferId + "/accept"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testDuplicateAcceptance_rejected() throws Exception {
        User user1 = createDriverUser("d1");
        Driver driver1 = createEligibleDriver(user1, 12.9800, 77.5720, VehicleType.SEDAN);

        Ride ride = createRequestedRide(customerUser);
        dispatchService.dispatchRide(ride.getId(), UserPrincipal.create(customerUser), null);

        List<RideOffer> offers = rideOfferRepository.findByDriverIdAndStatus(driver1.getId(), RideOfferStatus.PENDING);
        UUID offerId = offers.get(0).getId();
        String driver1Token = jwtTokenProvider.generateToken(user1);

        // First acceptance -> 200 OK
        mockMvc.perform(post("/api/v1/offers/" + offerId + "/accept")
                        .header("Authorization", "Bearer " + driver1Token))
                .andExpect(status().isOk());

        // Second acceptance on already accepted offer -> 400 Bad Request
        mockMvc.perform(post("/api/v1/offers/" + offerId + "/accept")
                        .header("Authorization", "Bearer " + driver1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }

    @Test
    void testCannotDispatchNonRequestedRide() throws Exception {
        Ride ride = createRequestedRide(customerUser);
        ride.setStatus(RideStatus.COMPLETED);
        rideRepository.save(ride);

        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/dispatch")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }

    @Test
    void testConcurrentAcceptance_onlyOneDriverSucceeds() throws Exception {
        User user1 = createDriverUser("cd1");
        Driver driver1 = createEligibleDriver(user1, 12.9800, 77.5720, VehicleType.SEDAN);

        User user2 = createDriverUser("cd2");
        Driver driver2 = createEligibleDriver(user2, 12.9850, 77.5750, VehicleType.SEDAN);

        Ride ride = createRequestedRide(customerUser);
        dispatchService.dispatchRide(ride.getId(), UserPrincipal.create(customerUser), null);

        List<RideOffer> d1Offers = rideOfferRepository.findByDriverIdAndStatus(driver1.getId(), RideOfferStatus.PENDING);
        List<RideOffer> d2Offers = rideOfferRepository.findByDriverIdAndStatus(driver2.getId(), RideOfferStatus.PENDING);
        assertEquals(1, d1Offers.size());
        assertEquals(1, d2Offers.size());

        UUID offer1Id = d1Offers.get(0).getId();
        UUID offer2Id = d2Offers.get(0).getId();

        UserPrincipal principal1 = UserPrincipal.create(user1);
        UserPrincipal principal2 = UserPrincipal.create(user2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                dispatchService.acceptOffer(offer1Id, principal1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                dispatchService.acceptOffer(offer2Id, principal2);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown(); // start both simultaneously
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Concurrent threads should complete within timeout");
        assertEquals(1, successCount.get(), "Exactly one driver must successfully accept the ride");
        assertEquals(1, failureCount.get(), "The other driver must fail to accept the ride");

        // Database consistency check
        Ride finalRide = rideRepository.findById(ride.getId()).orElseThrow();
        assertEquals(RideStatus.ACCEPTED, finalRide.getStatus());
        assertNotNull(finalRide.getDriver());

        List<RideOffer> finalOffers = rideOfferRepository.findByRideId(ride.getId());
        long acceptedOffers = finalOffers.stream().filter(o -> o.getStatus() == RideOfferStatus.ACCEPTED).count();
        long cancelledOffers = finalOffers.stream().filter(o -> o.getStatus() == RideOfferStatus.CANCELLED).count();

        assertEquals(1, acceptedOffers, "Database must contain exactly 1 ACCEPTED offer");
        assertEquals(1, cancelledOffers, "Database must contain exactly 1 CANCELLED offer");
    }
}

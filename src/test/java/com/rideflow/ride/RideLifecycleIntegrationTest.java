package com.rideflow.ride;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.driver.Driver;
import com.rideflow.driver.DriverRepository;
import com.rideflow.driver.VerificationStatus;
import com.rideflow.security.JwtTokenProvider;
import com.rideflow.user.Role;
import com.rideflow.user.User;
import com.rideflow.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RideLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        rideRepository.deleteAll();
        driverRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createCustomer(String email) {
        return userRepository.save(new User("Customer User", email, "+1900000001",
                passwordEncoder.encode("secret123"), Role.CUSTOMER));
    }

    private User createDriverUser(String email) {
        return userRepository.save(new User("Driver User", email, "+1900000002",
                passwordEncoder.encode("secret123"), Role.DRIVER));
    }

    private Driver createDriver(String email, String license, VerificationStatus status, boolean available) {
        User user = createDriverUser(email);
        return driverRepository.save(new Driver(user, license, status, available));
    }

    private Driver createApprovedAvailableDriver(String email, String license) {
        return createDriver(email, license, VerificationStatus.APPROVED, true);
    }

    private Ride createRequestedRide(User customer) {
        Ride ride = new Ride(
                customer,
                12.9716, 77.5946,
                12.9352, 77.6245,
                "MG Road, Bangalore",
                "Koramangala, Bangalore",
                BigDecimal.valueOf(150.00),
                "INR"
        );
        return rideRepository.save(ride);
    }

    private Ride createRideWithStatus(User customer, Driver driver, RideStatus status) {
        Ride ride = new Ride(
                customer,
                12.9716, 77.5946,
                12.9352, 77.6245,
                "MG Road, Bangalore",
                "Koramangala, Bangalore",
                BigDecimal.valueOf(150.00),
                "INR"
        );
        ride.setDriver(driver);
        ride.setStatus(status);
        return rideRepository.save(ride);
    }

    @Test
    void testFullLifecycleSuccess_RequestedToCompleted() throws Exception {
        User customer = createCustomer("lifecycle.cust@example.com");
        Driver driver = createApprovedAvailableDriver("lifecycle.drv@example.com", "DL-LIFE-001");
        String driverToken = jwtTokenProvider.generateToken(driver.getUser());

        Ride ride = createRequestedRide(customer);
        assertEquals(RideStatus.REQUESTED, ride.getStatus());

        // 1. Accept ride
        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/accept")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(ride.getId().toString())))
                .andExpect(jsonPath("$.status", is("ACCEPTED")))
                .andExpect(jsonPath("$.driverId", is(driver.getId().toString())))
                .andExpect(jsonPath("$.customerId", is(customer.getId().toString())));

        Ride accepted = rideRepository.findById(ride.getId()).orElseThrow();
        assertEquals(RideStatus.ACCEPTED, accepted.getStatus());
        assertEquals(driver.getId(), accepted.getDriver().getId());

        // 2. Mark arrived
        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/arrive")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ARRIVED")))
                .andExpect(jsonPath("$.driverId", is(driver.getId().toString())));

        Ride arrived = rideRepository.findById(ride.getId()).orElseThrow();
        assertEquals(RideStatus.ARRIVED, arrived.getStatus());

        // 3. Start ride
        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/start")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.driverId", is(driver.getId().toString())));

        Ride inProgress = rideRepository.findById(ride.getId()).orElseThrow();
        assertEquals(RideStatus.IN_PROGRESS, inProgress.getStatus());

        // 4. Complete ride
        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/complete")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.driverId", is(driver.getId().toString())));

        Ride completed = rideRepository.findById(ride.getId()).orElseThrow();
        assertEquals(RideStatus.COMPLETED, completed.getStatus());

        // Availability must not be changed automatically
        Driver driverAfterRide = driverRepository.findById(driver.getId()).orElseThrow();
        assertTrue(driverAfterRide.isAvailable(), "Driver availability should not be altered automatically");
    }

    @Test
    void testAlternativePostAndStatusEndpoints() throws Exception {
        User customer = createCustomer("alt.cust@example.com");
        Driver driver = createApprovedAvailableDriver("alt.drv@example.com", "DL-ALT-001");
        String driverToken = jwtTokenProvider.generateToken(driver.getUser());

        Ride ride = createRequestedRide(customer);

        // POST /api/v1/rides/{id}/accept
        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/accept")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));

        // POST /api/v1/rides/{id}/arrived
        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/arrived")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ARRIVED")));

        // PATCH /api/v1/rides/{id}/status with IN_PROGRESS
        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/status")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RideStatusUpdateRequest(RideStatus.IN_PROGRESS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        // POST /api/v1/rides/{id}/completed
        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/completed")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void testAcceptRideUnapprovedDriver_PendingReturns400() throws Exception {
        User customer = createCustomer("unapp.pending.c@example.com");
        Driver pendingDriver = createDriver("pending.drv@example.com", "DL-PEND-001", VerificationStatus.PENDING, false);
        String token = jwtTokenProvider.generateToken(pendingDriver.getUser());

        Ride ride = createRequestedRide(customer);

        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }

    @Test
    void testAcceptRideUnapprovedDriver_RejectedReturns400() throws Exception {
        User customer = createCustomer("unapp.rej.c@example.com");
        Driver rejectedDriver = createDriver("rej.drv@example.com", "DL-REJ-001", VerificationStatus.REJECTED, false);
        String token = jwtTokenProvider.generateToken(rejectedDriver.getUser());

        Ride ride = createRequestedRide(customer);

        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }

    @Test
    void testAcceptRideUnavailableDriverReturns400() throws Exception {
        User customer = createCustomer("unavail.c@example.com");
        Driver unavailDriver = createDriver("unavail.drv@example.com", "DL-UNAVAIL-001", VerificationStatus.APPROVED, false);
        String token = jwtTokenProvider.generateToken(unavailDriver.getUser());

        Ride ride = createRequestedRide(customer);

        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }

    @Test
    void testAcceptRideAlreadyAcceptedReturns400() throws Exception {
        User customer = createCustomer("already.c@example.com");
        Driver driver1 = createApprovedAvailableDriver("drv1.already@example.com", "DL-ALR-001");
        Driver driver2 = createApprovedAvailableDriver("drv2.already@example.com", "DL-ALR-002");
        String driver1Token = jwtTokenProvider.generateToken(driver1.getUser());
        String driver2Token = jwtTokenProvider.generateToken(driver2.getUser());

        Ride ride = createRequestedRide(customer);

        // Driver 1 accepts successfully
        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/accept")
                        .header("Authorization", "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));

        // Driver 2 attempts to accept already accepted ride
        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/accept")
                        .header("Authorization", "Bearer " + driver2Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }

    @Test
    void testInvalidTransitionsReturn400() throws Exception {
        User customer = createCustomer("inv.trans.c@example.com");
        Driver driver = createApprovedAvailableDriver("inv.trans.d@example.com", "DL-INV-001");
        String token = jwtTokenProvider.generateToken(driver.getUser());

        // 1. Cannot mark ARRIVED from REQUESTED
        Ride requestedRide = createRequestedRide(customer);
        requestedRide.setDriver(driver);
        rideRepository.save(requestedRide);
        mockMvc.perform(patch("/api/v1/rides/" + requestedRide.getId() + "/arrive")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        // 2. Cannot start trip from ACCEPTED (skipping ARRIVED)
        Ride acceptedRide = createRideWithStatus(customer, driver, RideStatus.ACCEPTED);
        mockMvc.perform(patch("/api/v1/rides/" + acceptedRide.getId() + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        // 3. Cannot complete trip from ACCEPTED
        mockMvc.perform(patch("/api/v1/rides/" + acceptedRide.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        // 4. Cannot complete trip from ARRIVED (skipping IN_PROGRESS)
        Ride arrivedRide = createRideWithStatus(customer, driver, RideStatus.ARRIVED);
        mockMvc.perform(patch("/api/v1/rides/" + arrivedRide.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        // 5. Cannot mark ARRIVED from IN_PROGRESS
        Ride inProgressRide = createRideWithStatus(customer, driver, RideStatus.IN_PROGRESS);
        mockMvc.perform(patch("/api/v1/rides/" + inProgressRide.getId() + "/arrive")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        // 6. Cannot perform transitions on COMPLETED ride
        Ride completedRide = createRideWithStatus(customer, driver, RideStatus.COMPLETED);
        mockMvc.perform(patch("/api/v1/rides/" + completedRide.getId() + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        mockMvc.perform(patch("/api/v1/rides/" + completedRide.getId() + "/arrive")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        mockMvc.perform(patch("/api/v1/rides/" + completedRide.getId() + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        mockMvc.perform(patch("/api/v1/rides/" + completedRide.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }

    @Test
    void testDriverOwnershipEnforcementReturns403() throws Exception {
        User customer = createCustomer("owner.c@example.com");
        Driver ownerDriver = createApprovedAvailableDriver("owner.d@example.com", "DL-OWN-001");
        Driver intruderDriver = createApprovedAvailableDriver("intruder.d@example.com", "DL-OWN-002");
        String intruderToken = jwtTokenProvider.generateToken(intruderDriver.getUser());

        // Ride is assigned to ownerDriver
        Ride acceptedRide = createRideWithStatus(customer, ownerDriver, RideStatus.ACCEPTED);

        // Intruder driver attempts to mark ARRIVED
        mockMvc.perform(patch("/api/v1/rides/" + acceptedRide.getId() + "/arrive")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)));

        // Intruder driver attempts to START
        Ride arrivedRide = createRideWithStatus(customer, ownerDriver, RideStatus.ARRIVED);
        mockMvc.perform(patch("/api/v1/rides/" + arrivedRide.getId() + "/start")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)));

        // Intruder driver attempts to COMPLETE
        Ride inProgressRide = createRideWithStatus(customer, ownerDriver, RideStatus.IN_PROGRESS);
        mockMvc.perform(patch("/api/v1/rides/" + inProgressRide.getId() + "/complete")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    void testCustomerAttemptingDriverTransitionsReturns403() throws Exception {
        User customer = createCustomer("cust.attempt@example.com");
        String customerToken = jwtTokenProvider.generateToken(customer);

        Ride ride = createRequestedRide(customer);

        // Customer attempts accept
        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/accept")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)));

        // Customer attempts arrive
        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/arrive")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)));

        // Customer attempts start
        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/start")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)));

        // Customer attempts complete
        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/complete")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    void testUnauthenticatedRequestsReturn401() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/rides/" + randomId + "/accept"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(patch("/api/v1/rides/" + randomId + "/arrive"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(patch("/api/v1/rides/" + randomId + "/start"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(patch("/api/v1/rides/" + randomId + "/complete"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testMissingRideReturns404() throws Exception {
        Driver driver = createApprovedAvailableDriver("missing.d@example.com", "DL-MISS-001");
        String token = jwtTokenProvider.generateToken(driver.getUser());
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/rides/" + nonExistentId + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.status", is(404)));

        mockMvc.perform(patch("/api/v1/rides/" + nonExistentId + "/arrive")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.status", is(404)));

        mockMvc.perform(patch("/api/v1/rides/" + nonExistentId + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.status", is(404)));

        mockMvc.perform(patch("/api/v1/rides/" + nonExistentId + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void testDriverWithoutDriverProfileReturns404() throws Exception {
        User driverUserWithoutProfile = createDriverUser("noprofile.drv@example.com");
        String token = jwtTokenProvider.generateToken(driverUserWithoutProfile);

        User customer = createCustomer("noprofile.cust@example.com");
        Ride ride = createRequestedRide(customer);

        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void testConcurrentAcceptanceProtection() throws Exception {
        User customer = createCustomer("concurrent.cust@example.com");
        Driver driver1 = createApprovedAvailableDriver("concurrent.d1@example.com", "DL-CONC-001");
        Driver driver2 = createApprovedAvailableDriver("concurrent.d2@example.com", "DL-CONC-002");

        String token1 = jwtTokenProvider.generateToken(driver1.getUser());
        String token2 = jwtTokenProvider.generateToken(driver2.getUser());

        Ride ride = createRequestedRide(customer);
        UUID rideId = ride.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<Integer> task1 = () -> {
            readyLatch.countDown();
            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.await(5, TimeUnit.SECONDS);
            MvcResult result = mockMvc.perform(patch("/api/v1/rides/" + rideId + "/accept")
                            .header("Authorization", "Bearer " + token1))
                    .andReturn();
            return result.getResponse().getStatus();
        };

        Callable<Integer> task2 = () -> {
            readyLatch.countDown();
            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.await(5, TimeUnit.SECONDS);
            MvcResult result = mockMvc.perform(patch("/api/v1/rides/" + rideId + "/accept")
                            .header("Authorization", "Bearer " + token2))
                    .andReturn();
            return result.getResponse().getStatus();
        };

        List<Callable<Integer>> tasks = List.of(task1, task2);
        List<Future<Integer>> futures = new ArrayList<>();
        for (Callable<Integer> task : tasks) {
            futures.add(executor.submit(task));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire both threads simultaneously

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : futures) {
            statuses.add(f.get(10, TimeUnit.SECONDS));
        }

        executor.shutdown();

        // Exactly one driver must succeed (200 OK) and the other must be rejected (400 Bad Request)
        assertTrue(statuses.contains(200), "One driver must successfully claim the ride with 200 OK");
        assertTrue(statuses.contains(400), "The other driver must be rejected with 400 Bad Request");

        Ride finalRide = rideRepository.findById(rideId).orElseThrow();
        assertEquals(RideStatus.ACCEPTED, finalRide.getStatus(), "Ride status must be ACCEPTED");
        assertNotNull(finalRide.getDriver(), "Ride must have an assigned driver");
        assertTrue(
                finalRide.getDriver().getId().equals(driver1.getId()) ||
                finalRide.getDriver().getId().equals(driver2.getId()),
                "Assigned driver must be either driver 1 or driver 2"
        );
    }

    @Test
    void testDriverCannotAcceptOwnRideRequestReturns400() throws Exception {
        Driver driver = createApprovedAvailableDriver("self.accept.drv@example.com", "DL-SELF-001");
        String driverToken = jwtTokenProvider.generateToken(driver.getUser());

        // Ride requested with driver user as customer
        Ride ride = createRequestedRide(driver.getUser());

        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/accept")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.detail", is("Driver cannot accept their own ride request")));
    }
}

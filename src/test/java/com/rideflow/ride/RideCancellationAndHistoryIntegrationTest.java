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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RideCancellationAndHistoryIntegrationTest {

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

    @Autowired
    private com.rideflow.common.exception.GlobalExceptionHandler globalExceptionHandler;

    @AfterEach
    void tearDown() {
        rideRepository.deleteAll();
        driverRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createCustomer(String email) {
        return userRepository.save(new User("Customer User", email, "+1911000001",
                passwordEncoder.encode("secret123"), Role.CUSTOMER));
    }

    private User createDriverUser(String email) {
        return userRepository.save(new User("Driver User", email, "+1911000002",
                passwordEncoder.encode("secret123"), Role.DRIVER));
    }

    private Driver createApprovedAvailableDriver(String email, String license) {
        User user = createDriverUser(email);
        return driverRepository.save(new Driver(user, license, VerificationStatus.APPROVED, true));
    }

    private Ride createRide(User customer, Driver driver, RideStatus status) {
        Ride ride = new Ride(
                customer,
                12.9716, 77.5946,
                12.9352, 77.6245,
                "Pickup Point",
                "Dropoff Point",
                BigDecimal.valueOf(120.00),
                "INR"
        );
        ride.setDriver(driver);
        ride.setStatus(status);
        return rideRepository.save(ride);
    }

    // ==================== Cancellation Tests ====================

    @Test
    void testCustomerCancelsRequestedRideSuccess() throws Exception {
        User customer = createCustomer("cancel.req@example.com");
        String token = jwtTokenProvider.generateToken(customer);
        Ride ride = createRide(customer, null, RideStatus.REQUESTED);

        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(ride.getId().toString())))
                .andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.customerId", is(customer.getId().toString())));

        Ride updated = rideRepository.findById(ride.getId()).orElseThrow();
        assertEquals(RideStatus.CANCELLED, updated.getStatus());
    }

    @Test
    void testCustomerCancelsAcceptedRideSuccess() throws Exception {
        User customer = createCustomer("cancel.acc@example.com");
        Driver driver = createApprovedAvailableDriver("cancel.acc.drv@example.com", "DL-CANC-001");
        String token = jwtTokenProvider.generateToken(customer);

        Ride ride = createRide(customer, driver, RideStatus.ACCEPTED);

        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(ride.getId().toString())))
                .andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.driverId", is(driver.getId().toString())));

        Ride updated = rideRepository.findById(ride.getId()).orElseThrow();
        assertEquals(RideStatus.CANCELLED, updated.getStatus());
    }

    @Test
    void testCancellationRejectedForOtherStatusesReturns400() throws Exception {
        User customer = createCustomer("cancel.other@example.com");
        Driver driver = createApprovedAvailableDriver("cancel.other.drv@example.com", "DL-CANC-002");
        String token = jwtTokenProvider.generateToken(customer);

        // 1. ARRIVED
        Ride arrivedRide = createRide(customer, driver, RideStatus.ARRIVED);
        mockMvc.perform(patch("/api/v1/rides/" + arrivedRide.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        // 2. IN_PROGRESS
        Ride inProgressRide = createRide(customer, driver, RideStatus.IN_PROGRESS);
        mockMvc.perform(patch("/api/v1/rides/" + inProgressRide.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        // 3. COMPLETED
        Ride completedRide = createRide(customer, driver, RideStatus.COMPLETED);
        mockMvc.perform(patch("/api/v1/rides/" + completedRide.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        // 4. CANCELLED (already cancelled)
        Ride cancelledRide = createRide(customer, null, RideStatus.CANCELLED);
        mockMvc.perform(patch("/api/v1/rides/" + cancelledRide.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }

    @Test
    void testWrongCustomerAttemptingCancellationReturns403() throws Exception {
        User ownerCustomer = createCustomer("owner.c@example.com");
        User intruderCustomer = createCustomer("intruder.c@example.com");
        String intruderToken = jwtTokenProvider.generateToken(intruderCustomer);

        Ride ride = createRide(ownerCustomer, null, RideStatus.REQUESTED);

        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/cancel")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    void testDriverAttemptingCustomerCancellationReturns403() throws Exception {
        User customer = createCustomer("owner.c2@example.com");
        Driver driver = createApprovedAvailableDriver("cancel.drv@example.com", "DL-CANC-003");
        String driverToken = jwtTokenProvider.generateToken(driver.getUser());

        Ride ride = createRide(customer, driver, RideStatus.ACCEPTED);

        mockMvc.perform(patch("/api/v1/rides/" + ride.getId() + "/cancel")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    void testCancellationMissingRideReturns404() throws Exception {
        User customer = createCustomer("missing.c@example.com");
        String token = jwtTokenProvider.generateToken(customer);
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/rides/" + nonExistentId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void testCancellationUnauthenticatedReturns401() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/rides/" + randomId + "/cancel"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    // ==================== Ride History Tests ====================

    @Test
    void testCustomerHistoryIsolation() throws Exception {
        User customer1 = createCustomer("hist.c1@example.com");
        User customer2 = createCustomer("hist.c2@example.com");

        Ride ride1 = createRide(customer1, null, RideStatus.REQUESTED);
        Ride ride2 = createRide(customer1, null, RideStatus.COMPLETED);
        Ride ride3 = createRide(customer2, null, RideStatus.REQUESTED);

        String token1 = jwtTokenProvider.generateToken(customer1);

        mockMvc.perform(get("/api/v1/rides/my-rides")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[?(@.id == '" + ride1.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id == '" + ride2.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id == '" + ride3.getId() + "')]").doesNotExist());
    }

    @Test
    void testDriverHistoryIsolation() throws Exception {
        User customer = createCustomer("hist.shared.c@example.com");
        Driver driver1 = createApprovedAvailableDriver("hist.d1@example.com", "DL-HIST-001");
        Driver driver2 = createApprovedAvailableDriver("hist.d2@example.com", "DL-HIST-002");

        Ride ride1 = createRide(customer, driver1, RideStatus.IN_PROGRESS);
        Ride ride2 = createRide(customer, driver2, RideStatus.COMPLETED);
        Ride ride3 = createRide(customer, null, RideStatus.REQUESTED); // unassigned

        String token1 = jwtTokenProvider.generateToken(driver1.getUser());

        mockMvc.perform(get("/api/v1/rides/my-rides")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(ride1.getId().toString())))
                .andExpect(jsonPath("$.content[?(@.id == '" + ride2.getId() + "')]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.id == '" + ride3.getId() + "')]").doesNotExist());
    }

    @Test
    void testDriverWithoutProfileReturnsEmptyHistory() throws Exception {
        User driverUser = createDriverUser("noprof.hist@example.com");
        String token = jwtTokenProvider.generateToken(driverUser);

        mockMvc.perform(get("/api/v1/rides/my-rides")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void testRideHistoryPagination() throws Exception {
        User customer = createCustomer("page.cust@example.com");
        String token = jwtTokenProvider.generateToken(customer);

        for (int i = 0; i < 5; i++) {
            createRide(customer, null, RideStatus.REQUESTED);
        }

        // Page 0, size 2
        mockMvc.perform(get("/api/v1/rides/my-rides?page=0&size=2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(5)))
                .andExpect(jsonPath("$.totalPages", is(3)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Page 1, size 2
        mockMvc.perform(get("/api/v1/rides/my-rides?page=1&size=2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is(1)))
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Page 2, size 2
        mockMvc.perform(get("/api/v1/rides/my-rides?page=2&size=2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is(2)))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void testRideHistoryMaxPageSizeCapping() throws Exception {
        User customer = createCustomer("cap.cust@example.com");
        String token = jwtTokenProvider.generateToken(customer);

        // Requested size 500 should be clamped to 50
        mockMvc.perform(get("/api/v1/rides/my-rides?page=0&size=500")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(50)));
    }

    @Test
    void testRideHistoryUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/rides/my-rides"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testRideHistoryDefaultSortOrdersByCreatedAtDesc() throws Exception {
        User customer = createCustomer("default.sort@example.com");
        String token = jwtTokenProvider.generateToken(customer);

        Ride ride1 = createRide(customer, null, RideStatus.REQUESTED);
        Thread.sleep(10);
        Ride ride2 = createRide(customer, null, RideStatus.REQUESTED);

        mockMvc.perform(get("/api/v1/rides/my-rides")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(ride2.getId().toString())))
                .andExpect(jsonPath("$.content[1].id", is(ride1.getId().toString())));
    }

    @Test
    void testRideHistoryValidSortProperty() throws Exception {
        User customer = createCustomer("valid.sort@example.com");
        String token = jwtTokenProvider.generateToken(customer);

        Ride rideLow = createRide(customer, null, RideStatus.REQUESTED);
        rideLow.setEstimatedFare(BigDecimal.valueOf(50.00));
        rideRepository.save(rideLow);

        Ride rideHigh = createRide(customer, null, RideStatus.REQUESTED);
        rideHigh.setEstimatedFare(BigDecimal.valueOf(250.00));
        rideRepository.save(rideHigh);

        mockMvc.perform(get("/api/v1/rides/my-rides?sort=estimatedFare,asc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(rideLow.getId().toString())))
                .andExpect(jsonPath("$.content[1].id", is(rideHigh.getId().toString())));
    }

    @Test
    void testRideHistoryInvalidSortPropertyFallsBackToDefaultAndDoesNotProduce500() throws Exception {
        User customer = createCustomer("invalid.sort@example.com");
        String token = jwtTokenProvider.generateToken(customer);

        Ride ride1 = createRide(customer, null, RideStatus.REQUESTED);
        Thread.sleep(10);
        Ride ride2 = createRide(customer, null, RideStatus.REQUESTED);

        // Unsupported / invalid property (e.g. database column name or arbitrary field)
        // must NOT throw HTTP 500; it falls back to default createdAt DESC
        mockMvc.perform(get("/api/v1/rides/my-rides?sort=created_at,desc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(ride2.getId().toString())))
                .andExpect(jsonPath("$.content[1].id", is(ride1.getId().toString())));

        mockMvc.perform(get("/api/v1/rides/my-rides?sort=nonExistentProperty,asc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(ride2.getId().toString())))
                .andExpect(jsonPath("$.content[1].id", is(ride1.getId().toString())));
    }

    @Test
    void testPropertyReferenceExceptionHandlerReturns400() {
        org.springframework.data.mapping.PropertyReferenceException ex =
                new org.springframework.data.mapping.PropertyReferenceException(
                        "invalidField",
                        org.springframework.data.util.ClassTypeInformation.from(Ride.class),
                        java.util.Collections.emptyList()
                );
        org.springframework.http.ProblemDetail problem = globalExceptionHandler.handlePropertyReference(ex);
        assertEquals(400, problem.getStatus());
        assertEquals("Bad Request", problem.getTitle());
        assertEquals("Invalid sort property: invalidField", problem.getDetail());
    }
}


package com.rideflow.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.driver.Driver;
import com.rideflow.driver.DriverRepository;
import com.rideflow.driver.VerificationStatus;
import com.rideflow.ride.Ride;
import com.rideflow.ride.RideRepository;
import com.rideflow.ride.RideStatus;
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
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        rideRepository.deleteAll();
        driverRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createCustomer(String email) {
        return userRepository.save(new User("Payment Customer", email, "+1920000001",
                passwordEncoder.encode("secret123"), Role.CUSTOMER));
    }

    private Driver createDriver(String email, String license) {
        User user = userRepository.save(new User("Payment Driver", email, "+1920000002",
                passwordEncoder.encode("secret123"), Role.DRIVER));
        return driverRepository.save(new Driver(user, license, VerificationStatus.APPROVED, true));
    }

    private Ride createRideWithStatus(User customer, Driver driver, RideStatus status) {
        Ride ride = new Ride(
                customer,
                12.9716, 77.5946,
                12.9352, 77.6245,
                "MG Road, Bangalore",
                "Koramangala, Bangalore",
                BigDecimal.valueOf(175.50),
                "INR"
        );
        ride.setDriver(driver);
        ride.setStatus(status);
        return rideRepository.save(ride);
    }

    @Test
    void testSimulatePaymentSuccessForCompletedRide() throws Exception {
        User customer = createCustomer("pay.succ@example.com");
        Driver driver = createDriver("pay.succ.d@example.com", "DL-PAY-001");
        String token = jwtTokenProvider.generateToken(customer);

        Ride ride = createRideWithStatus(customer, driver, RideStatus.COMPLETED);

        PaymentSimulationRequest request = new PaymentSimulationRequest(ride.getId());

        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.rideId", is(ride.getId().toString())))
                .andExpect(jsonPath("$.customerId", is(customer.getId().toString())))
                .andExpect(jsonPath("$.amount", is(175.50)))
                .andExpect(jsonPath("$.currency", is("INR")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.paymentMethod", is("SIMULATED")))
                .andExpect(jsonPath("$.transactionId", startsWith("SIM-TXN-")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andReturn();

        PaymentResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentResponse.class);
        Payment persisted = paymentRepository.findById(response.getId()).orElseThrow();
        assertEquals(PaymentStatus.SUCCESS, persisted.getStatus());
        assertEquals(new BigDecimal("175.50"), persisted.getAmount());
    }

    @Test
    void testSimulatePaymentViaRidePathEndpoint() throws Exception {
        User customer = createCustomer("pay.path@example.com");
        Driver driver = createDriver("pay.path.d@example.com", "DL-PAY-002");
        String token = jwtTokenProvider.generateToken(customer);

        Ride ride = createRideWithStatus(customer, driver, RideStatus.COMPLETED);

        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/payment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.rideId", is(ride.getId().toString())));
    }

    @Test
    void testSimulatePaymentFailure() throws Exception {
        User customer = createCustomer("pay.fail@example.com");
        Driver driver = createDriver("pay.fail.d@example.com", "DL-PAY-003");
        String token = jwtTokenProvider.generateToken(customer);

        Ride ride = createRideWithStatus(customer, driver, RideStatus.COMPLETED);

        PaymentSimulationRequest request = new PaymentSimulationRequest(ride.getId(), true);

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.transactionId", startsWith("SIM-FAIL-")));

        assertEquals(1, paymentRepository.findByRideId(ride.getId()).size());
        assertEquals(PaymentStatus.FAILED, paymentRepository.findByRideId(ride.getId()).get(0).getStatus());
    }

    @Test
    void testRetryAfterFailedPaymentSucceeds() throws Exception {
        User customer = createCustomer("pay.retry@example.com");
        Driver driver = createDriver("pay.retry.d@example.com", "DL-PAY-004");
        String token = jwtTokenProvider.generateToken(customer);

        Ride ride = createRideWithStatus(customer, driver, RideStatus.COMPLETED);

        // Attempt 1: Failed
        PaymentSimulationRequest failRequest = new PaymentSimulationRequest(ride.getId(), true);
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FAILED")));

        // Attempt 2: Success
        PaymentSimulationRequest successRequest = new PaymentSimulationRequest(ride.getId(), false);
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(successRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SUCCESS")));

        assertEquals(2, paymentRepository.findByRideId(ride.getId()).size());
    }

    @Test
    void testDuplicateSuccessfulPaymentReturns409Conflict() throws Exception {
        User customer = createCustomer("pay.dup@example.com");
        Driver driver = createDriver("pay.dup.d@example.com", "DL-PAY-005");
        String token = jwtTokenProvider.generateToken(customer);

        Ride ride = createRideWithStatus(customer, driver, RideStatus.COMPLETED);

        // First payment succeeds
        PaymentSimulationRequest request = new PaymentSimulationRequest(ride.getId());
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SUCCESS")));

        // Second payment attempt must be rejected
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Conflict")));
    }

    @Test
    void testIneligibleRideStatusesRejectedWith400() throws Exception {
        User customer = createCustomer("pay.inel@example.com");
        Driver driver = createDriver("pay.inel.d@example.com", "DL-PAY-006");
        String token = jwtTokenProvider.generateToken(customer);

        RideStatus[] ineligibleStatuses = {
                RideStatus.REQUESTED,
                RideStatus.ACCEPTED,
                RideStatus.ARRIVED,
                RideStatus.IN_PROGRESS,
                RideStatus.CANCELLED
        };

        for (RideStatus st : ineligibleStatuses) {
            Ride ride = createRideWithStatus(customer, driver, st);
            PaymentSimulationRequest request = new PaymentSimulationRequest(ride.getId());

            mockMvc.perform(post("/api/v1/payments")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title", is("Bad Request")));
        }
    }

    @Test
    void testOwnershipIsolationCustomerCannotPayForAnotherCustomerRide() throws Exception {
        User customer1 = createCustomer("pay.c1@example.com");
        User customer2 = createCustomer("pay.c2@example.com");
        Driver driver = createDriver("pay.iso.d@example.com", "DL-PAY-007");

        Ride ride = createRideWithStatus(customer1, driver, RideStatus.COMPLETED);

        String token2 = jwtTokenProvider.generateToken(customer2);
        PaymentSimulationRequest request = new PaymentSimulationRequest(ride.getId());

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    void testOwnershipIsolationDriverCannotPayForRide() throws Exception {
        User customer = createCustomer("pay.drivernotallowed.c@example.com");
        Driver driver = createDriver("pay.drivernotallowed.d@example.com", "DL-PAY-008");

        Ride ride = createRideWithStatus(customer, driver, RideStatus.COMPLETED);

        String driverToken = jwtTokenProvider.generateToken(driver.getUser());
        PaymentSimulationRequest request = new PaymentSimulationRequest(ride.getId());

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    void testOwnershipIsolationCustomerCannotViewAnotherCustomerPayment() throws Exception {
        User customer1 = createCustomer("view.c1@example.com");
        User customer2 = createCustomer("view.c2@example.com");
        Driver driver = createDriver("view.d@example.com", "DL-PAY-009");

        Ride ride = createRideWithStatus(customer1, driver, RideStatus.COMPLETED);
        String token1 = jwtTokenProvider.generateToken(customer1);
        String token2 = jwtTokenProvider.generateToken(customer2);

        // Customer 1 pays
        PaymentSimulationRequest request = new PaymentSimulationRequest(ride.getId());
        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentResponse.class);

        // Customer 2 attempts to view via ride endpoint
        mockMvc.perform(get("/api/v1/payments/ride/" + ride.getId())
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")));

        // Customer 2 attempts to view via payment ID endpoint
        mockMvc.perform(get("/api/v1/payments/" + response.getId())
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")));
    }

    @Test
    void testGetPaymentStatusSuccess() throws Exception {
        User customer = createCustomer("get.pay@example.com");
        Driver driver = createDriver("get.pay.d@example.com", "DL-PAY-010");
        String token = jwtTokenProvider.generateToken(customer);

        Ride ride = createRideWithStatus(customer, driver, RideStatus.COMPLETED);

        // Process payment
        PaymentSimulationRequest request = new PaymentSimulationRequest(ride.getId());
        MvcResult payResult = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponse created = objectMapper.readValue(payResult.getResponse().getContentAsString(), PaymentResponse.class);

        // 1. View via /api/v1/payments/ride/{rideId}
        mockMvc.perform(get("/api/v1/payments/ride/" + ride.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(created.getId().toString())))
                .andExpect(jsonPath("$.status", is("SUCCESS")));

        // 2. View via /api/v1/rides/{rideId}/payment
        mockMvc.perform(get("/api/v1/rides/" + ride.getId() + "/payment")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(created.getId().toString())))
                .andExpect(jsonPath("$.status", is("SUCCESS")));

        // 3. View via /api/v1/payments/{paymentId}
        mockMvc.perform(get("/api/v1/payments/" + created.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(created.getId().toString())))
                .andExpect(jsonPath("$.status", is("SUCCESS")));
    }

    @Test
    void testPaymentMissingResourcesReturn404() throws Exception {
        User customer = createCustomer("miss.pay@example.com");
        String token = jwtTokenProvider.generateToken(customer);
        UUID nonExistentId = UUID.randomUUID();

        // 1. Missing ride on simulate payment
        PaymentSimulationRequest req = new PaymentSimulationRequest(nonExistentId);
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.status", is(404)));

        // 2. Missing payment on get payment by ride
        Ride ride = createRideWithStatus(customer, null, RideStatus.COMPLETED);
        mockMvc.perform(get("/api/v1/payments/ride/" + ride.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));

        // 3. Missing payment ID
        mockMvc.perform(get("/api/v1/payments/" + nonExistentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    void testUnauthenticatedAccessReturns401() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rideId\":\"" + randomId + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(get("/api/v1/payments/ride/" + randomId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(get("/api/v1/payments/" + randomId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testNoSensitiveFinancialDataExposedInResponse() throws Exception {
        User customer = createCustomer("safety.pay@example.com");
        Driver driver = createDriver("safety.pay.d@example.com", "DL-PAY-011");
        String token = jwtTokenProvider.generateToken(customer);

        Ride ride = createRideWithStatus(customer, driver, RideStatus.COMPLETED);

        PaymentSimulationRequest request = new PaymentSimulationRequest(ride.getId());
        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertFalse(json.contains("cardNumber"), "Must not expose card number");
        assertFalse(json.contains("cvv"), "Must not expose CVV");
        assertFalse(json.contains("password"), "Must not expose password");
        assertFalse(json.contains("bankAccount"), "Must not expose bank account");
    }

    @Test
    void testPaymentSimulationPathAndBodyRideIdMismatchReturns400() throws Exception {
        User customer = createCustomer("mismatch.pay@example.com");
        Driver driver = createDriver("mismatch.pay.d@example.com", "DL-PAY-012");
        String token = jwtTokenProvider.generateToken(customer);

        Ride rideA = createRideWithStatus(customer, driver, RideStatus.COMPLETED);
        UUID rideBId = UUID.randomUUID();

        // Path is rideA, but body specifies rideB
        PaymentSimulationRequest request = new PaymentSimulationRequest(rideBId);
        mockMvc.perform(post("/api/v1/rides/" + rideA.getId() + "/payment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.detail", is("Ride ID in path does not match ride ID in request body")));
    }

    @Test
    void testPaymentSimulationPendingStatusReturns400() throws Exception {
        User customer = createCustomer("pending.pay@example.com");
        Driver driver = createDriver("pending.pay.d@example.com", "DL-PAY-013");
        String token = jwtTokenProvider.generateToken(customer);

        Ride ride = createRideWithStatus(customer, driver, RideStatus.COMPLETED);

        String jsonPayload = String.format("""
                {
                    "rideId": "%s",
                    "status": "PENDING"
                }
                """, ride.getId());

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.detail", is("Simulated payment status cannot be PENDING; must be SUCCESS or FAILED")));
    }

    @Test
    void testPaymentSimulationOversizedPaymentMethodReturns400() throws Exception {
        User customer = createCustomer("oversize.pay@example.com");
        Driver driver = createDriver("oversize.pay.d@example.com", "DL-PAY-014");
        String token = jwtTokenProvider.generateToken(customer);

        Ride ride = createRideWithStatus(customer, driver, RideStatus.COMPLETED);

        String longMethod = "A".repeat(55);
        String jsonPayload = String.format("""
                {
                    "rideId": "%s",
                    "paymentMethod": "%s"
                }
                """, ride.getId(), longMethod);

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }
}

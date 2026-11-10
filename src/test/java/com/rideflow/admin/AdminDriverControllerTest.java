package com.rideflow.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.driver.Driver;
import com.rideflow.driver.DriverRepository;
import com.rideflow.driver.DriverVerificationRequest;
import com.rideflow.driver.VerificationStatus;
import com.rideflow.security.JwtTokenProvider;
import com.rideflow.user.Role;
import com.rideflow.user.User;
import com.rideflow.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminDriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User createAdminUser(String email) {
        return userRepository.save(new User("Admin User", email, "+1999999999",
                passwordEncoder.encode("adminPass123"), Role.ADMIN));
    }

    private User createCustomerUser(String email) {
        return userRepository.save(new User("Customer User", email, "+1888888888",
                passwordEncoder.encode("custPass123"), Role.CUSTOMER));
    }

    private User createDriverUser(String email) {
        return userRepository.save(new User("Driver User", email, "+1777777777",
                passwordEncoder.encode("driverPass123"), Role.DRIVER));
    }

    @Test
    void testAdminApproveDriverSuccess() throws Exception {
        User admin = createAdminUser("admin.approve@example.com");
        String adminToken = jwtTokenProvider.generateToken(admin);

        User driverUser = createDriverUser("applicant.appr@example.com");
        Driver driver = driverRepository.save(new Driver(driverUser, "DL-ADM-APPR-1", VerificationStatus.PENDING, false));

        DriverVerificationRequest request = new DriverVerificationRequest(VerificationStatus.APPROVED);

        mockMvc.perform(patch("/api/v1/admin/drivers/" + driver.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(driver.getId().toString())))
                .andExpect(jsonPath("$.userId", is(driverUser.getId().toString())))
                .andExpect(jsonPath("$.licenseNumber", is("DL-ADM-APPR-1")))
                .andExpect(jsonPath("$.verificationStatus", is("APPROVED")))
                .andExpect(jsonPath("$.available", is(false)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist());

        Driver updated = driverRepository.findById(driver.getId()).orElseThrow();
        assertEquals(VerificationStatus.APPROVED, updated.getVerificationStatus(), "Verification status must be APPROVED");
        assertFalse(updated.isAvailable(), "Approval must not automatically set available to true");
    }

    @Test
    void testAdminRejectDriverSuccess() throws Exception {
        User admin = createAdminUser("admin.reject@example.com");
        String adminToken = jwtTokenProvider.generateToken(admin);

        User driverUser = createDriverUser("applicant.rej@example.com");
        Driver driver = driverRepository.save(new Driver(driverUser, "DL-ADM-REJ-1", VerificationStatus.PENDING, false));

        DriverVerificationRequest request = new DriverVerificationRequest(VerificationStatus.REJECTED);

        mockMvc.perform(patch("/api/v1/admin/drivers/" + driver.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(driver.getId().toString())))
                .andExpect(jsonPath("$.verificationStatus", is("REJECTED")))
                .andExpect(jsonPath("$.available", is(false)));

        Driver updated = driverRepository.findById(driver.getId()).orElseThrow();
        assertEquals(VerificationStatus.REJECTED, updated.getVerificationStatus(), "Verification status must be REJECTED");
        assertFalse(updated.isAvailable(), "Driver must remain unavailable upon rejection");
    }

    @Test
    void testAdminRejectionForcesAvailabilityToFalse() throws Exception {
        User admin = createAdminUser("admin.forceoff@example.com");
        String adminToken = jwtTokenProvider.generateToken(admin);

        User driverUser = createDriverUser("active.driver.rej@example.com");
        // Driver was approved and actively available
        Driver driver = driverRepository.save(new Driver(driverUser, "DL-ACT-FORCE-OFF", VerificationStatus.APPROVED, true));
        assertTrue(driver.isAvailable(), "Precondition: driver is initially available");

        DriverVerificationRequest request = new DriverVerificationRequest(VerificationStatus.REJECTED);

        mockMvc.perform(patch("/api/v1/admin/drivers/" + driver.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(driver.getId().toString())))
                .andExpect(jsonPath("$.verificationStatus", is("REJECTED")))
                .andExpect(jsonPath("$.available", is(false)));

        Driver updated = driverRepository.findById(driver.getId()).orElseThrow();
        assertEquals(VerificationStatus.REJECTED, updated.getVerificationStatus());
        assertFalse(updated.isAvailable(), "Rejection must force availability to false");
    }

    @Test
    void testAdminApprovalDoesNotAutomaticallyEnableAvailability() throws Exception {
        User admin = createAdminUser("admin.noautoavail@example.com");
        String adminToken = jwtTokenProvider.generateToken(admin);

        User driverUser = createDriverUser("applicant.noauto@example.com");
        Driver driver = driverRepository.save(new Driver(driverUser, "DL-NO-AUTO-AVAIL", VerificationStatus.PENDING, false));

        DriverVerificationRequest request = new DriverVerificationRequest(VerificationStatus.APPROVED);

        mockMvc.perform(patch("/api/v1/admin/drivers/" + driver.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus", is("APPROVED")))
                .andExpect(jsonPath("$.available", is(false)));

        Driver updated = driverRepository.findById(driver.getId()).orElseThrow();
        assertFalse(updated.isAvailable(), "Approval must NOT automatically enable availability");
    }

    @Test
    void testCustomerDeniedAccessReturns403() throws Exception {
        User customer = createCustomerUser("customer.forbidden@example.com");
        String customerToken = jwtTokenProvider.generateToken(customer);

        User driverUser = createDriverUser("driver.target1@example.com");
        Driver driver = driverRepository.save(new Driver(driverUser, "DL-CUST-FORBID-1"));

        DriverVerificationRequest request = new DriverVerificationRequest(VerificationStatus.APPROVED);

        mockMvc.perform(patch("/api/v1/admin/drivers/" + driver.getId() + "/verification")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    void testDriverDeniedAccessReturns403() throws Exception {
        User driverSelf = createDriverUser("driver.forbidden@example.com");
        String driverToken = jwtTokenProvider.generateToken(driverSelf);

        User otherDriverUser = createDriverUser("other.driver@example.com");
        Driver targetDriver = driverRepository.save(new Driver(otherDriverUser, "DL-DRV-FORBID-1"));

        DriverVerificationRequest request = new DriverVerificationRequest(VerificationStatus.APPROVED);

        mockMvc.perform(patch("/api/v1/admin/drivers/" + targetDriver.getId() + "/verification")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    void testUnauthenticatedAccessReturns401() throws Exception {
        UUID randomId = UUID.randomUUID();
        DriverVerificationRequest request = new DriverVerificationRequest(VerificationStatus.APPROVED);

        mockMvc.perform(patch("/api/v1/admin/drivers/" + randomId + "/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testInvalidTokenReturns401() throws Exception {
        UUID randomId = UUID.randomUUID();
        DriverVerificationRequest request = new DriverVerificationRequest(VerificationStatus.APPROVED);

        mockMvc.perform(patch("/api/v1/admin/drivers/" + randomId + "/verification")
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testUnknownDriverIdReturns404() throws Exception {
        User admin = createAdminUser("admin.unknown@example.com");
        String adminToken = jwtTokenProvider.generateToken(admin);

        UUID nonExistentDriverId = UUID.randomUUID();
        DriverVerificationRequest request = new DriverVerificationRequest(VerificationStatus.APPROVED);

        mockMvc.perform(patch("/api/v1/admin/drivers/" + nonExistentDriverId + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void testUnsupportedStatusPendingReturns400() throws Exception {
        User admin = createAdminUser("admin.unsupported@example.com");
        String adminToken = jwtTokenProvider.generateToken(admin);

        User driverUser = createDriverUser("target.unsupported@example.com");
        Driver driver = driverRepository.save(new Driver(driverUser, "DL-UNSUPPORTED-1", VerificationStatus.APPROVED, false));

        DriverVerificationRequest request = new DriverVerificationRequest(VerificationStatus.PENDING);

        mockMvc.perform(patch("/api/v1/admin/drivers/" + driver.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", is("Verification status must be either APPROVED or REJECTED")));
    }

    @Test
    void testMissingStatusReturns400() throws Exception {
        User admin = createAdminUser("admin.missing@example.com");
        String adminToken = jwtTokenProvider.generateToken(admin);

        User driverUser = createDriverUser("target.missing@example.com");
        Driver driver = driverRepository.save(new Driver(driverUser, "DL-MISSING-1"));

        mockMvc.perform(patch("/api/v1/admin/drivers/" + driver.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.status", notNullValue()));
    }

    @Test
    void testNullStatusReturns400() throws Exception {
        User admin = createAdminUser("admin.nullstatus@example.com");
        String adminToken = jwtTokenProvider.generateToken(admin);

        User driverUser = createDriverUser("target.nullstatus@example.com");
        Driver driver = driverRepository.save(new Driver(driverUser, "DL-NULL-STATUS-1"));

        mockMvc.perform(patch("/api/v1/admin/drivers/" + driver.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.status", notNullValue()));
    }

    @Test
    void testInvalidStatusStringReturns400() throws Exception {
        User admin = createAdminUser("admin.invalidstr@example.com");
        String adminToken = jwtTokenProvider.generateToken(admin);

        User driverUser = createDriverUser("target.invalidstr@example.com");
        Driver driver = driverRepository.save(new Driver(driverUser, "DL-INVALID-STR-1"));

        mockMvc.perform(patch("/api/v1/admin/drivers/" + driver.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"UNRECOGNIZED_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void testEmptyBodyReturns400() throws Exception {
        User admin = createAdminUser("admin.emptybody@example.com");
        String adminToken = jwtTokenProvider.generateToken(admin);

        User driverUser = createDriverUser("target.emptybody@example.com");
        Driver driver = driverRepository.save(new Driver(driverUser, "DL-EMPTY-BODY-1"));

        mockMvc.perform(patch("/api/v1/admin/drivers/" + driver.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void testVerificationStatusAliasAccepted() throws Exception {
        User admin = createAdminUser("admin.alias@example.com");
        String adminToken = jwtTokenProvider.generateToken(admin);

        User driverUser = createDriverUser("applicant.alias@example.com");
        Driver driver = driverRepository.save(new Driver(driverUser, "DL-ALIAS-1", VerificationStatus.PENDING, false));

        mockMvc.perform(patch("/api/v1/admin/drivers/" + driver.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verificationStatus\": \"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus", is("APPROVED")));

        Driver updated = driverRepository.findById(driver.getId()).orElseThrow();
        assertEquals(VerificationStatus.APPROVED, updated.getVerificationStatus());
    }
}

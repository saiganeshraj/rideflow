package com.rideflow.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DriverControllerTest {

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

    @Test
    void testRegisterDriverSuccess() throws Exception {
        User user = userRepository.save(new User("Driver Applicant", "applicant@example.com",
                "+1234567890", passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(user);

        DriverRegistrationRequest request = new DriverRegistrationRequest("DL-VALID-12345");

        mockMvc.perform(post("/api/v1/drivers/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.userId", is(user.getId().toString())))
                .andExpect(jsonPath("$.licenseNumber", is("DL-VALID-12345")))
                .andExpect(jsonPath("$.verificationStatus", is("PENDING")))
                .andExpect(jsonPath("$.available", is(false)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                // Verify no credential or internal fields exposed
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist());
    }

    @Test
    void testRegisterDriverAtBasePathSuccess() throws Exception {
        User user = userRepository.save(new User("Base Path Driver", "basepath.driver@example.com",
                "+1234567891", passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(user);

        DriverRegistrationRequest request = new DriverRegistrationRequest("DL-BASE-54321");

        mockMvc.perform(post("/api/v1/drivers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licenseNumber", is("DL-BASE-54321")))
                .andExpect(jsonPath("$.verificationStatus", is("PENDING")));
    }

    @Test
    void testRegisterDriverUnauthenticatedReturns401() throws Exception {
        DriverRegistrationRequest request = new DriverRegistrationRequest("DL-VALID-12345");

        mockMvc.perform(post("/api/v1/drivers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testRegisterDriverInvalidTokenReturns401() throws Exception {
        DriverRegistrationRequest request = new DriverRegistrationRequest("DL-VALID-12345");

        mockMvc.perform(post("/api/v1/drivers/register")
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testRegisterDriverBlankLicenseReturns400() throws Exception {
        User user = userRepository.save(new User("Test User", "blank.license@example.com",
                null, passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(user);

        DriverRegistrationRequest request = new DriverRegistrationRequest("   ");

        mockMvc.perform(post("/api/v1/drivers/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.licenseNumber", notNullValue()));
    }

    @Test
    void testRegisterDriverTooShortLicenseReturns400() throws Exception {
        User user = userRepository.save(new User("Test User", "short.license@example.com",
                null, passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(user);

        DriverRegistrationRequest request = new DriverRegistrationRequest("DL");

        mockMvc.perform(post("/api/v1/drivers/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.licenseNumber", notNullValue()));
    }

    @Test
    void testRegisterDriverDuplicateLicenseNumberReturns409() throws Exception {
        User user1 = userRepository.save(new User("First Driver", "first.driver@example.com",
                null, passwordEncoder.encode("secret123"), Role.CUSTOMER));
        User user2 = userRepository.save(new User("Second Driver", "second.driver@example.com",
                null, passwordEncoder.encode("secret123"), Role.CUSTOMER));

        String token1 = jwtTokenProvider.generateToken(user1);
        String token2 = jwtTokenProvider.generateToken(user2);

        DriverRegistrationRequest request = new DriverRegistrationRequest("DL-DUPLICATE-999");

        // First registration succeeds
        mockMvc.perform(post("/api/v1/drivers/register")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second registration with same license returns 409
        mockMvc.perform(post("/api/v1/drivers/register")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Conflict")))
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void testRegisterDriverDuplicateProfileForUserReturns409() throws Exception {
        User user = userRepository.save(new User("One Profile User", "one.profile@example.com",
                null, passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(user);

        DriverRegistrationRequest request1 = new DriverRegistrationRequest("DL-FIRST-111");
        DriverRegistrationRequest request2 = new DriverRegistrationRequest("DL-SECOND-222");

        // First registration succeeds
        mockMvc.perform(post("/api/v1/drivers/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Second registration by same user returns 409
        mockMvc.perform(post("/api/v1/drivers/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Conflict")))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.detail", is("Driver profile already exists for this user")));
    }

    @Test
    void testGetMyDriverProfileSuccess() throws Exception {
        User user = userRepository.save(new User("Active Driver", "active.driver@example.com",
                "+1234567892", passwordEncoder.encode("secret123"), Role.DRIVER));
        Driver driver = driverRepository.save(new Driver(user, "DL-MY-PROFILE-123"));
        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(get("/api/v1/drivers/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(driver.getId().toString())))
                .andExpect(jsonPath("$.userId", is(user.getId().toString())))
                .andExpect(jsonPath("$.licenseNumber", is("DL-MY-PROFILE-123")))
                .andExpect(jsonPath("$.verificationStatus", is("PENDING")))
                .andExpect(jsonPath("$.available", is(false)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist());
    }

    @Test
    void testGetMyDriverProfileUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/drivers/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testGetMyDriverProfileNotFoundReturns404() throws Exception {
        User user = userRepository.save(new User("Non Driver User", "nondriver@example.com",
                null, passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(get("/api/v1/drivers/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void testUpdateAvailabilityApprovedDriverCanBecomeAvailable() throws Exception {
        User user = userRepository.save(new User("Approved Driver 1", "approved1@example.com",
                "+1234567801", passwordEncoder.encode("secret123"), Role.DRIVER));
        Driver driver = driverRepository.save(new Driver(user, "DL-APPR-001", VerificationStatus.APPROVED, false));
        String token = jwtTokenProvider.generateToken(user);

        DriverAvailabilityRequest request = new DriverAvailabilityRequest(true);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(driver.getId().toString())))
                .andExpect(jsonPath("$.userId", is(user.getId().toString())))
                .andExpect(jsonPath("$.licenseNumber", is("DL-APPR-001")))
                .andExpect(jsonPath("$.verificationStatus", is("APPROVED")))
                .andExpect(jsonPath("$.available", is(true)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist());

        assertTrue(driverRepository.findById(driver.getId()).orElseThrow().isAvailable(),
                "Driver should be marked available in repository");
    }

    @Test
    void testUpdateAvailabilityApprovedDriverCanBecomeUnavailable() throws Exception {
        User user = userRepository.save(new User("Approved Driver 2", "approved2@example.com",
                "+1234567802", passwordEncoder.encode("secret123"), Role.DRIVER));
        Driver driver = driverRepository.save(new Driver(user, "DL-APPR-002", VerificationStatus.APPROVED, true));
        String token = jwtTokenProvider.generateToken(user);

        DriverAvailabilityRequest request = new DriverAvailabilityRequest(false);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(driver.getId().toString())))
                .andExpect(jsonPath("$.verificationStatus", is("APPROVED")))
                .andExpect(jsonPath("$.available", is(false)));

        assertFalse(driverRepository.findById(driver.getId()).orElseThrow().isAvailable(),
                "Driver should be marked unavailable in repository");
    }

    @Test
    void testUpdateAvailabilityPendingDriverCannotBecomeAvailable() throws Exception {
        User user = userRepository.save(new User("Pending Driver 1", "pending1@example.com",
                "+1234567803", passwordEncoder.encode("secret123"), Role.DRIVER));
        Driver driver = driverRepository.save(new Driver(user, "DL-PEND-001", VerificationStatus.PENDING, false));
        String token = jwtTokenProvider.generateToken(user);

        DriverAvailabilityRequest request = new DriverAvailabilityRequest(true);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", is("Driver must have APPROVED verification status to become available")));

        assertFalse(driverRepository.findById(driver.getId()).orElseThrow().isAvailable(),
                "Pending driver must remain unavailable in repository");
    }

    @Test
    void testUpdateAvailabilityRejectedDriverCannotBecomeAvailable() throws Exception {
        User user = userRepository.save(new User("Rejected Driver 1", "rejected1@example.com",
                "+1234567804", passwordEncoder.encode("secret123"), Role.DRIVER));
        Driver driver = driverRepository.save(new Driver(user, "DL-REJ-001", VerificationStatus.REJECTED, false));
        String token = jwtTokenProvider.generateToken(user);

        DriverAvailabilityRequest request = new DriverAvailabilityRequest(true);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", is("Driver must have APPROVED verification status to become available")));

        assertFalse(driverRepository.findById(driver.getId()).orElseThrow().isAvailable(),
                "Rejected driver must remain unavailable in repository");
    }

    @Test
    void testUpdateAvailabilityPendingDriverCanBecomeUnavailable() throws Exception {
        User user = userRepository.save(new User("Pending Driver 2", "pending2@example.com",
                "+1234567805", passwordEncoder.encode("secret123"), Role.DRIVER));
        Driver driver = driverRepository.save(new Driver(user, "DL-PEND-002", VerificationStatus.PENDING, false));
        String token = jwtTokenProvider.generateToken(user);

        DriverAvailabilityRequest request = new DriverAvailabilityRequest(false);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(driver.getId().toString())))
                .andExpect(jsonPath("$.verificationStatus", is("PENDING")))
                .andExpect(jsonPath("$.available", is(false)));

        assertFalse(driverRepository.findById(driver.getId()).orElseThrow().isAvailable(),
                "Pending driver should be confirmed unavailable");
    }

    @Test
    void testUpdateAvailabilityRejectedDriverCanBecomeUnavailable() throws Exception {
        User user = userRepository.save(new User("Rejected Driver 2", "rejected2@example.com",
                "+1234567806", passwordEncoder.encode("secret123"), Role.DRIVER));
        Driver driver = driverRepository.save(new Driver(user, "DL-REJ-002", VerificationStatus.REJECTED, false));
        String token = jwtTokenProvider.generateToken(user);

        DriverAvailabilityRequest request = new DriverAvailabilityRequest(false);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(driver.getId().toString())))
                .andExpect(jsonPath("$.verificationStatus", is("REJECTED")))
                .andExpect(jsonPath("$.available", is(false)));

        assertFalse(driverRepository.findById(driver.getId()).orElseThrow().isAvailable(),
                "Rejected driver should be confirmed unavailable");
    }

    @Test
    void testUpdateAvailabilityUnauthenticatedReturns401() throws Exception {
        DriverAvailabilityRequest request = new DriverAvailabilityRequest(true);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testUpdateAvailabilityInvalidTokenReturns401() throws Exception {
        DriverAvailabilityRequest request = new DriverAvailabilityRequest(true);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testUpdateAvailabilityWithoutDriverProfileReturns404() throws Exception {
        User user = userRepository.save(new User("No Driver Profile", "nodriver@example.com",
                null, passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(user);

        DriverAvailabilityRequest request = new DriverAvailabilityRequest(false);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void testUpdateAvailabilityMissingValueReturns400() throws Exception {
        User user = userRepository.save(new User("Missing Val Driver", "missingval@example.com",
                null, passwordEncoder.encode("secret123"), Role.DRIVER));
        driverRepository.save(new Driver(user, "DL-MISS-001", VerificationStatus.APPROVED, false));
        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.available", notNullValue()));
    }

    @Test
    void testUpdateAvailabilityNullValueReturns400() throws Exception {
        User user = userRepository.save(new User("Null Val Driver", "nullval@example.com",
                null, passwordEncoder.encode("secret123"), Role.DRIVER));
        driverRepository.save(new Driver(user, "DL-NULL-001", VerificationStatus.APPROVED, false));
        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.available", notNullValue()));
    }

    @Test
    void testUpdateAvailabilityInvalidTypeReturns400() throws Exception {
        User user = userRepository.save(new User("Invalid Type Driver", "invalidtype@example.com",
                null, passwordEncoder.encode("secret123"), Role.DRIVER));
        driverRepository.save(new Driver(user, "DL-INV-001", VerificationStatus.APPROVED, false));
        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\": \"not-a-boolean\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void testUpdateAvailabilityEmptyBodyReturns400() throws Exception {
        User user = userRepository.save(new User("Empty Body Driver", "emptybody@example.com",
                null, passwordEncoder.encode("secret123"), Role.DRIVER));
        driverRepository.save(new Driver(user, "DL-EMPTY-001", VerificationStatus.APPROVED, false));
        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)));
    }

    // ==================== TASK 3F: VEHICLE DETAILS TESTS ====================

    @Test
    void testRegisterVehicleSuccess() throws Exception {
        User user = userRepository.save(new User("Vehicle Driver", "vehicle.driver@example.com",
                "+1234567890", passwordEncoder.encode("secret123"), Role.DRIVER));
        Driver driver = driverRepository.save(new Driver(user, "DL-VEH-01"));
        String token = jwtTokenProvider.generateToken(user);

        VehicleRegistrationRequest request = new VehicleRegistrationRequest(
                VehicleType.SEDAN, "Toyota", "Camry", "KA-01-AB-1234", "Silver"
        );

        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.driverId", is(driver.getId().toString())))
                .andExpect(jsonPath("$.vehicleType", is("SEDAN")))
                .andExpect(jsonPath("$.make", is("Toyota")))
                .andExpect(jsonPath("$.model", is("Camry")))
                .andExpect(jsonPath("$.registrationNumber", is("KA-01-AB-1234")))
                .andExpect(jsonPath("$.color", is("Silver")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));
    }

    @Test
    void testRegisterVehicleDuplicateRegistrationNumberReturns409() throws Exception {
        User user1 = userRepository.save(new User("Vehicle Driver 1", "veh.driver1@example.com",
                "+1234567891", passwordEncoder.encode("secret123"), Role.DRIVER));
        driverRepository.save(new Driver(user1, "DL-VEH-02"));
        String token1 = jwtTokenProvider.generateToken(user1);

        VehicleRegistrationRequest req1 = new VehicleRegistrationRequest(
                VehicleType.SEDAN, "Toyota", "Camry", "KA-01-DUP-9999", "Silver"
        );
        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        User user2 = userRepository.save(new User("Vehicle Driver 2", "veh.driver2@example.com",
                "+1234567892", passwordEncoder.encode("secret123"), Role.DRIVER));
        driverRepository.save(new Driver(user2, "DL-VEH-03"));
        String token2 = jwtTokenProvider.generateToken(user2);

        VehicleRegistrationRequest req2 = new VehicleRegistrationRequest(
                VehicleType.SUV, "Honda", "CR-V", "ka-01-dup-9999", "Blue"
        );
        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.detail", notNullValue()));
    }

    @Test
    void testRegisterVehicleAlreadyRegisteredForDriverReturns409() throws Exception {
        User user = userRepository.save(new User("Double Vehicle Driver", "double.veh@example.com",
                "+1234567893", passwordEncoder.encode("secret123"), Role.DRIVER));
        driverRepository.save(new Driver(user, "DL-VEH-04"));
        String token = jwtTokenProvider.generateToken(user);

        VehicleRegistrationRequest req1 = new VehicleRegistrationRequest(
                VehicleType.HATCHBACK, "Hyundai", "i20", "KA-01-FIRST", "Red"
        );
        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        VehicleRegistrationRequest req2 = new VehicleRegistrationRequest(
                VehicleType.SEDAN, "Honda", "City", "KA-01-SECOND", "White"
        );
        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void testRegisterVehicleUnauthenticatedReturns401() throws Exception {
        VehicleRegistrationRequest request = new VehicleRegistrationRequest(
                VehicleType.SEDAN, "Toyota", "Corolla", "KA-01-UNAUTH", "Black"
        );

        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testRegisterVehicleWithoutDriverProfileReturns404() throws Exception {
        User user = userRepository.save(new User("Non Driver User", "non.driver@example.com",
                "+1234567894", passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(user);

        VehicleRegistrationRequest request = new VehicleRegistrationRequest(
                VehicleType.SEDAN, "Toyota", "Corolla", "KA-01-NODRIVER", "Black"
        );

        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void testRegisterVehicleValidationFailuresReturn400() throws Exception {
        User user = userRepository.save(new User("Val Driver", "val.driver@example.com",
                "+1234567895", passwordEncoder.encode("secret123"), Role.DRIVER));
        driverRepository.save(new Driver(user, "DL-VAL-01"));
        String token = jwtTokenProvider.generateToken(user);

        // Missing make
        VehicleRegistrationRequest invalidMake = new VehicleRegistrationRequest(
                VehicleType.SEDAN, "", "Camry", "KA-01-TEST-1", "Red"
        );
        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidMake)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.make", notNullValue()));

        // Missing model
        VehicleRegistrationRequest invalidModel = new VehicleRegistrationRequest(
                VehicleType.SEDAN, "Toyota", "   ", "KA-01-TEST-2", "Red"
        );
        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidModel)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.model", notNullValue()));

        // Short registration number (< 3 chars)
        VehicleRegistrationRequest shortReg = new VehicleRegistrationRequest(
                VehicleType.SEDAN, "Toyota", "Camry", "K", "Red"
        );
        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortReg)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.registrationNumber", notNullValue()));

        // Missing color
        VehicleRegistrationRequest invalidColor = new VehicleRegistrationRequest(
                VehicleType.SEDAN, "Toyota", "Camry", "KA-01-TEST-3", ""
        );
        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidColor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.color", notNullValue()));
    }

    @Test
    void testGetVehicleSuccess() throws Exception {
        User user = userRepository.save(new User("Get Veh Driver", "get.veh@example.com",
                "+1234567896", passwordEncoder.encode("secret123"), Role.DRIVER));
        driverRepository.save(new Driver(user, "DL-GET-01"));
        String token = jwtTokenProvider.generateToken(user);

        VehicleRegistrationRequest regReq = new VehicleRegistrationRequest(
                VehicleType.SUV, "Ford", "EcoSport", "KA-04-GET-1234", "Blue"
        );
        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleType", is("SUV")))
                .andExpect(jsonPath("$.make", is("Ford")))
                .andExpect(jsonPath("$.model", is("EcoSport")))
                .andExpect(jsonPath("$.registrationNumber", is("KA-04-GET-1234")))
                .andExpect(jsonPath("$.color", is("Blue")));
    }

    @Test
    void testGetVehicleNotFoundReturns404() throws Exception {
        User user = userRepository.save(new User("No Veh Driver", "no.veh@example.com",
                "+1234567897", passwordEncoder.encode("secret123"), Role.DRIVER));
        driverRepository.save(new Driver(user, "DL-NO-VEH"));
        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(get("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void testGetVehicleUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/drivers/me/vehicle"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testUpdateVehicleSuccess() throws Exception {
        User user = userRepository.save(new User("Update Veh Driver", "update.veh@example.com",
                "+1234567898", passwordEncoder.encode("secret123"), Role.DRIVER));
        driverRepository.save(new Driver(user, "DL-UPD-01"));
        String token = jwtTokenProvider.generateToken(user);

        // Register initial vehicle
        VehicleRegistrationRequest initialReq = new VehicleRegistrationRequest(
                VehicleType.SEDAN, "Maruti", "Ciaz", "KA-02-UPD-1111", "White"
        );
        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialReq)))
                .andExpect(status().isCreated());

        // Update vehicle with changed make, model, color, vehicleType and same registration number
        VehicleRegistrationRequest updateReq = new VehicleRegistrationRequest(
                VehicleType.SUV, "Tata", "Harrier", "KA-02-UPD-1111", "Grey"
        );
        mockMvc.perform(put("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleType", is("SUV")))
                .andExpect(jsonPath("$.make", is("Tata")))
                .andExpect(jsonPath("$.model", is("Harrier")))
                .andExpect(jsonPath("$.registrationNumber", is("KA-02-UPD-1111")))
                .andExpect(jsonPath("$.color", is("Grey")));
    }

    @Test
    void testUpdateVehicleDuplicateRegistrationNumberReturns409() throws Exception {
        User user1 = userRepository.save(new User("Driver 1 Upd", "d1.upd@example.com",
                "+1234567801", passwordEncoder.encode("secret123"), Role.DRIVER));
        driverRepository.save(new Driver(user1, "DL-UPD-02"));
        String token1 = jwtTokenProvider.generateToken(user1);

        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VehicleRegistrationRequest(
                                VehicleType.SEDAN, "Toyota", "Camry", "KA-01-TAKEN", "Black"
                        ))))
                .andExpect(status().isCreated());

        User user2 = userRepository.save(new User("Driver 2 Upd", "d2.upd@example.com",
                "+1234567802", passwordEncoder.encode("secret123"), Role.DRIVER));
        driverRepository.save(new Driver(user2, "DL-UPD-03"));
        String token2 = jwtTokenProvider.generateToken(user2);

        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VehicleRegistrationRequest(
                                VehicleType.SEDAN, "Honda", "City", "KA-01-FREE", "White"
                        ))))
                .andExpect(status().isCreated());

        // Driver 2 tries to update their registration number to driver 1's registration number
        VehicleRegistrationRequest updateReq = new VehicleRegistrationRequest(
                VehicleType.SEDAN, "Honda", "City", "ka-01-taken", "White"
        );
        mockMvc.perform(put("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void testDriverProfileReflectsVehicleWhenRegistered() throws Exception {
        User user = userRepository.save(new User("Profile Veh Driver", "profile.veh@example.com",
                "+1234567803", passwordEncoder.encode("secret123"), Role.DRIVER));
        driverRepository.save(new Driver(user, "DL-PROF-01"));
        String token = jwtTokenProvider.generateToken(user);

        // Before vehicle registration, vehicle is null
        mockMvc.perform(get("/api/v1/drivers/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicle").doesNotExist());

        // Register vehicle
        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VehicleRegistrationRequest(
                                VehicleType.SEDAN, "Honda", "Amaze", "KA-05-PROF-111", "Red"
                        ))))
                .andExpect(status().isCreated());

        // After vehicle registration, vehicle is present in profile
        mockMvc.perform(get("/api/v1/drivers/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicle", notNullValue()))
                .andExpect(jsonPath("$.vehicle.registrationNumber", is("KA-05-PROF-111")))
                .andExpect(jsonPath("$.vehicle.make", is("Honda")));
    }
}


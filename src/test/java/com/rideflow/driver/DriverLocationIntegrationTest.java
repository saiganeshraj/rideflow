package com.rideflow.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DriverLocationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private DriverLocationStore driverLocationStore;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        driverLocationStore.clear();
        driverRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createDriverUser(String email) {
        return userRepository.save(new User("Test Driver", email, "+1912000001",
                passwordEncoder.encode("secret123"), Role.DRIVER));
    }

    private User createCustomerUser(String email) {
        return userRepository.save(new User("Test Customer", email, "+1912000002",
                passwordEncoder.encode("secret123"), Role.CUSTOMER));
    }

    private Driver createDriverWithStatus(User user, VerificationStatus status) {
        return driverRepository.save(new Driver(user, "DL-LOC-" + UUID.randomUUID().toString().substring(0, 8), status, false));
    }

    @Test
    void testUpdateAndGetLocationSuccess() throws Exception {
        User user = createDriverUser("loc.driver@example.com");
        Driver driver = createDriverWithStatus(user, VerificationStatus.APPROVED);
        String token = jwtTokenProvider.generateToken(user);

        DriverLocationUpdateRequest request = new DriverLocationUpdateRequest(12.9716, 77.5946, 180.0, 42.5);

        // 1. Update location
        mockMvc.perform(put("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId", is(driver.getId().toString())))
                .andExpect(jsonPath("$.latitude", is(12.9716)))
                .andExpect(jsonPath("$.longitude", is(77.5946)))
                .andExpect(jsonPath("$.heading", is(180.0)))
                .andExpect(jsonPath("$.speed", is(42.5)))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        // 2. Retrieve location
        mockMvc.perform(get("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId", is(driver.getId().toString())))
                .andExpect(jsonPath("$.latitude", is(12.9716)))
                .andExpect(jsonPath("$.longitude", is(77.5946)))
                .andExpect(jsonPath("$.heading", is(180.0)))
                .andExpect(jsonPath("$.speed", is(42.5)))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));
    }

    @Test
    void testUpdateLocationMinimalFieldsSuccess() throws Exception {
        User user = createDriverUser("loc.min@example.com");
        Driver driver = createDriverWithStatus(user, VerificationStatus.APPROVED);
        String token = jwtTokenProvider.generateToken(user);

        DriverLocationUpdateRequest request = new DriverLocationUpdateRequest(37.7749, -122.4194);

        mockMvc.perform(put("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId", is(driver.getId().toString())))
                .andExpect(jsonPath("$.latitude", is(37.7749)))
                .andExpect(jsonPath("$.longitude", is(-122.4194)))
                .andExpect(jsonPath("$.heading", nullValue()))
                .andExpect(jsonPath("$.speed", nullValue()));
    }

    @Test
    void testUpdateLocationInvalidLatitudeReturns400() throws Exception {
        User user = createDriverUser("loc.invlat@example.com");
        createDriverWithStatus(user, VerificationStatus.APPROVED);
        String token = jwtTokenProvider.generateToken(user);

        // Latitude > 90
        DriverLocationUpdateRequest highLat = new DriverLocationUpdateRequest(95.0, 77.5946);
        mockMvc.perform(put("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(highLat)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        // Latitude < -90
        DriverLocationUpdateRequest lowLat = new DriverLocationUpdateRequest(-95.0, 77.5946);
        mockMvc.perform(put("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowLat)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }

    @Test
    void testUpdateLocationInvalidLongitudeReturns400() throws Exception {
        User user = createDriverUser("loc.invlon@example.com");
        createDriverWithStatus(user, VerificationStatus.APPROVED);
        String token = jwtTokenProvider.generateToken(user);

        // Longitude > 180
        DriverLocationUpdateRequest highLon = new DriverLocationUpdateRequest(12.9716, 185.0);
        mockMvc.perform(put("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(highLon)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        // Longitude < -180
        DriverLocationUpdateRequest lowLon = new DriverLocationUpdateRequest(12.9716, -185.0);
        mockMvc.perform(put("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowLon)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }

    @Test
    void testUpdateLocationInvalidHeadingAndSpeedReturns400() throws Exception {
        User user = createDriverUser("loc.invhead@example.com");
        createDriverWithStatus(user, VerificationStatus.APPROVED);
        String token = jwtTokenProvider.generateToken(user);

        // Heading > 360
        DriverLocationUpdateRequest badHeading = new DriverLocationUpdateRequest(12.9716, 77.5946, 365.0, 30.0);
        mockMvc.perform(put("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badHeading)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        // Speed < 0
        DriverLocationUpdateRequest badSpeed = new DriverLocationUpdateRequest(12.9716, 77.5946, 90.0, -10.0);
        mockMvc.perform(put("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badSpeed)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }

    @Test
    void testUpdateLocationMissingCoordinatesReturns400() throws Exception {
        User user = createDriverUser("loc.missing@example.com");
        createDriverWithStatus(user, VerificationStatus.APPROVED);
        String token = jwtTokenProvider.generateToken(user);

        DriverLocationUpdateRequest missing = new DriverLocationUpdateRequest(null, 77.5946);
        mockMvc.perform(put("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missing)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }

    @Test
    void testUpdateLocationUnapprovedDriverReturns400() throws Exception {
        // PENDING driver
        User pendingUser = createDriverUser("loc.pending@example.com");
        createDriverWithStatus(pendingUser, VerificationStatus.PENDING);
        String pendingToken = jwtTokenProvider.generateToken(pendingUser);

        DriverLocationUpdateRequest request = new DriverLocationUpdateRequest(12.9716, 77.5946);
        mockMvc.perform(put("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + pendingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.detail", is("Driver must have APPROVED verification status to update location")));

        // REJECTED driver
        User rejectedUser = createDriverUser("loc.rejected@example.com");
        createDriverWithStatus(rejectedUser, VerificationStatus.REJECTED);
        String rejectedToken = jwtTokenProvider.generateToken(rejectedUser);

        mockMvc.perform(put("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + rejectedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.detail", is("Driver must have APPROVED verification status to update location")));
    }

    @Test
    void testUpdateLocationMissingDriverProfileReturns404() throws Exception {
        // User has DRIVER role but no Driver record exists
        User driverWithoutProfile = createDriverUser("loc.noprofile@example.com");
        String token = jwtTokenProvider.generateToken(driverWithoutProfile);

        DriverLocationUpdateRequest request = new DriverLocationUpdateRequest(12.9716, 77.5946);
        mockMvc.perform(put("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void testUpdateLocationUnauthorizedRoleReturns403() throws Exception {
        User customer = createCustomerUser("loc.cust@example.com");
        String customerToken = jwtTokenProvider.generateToken(customer);

        DriverLocationUpdateRequest request = new DriverLocationUpdateRequest(12.9716, 77.5946);
        mockMvc.perform(put("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    void testUpdateLocationUnauthenticatedReturns401() throws Exception {
        DriverLocationUpdateRequest request = new DriverLocationUpdateRequest(12.9716, 77.5946);
        mockMvc.perform(put("/api/v1/drivers/me/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testGetLocationWhenNotReportedReturns404() throws Exception {
        User user = createDriverUser("loc.none@example.com");
        createDriverWithStatus(user, VerificationStatus.APPROVED);
        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(get("/api/v1/drivers/me/location")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void testStoreSaveGetRemoveClearDirectly() {
        UUID driverId = UUID.randomUUID();
        DriverLocation loc = new DriverLocation(driverId, 12.9716, 77.5946, 90.0, 30.0, null);

        driverLocationStore.saveLocation(loc);
        Optional<DriverLocation> retrieved = driverLocationStore.getLocation(driverId);
        assertTrue(retrieved.isPresent());
        assertEquals(12.9716, retrieved.get().getLatitude());
        assertEquals(77.5946, retrieved.get().getLongitude());

        driverLocationStore.removeLocation(driverId);
        assertFalse(driverLocationStore.getLocation(driverId).isPresent());

        driverLocationStore.saveLocation(loc);
        driverLocationStore.clear();
        assertFalse(driverLocationStore.getLocation(driverId).isPresent());
    }
}

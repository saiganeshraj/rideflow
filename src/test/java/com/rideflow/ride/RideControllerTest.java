package com.rideflow.ride;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.fare.FareEstimateRequest;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testEstimateFareSuccess() throws Exception {
        User user = userRepository.save(new User("Estimate Rider", "est.rider@example.com",
                "+1987650001", passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(user);

        FareEstimateRequest request = new FareEstimateRequest(12.9716, 77.5946, 12.9352, 77.6245);

        mockMvc.perform(post("/api/v1/rides/fare-estimate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceKm", greaterThan(5.0)))
                .andExpect(jsonPath("$.estimatedFare", notNullValue()))
                .andExpect(jsonPath("$.currency", is("INR")));
    }

    @Test
    void testEstimateFareUnauthenticatedReturns401() throws Exception {
        FareEstimateRequest request = new FareEstimateRequest(12.9716, 77.5946, 12.9352, 77.6245);

        mockMvc.perform(post("/api/v1/rides/fare-estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testEstimateFareValidationFailuresReturn400() throws Exception {
        User user = userRepository.save(new User("Val Rider", "val.rider@example.com",
                "+1987650002", passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(user);

        // Latitude out of range (> 90)
        FareEstimateRequest invalidLat = new FareEstimateRequest(95.0, 77.5946, 12.9352, 77.6245);
        mockMvc.perform(post("/api/v1/rides/fare-estimate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLat)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pickupLatitude", notNullValue()));

        // Longitude out of range (> 180)
        FareEstimateRequest invalidLon = new FareEstimateRequest(12.9716, 185.0, 12.9352, 77.6245);
        mockMvc.perform(post("/api/v1/rides/fare-estimate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLon)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pickupLongitude", notNullValue()));

        // Missing field
        mockMvc.perform(post("/api/v1/rides/fare-estimate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupLatitude\": 12.9716}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void testRequestRideSuccess() throws Exception {
        User customer = userRepository.save(new User("Ride Customer", "ride.customer@example.com",
                "+1987650003", passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(customer);

        RideCreateRequest request = new RideCreateRequest(
                12.9716, 77.5946,
                12.9352, 77.6245,
                "MG Road, Bangalore",
                "Koramangala, Bangalore"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/rides")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.customerId", is(customer.getId().toString())))
                .andExpect(jsonPath("$.driverId", nullValue()))
                .andExpect(jsonPath("$.status", is("REQUESTED")))
                .andExpect(jsonPath("$.pickupLatitude", is(12.9716)))
                .andExpect(jsonPath("$.pickupLongitude", is(77.5946)))
                .andExpect(jsonPath("$.dropoffLatitude", is(12.9352)))
                .andExpect(jsonPath("$.dropoffLongitude", is(77.6245)))
                .andExpect(jsonPath("$.pickupAddress", is("MG Road, Bangalore")))
                .andExpect(jsonPath("$.dropoffAddress", is("Koramangala, Bangalore")))
                .andExpect(jsonPath("$.estimatedFare", notNullValue()))
                .andExpect(jsonPath("$.currency", is("INR")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                .andReturn();

        // Verify database persistence
        String responseBody = result.getResponse().getContentAsString();
        RideResponse rideResponse = objectMapper.readValue(responseBody, RideResponse.class);
        UUID createdRideId = rideResponse.getId();

        Optional<Ride> persisted = rideRepository.findById(createdRideId);
        assertTrue(persisted.isPresent());
        Ride ride = persisted.get();
        assertEquals(customer.getId(), ride.getCustomer().getId());
        assertNull(ride.getDriver());
        assertEquals(RideStatus.REQUESTED, ride.getStatus());
        assertEquals("INR", ride.getCurrency());
        assertEquals("MG Road, Bangalore", ride.getPickupAddress());
    }

    @Test
    void testRequestRideUnauthenticatedReturns401() throws Exception {
        RideCreateRequest request = new RideCreateRequest(
                12.9716, 77.5946,
                12.9352, 77.6245,
                "MG Road, Bangalore",
                "Koramangala, Bangalore"
        );

        mockMvc.perform(post("/api/v1/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testRequestRideValidationFailuresReturn400() throws Exception {
        User customer = userRepository.save(new User("Val Fail Customer", "valfail@example.com",
                "+1987650004", passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(customer);

        // Blank pickup address
        RideCreateRequest blankPickup = new RideCreateRequest(
                12.9716, 77.5946, 12.9352, 77.6245,
                "  ", "Koramangala, Bangalore"
        );
        mockMvc.perform(post("/api/v1/rides")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankPickup)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pickupAddress", notNullValue()));

        // Blank dropoff address
        RideCreateRequest blankDropoff = new RideCreateRequest(
                12.9716, 77.5946, 12.9352, 77.6245,
                "MG Road, Bangalore", ""
        );
        mockMvc.perform(post("/api/v1/rides")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankDropoff)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dropoffAddress", notNullValue()));

        // Invalid latitude
        RideCreateRequest invalidLat = new RideCreateRequest(
                -95.0, 77.5946, 12.9352, 77.6245,
                "MG Road, Bangalore", "Koramangala, Bangalore"
        );
        mockMvc.perform(post("/api/v1/rides")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLat)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pickupLatitude", notNullValue()));
    }

    @Test
    void testRequestRideIgnoresSubmittedCurrencyAndUsesInr() throws Exception {
        User customer = userRepository.save(new User("Currency Customer", "currency.customer@example.com",
                "+1987650005", passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(customer);

        String request = """
                {
                    "pickupLatitude": 37.7749,
                    "pickupLongitude": -122.4194,
                    "dropoffLatitude": 37.7833,
                    "dropoffLongitude": -122.4167,
                    "pickupAddress": "Market St, SF",
                    "dropoffAddress": "Mission St, SF",
                    "currency": "USD"
                }
                """;

        mockMvc.perform(post("/api/v1/rides")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency", is("INR")));
    }

    @Test
    void testEstimateFareIdenticalLocationsReturns400() throws Exception {
        User user = userRepository.save(new User("Same Place Rider", "same.place@example.com",
                "+1987650010", passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(user);

        FareEstimateRequest request = new FareEstimateRequest(12.9716, 77.5946, 12.9716, 77.5946);

        mockMvc.perform(post("/api/v1/rides/fare-estimate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Pickup and dropoff coordinates must be different")));
    }

    @Test
    void testRequestRideIdenticalLocationsReturns400() throws Exception {
        User customer = userRepository.save(new User("Same Ride Customer", "same.ride@example.com",
                "+1987650011", passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(customer);

        RideCreateRequest request = new RideCreateRequest(
                12.9716, 77.5946,
                12.9716, 77.5946,
                "MG Road, Bangalore",
                "MG Road, Bangalore"
        );

        mockMvc.perform(post("/api/v1/rides")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Pickup and dropoff coordinates must be different")));
    }

    @Test
    void testRequestRideCustomerIdentityCannotBeSpoofed() throws Exception {
        User authenticUser = userRepository.save(new User("Real Customer", "real.customer@example.com",
                "+1987650006", passwordEncoder.encode("secret123"), Role.CUSTOMER));
        User victimUser = userRepository.save(new User("Victim Customer", "victim.customer@example.com",
                "+1987650007", passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(authenticUser);

        // JSON payload attempt to send victim's ID
        String spoofPayload = String.format("""
                {
                    "pickupLatitude": 12.9716,
                    "pickupLongitude": 77.5946,
                    "dropoffLatitude": 12.9352,
                    "dropoffLongitude": 77.6245,
                    "pickupAddress": "MG Road",
                    "dropoffAddress": "Koramangala",
                    "customerId": "%s"
                }
                """, victimUser.getId());

        MvcResult result = mockMvc.perform(post("/api/v1/rides")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spoofPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId", is(authenticUser.getId().toString())))
                .andReturn();

        RideResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), RideResponse.class);
        assertEquals(authenticUser.getId(), response.getCustomerId(),
                "Customer identity must be derived exclusively from security context");
    }

    @Test
    void testUnsupportedHttpMethodReturns405MethodNotAllowed() throws Exception {
        User user = userRepository.save(new User("Method User", "method.user@example.com",
                "+1987650008", passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/rides")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.title", is("Method Not Allowed")))
                .andExpect(jsonPath("$.status", is(405)));
    }

    @Test
    void testFareEstimateMissingCoordinatesReturn400() throws Exception {
        User user = userRepository.save(new User("Coord User", "coord.user@example.com",
                "+1987650009", passwordEncoder.encode("secret123"), Role.CUSTOMER));
        String token = jwtTokenProvider.generateToken(user);

        // Missing dropoffLongitude
        String payload = """
                {
                    "pickupLatitude": 12.9716,
                    "pickupLongitude": 77.5946,
                    "dropoffLatitude": 12.9352
                }
                """;

        mockMvc.perform(post("/api/v1/rides/fare-estimate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)));
    }
}

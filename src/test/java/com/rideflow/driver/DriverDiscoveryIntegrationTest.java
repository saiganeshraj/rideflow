package com.rideflow.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.fare.FareService;
import com.rideflow.ride.Ride;
import com.rideflow.ride.RideRepository;
import com.rideflow.ride.RideStatus;
import com.rideflow.security.JwtTokenProvider;
import com.rideflow.user.Role;
import com.rideflow.user.User;
import com.rideflow.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DriverDiscoveryIntegrationTest {

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
    private DriverLocationStore driverLocationStore;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FareService fareService;

    private User customerUser;
    private String customerToken;

    // Reference center: Bangalore Majestic (12.9772, 77.5708)
    private static final double PICKUP_LAT = 12.9772;
    private static final double PICKUP_LON = 77.5708;

    @BeforeEach
    void setUp() {
        customerUser = userRepository.save(new User(
                "Customer Jane",
                "customer." + UUID.randomUUID().toString().substring(0, 8) + "@example.com",
                "+919888800001",
                passwordEncoder.encode("secret123"),
                Role.CUSTOMER
        ));
        customerToken = jwtTokenProvider.generateToken(customerUser);
    }

    @AfterEach
    void tearDown() {
        rideRepository.deleteAll();
        vehicleRepository.deleteAll();
        driverRepository.deleteAll();
        userRepository.deleteAll();
        driverLocationStore.clear();
    }

    private Driver createDriverWithVehicle(String emailPrefix, VerificationStatus status, boolean available, VehicleType vehicleType) {
        User driverUser = userRepository.save(new User(
                "Driver " + emailPrefix,
                emailPrefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@example.com",
                "+9197777" + UUID.randomUUID().toString().substring(0, 5),
                passwordEncoder.encode("secret123"),
                Role.DRIVER
        ));

        Driver driver = driverRepository.save(new Driver(
                driverUser,
                "DL-" + UUID.randomUUID().toString().substring(0, 8),
                status,
                available
        ));

        if (vehicleType != null) {
            vehicleRepository.save(new Vehicle(
                    driver,
                    vehicleType,
                    "Toyota",
                    "Prius",
                    "KA-01-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(),
                    "White"
            ));
        }

        return driver;
    }

    @Test
    void testDistanceOrdering_returnsDriversSortedAscending() throws Exception {
        // Driver A: ~1.1 km away (12.9850, 77.5750)
        Driver driverA = createDriverWithVehicle("close", VerificationStatus.APPROVED, true, VehicleType.SEDAN);
        driverLocationStore.saveLocation(new DriverLocation(
                driverA.getId(), 12.9850, 77.5750, 90.0, 30.0, Instant.now()
        ));

        // Driver B: ~3.2 km away (13.0000, 77.5850)
        Driver driverB = createDriverWithVehicle("far", VerificationStatus.APPROVED, true, VehicleType.SEDAN);
        driverLocationStore.saveLocation(new DriverLocation(
                driverB.getId(), 13.0000, 77.5850, 180.0, 25.0, Instant.now()
        ));

        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", String.valueOf(PICKUP_LAT))
                        .param("longitude", String.valueOf(PICKUP_LON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].driverId", is(driverA.getId().toString())))
                .andExpect(jsonPath("$[1].driverId", is(driverB.getId().toString())))
                .andExpect(jsonPath("$[0].distanceKm").value(org.hamcrest.Matchers.lessThan(
                        1.5
                )))
                .andExpect(jsonPath("$[1].distanceKm").value(org.hamcrest.Matchers.greaterThan(
                        2.5
                )));
    }

    @Test
    void testRadiusFiltering_default5KmAndCustomRadius() throws Exception {
        // Driver A: ~3.2 km away (within 5 km)
        Driver driverA = createDriverWithVehicle("insideDefault", VerificationStatus.APPROVED, true, VehicleType.SEDAN);
        driverLocationStore.saveLocation(new DriverLocation(
                driverA.getId(), 13.0000, 77.5850, 0.0, 20.0, Instant.now()
        ));

        // Driver B: ~7.5 km away (outside 5 km, inside 10 km) (13.0400, 77.5900)
        Driver driverB = createDriverWithVehicle("outsideDefault", VerificationStatus.APPROVED, true, VehicleType.SUV);
        driverLocationStore.saveLocation(new DriverLocation(
                driverB.getId(), 13.0400, 77.5900, 45.0, 40.0, Instant.now()
        ));

        // 1. Default radius (5 km) -> only driverA returned
        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", String.valueOf(PICKUP_LAT))
                        .param("longitude", String.valueOf(PICKUP_LON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].driverId", is(driverA.getId().toString())));

        // 2. Custom radius (10 km) -> both returned
        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", String.valueOf(PICKUP_LAT))
                        .param("longitude", String.valueOf(PICKUP_LON))
                        .param("radiusKm", "10.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void testRadiusValidation_rejectsOutOfBoundsRadius() throws Exception {
        // Radius > 20 km -> 400 Bad Request
        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", String.valueOf(PICKUP_LAT))
                        .param("longitude", String.valueOf(PICKUP_LON))
                        .param("radiusKm", "20.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        // Radius <= 0 -> 400 Bad Request
        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", String.valueOf(PICKUP_LAT))
                        .param("longitude", String.valueOf(PICKUP_LON))
                        .param("radiusKm", "0.0"))
                .andExpect(status().isBadRequest());

        // Max radius 20.0 km allowed -> 200 OK
        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", String.valueOf(PICKUP_LAT))
                        .param("longitude", String.valueOf(PICKUP_LON))
                        .param("radiusKm", "20.0"))
                .andExpect(status().isOk());
    }

    @Test
    void testStaleLocationExcluded_olderThan10Minutes() throws Exception {
        Driver driver = createDriverWithVehicle("stale", VerificationStatus.APPROVED, true, VehicleType.SEDAN);

        // Location updated 12 minutes ago -> stale
        Instant twelveMinutesAgo = Instant.now().minus(Duration.ofMinutes(12));
        driverLocationStore.saveLocation(new DriverLocation(
                driver.getId(), 12.9800, 77.5720, 0.0, 10.0, twelveMinutesAgo
        ));

        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", String.valueOf(PICKUP_LAT))
                        .param("longitude", String.valueOf(PICKUP_LON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Now update location to 2 minutes ago -> fresh -> should be returned
        Instant twoMinutesAgo = Instant.now().minus(Duration.ofMinutes(2));
        driverLocationStore.saveLocation(new DriverLocation(
                driver.getId(), 12.9800, 77.5720, 0.0, 10.0, twoMinutesAgo
        ));

        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", String.valueOf(PICKUP_LAT))
                        .param("longitude", String.valueOf(PICKUP_LON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].driverId", is(driver.getId().toString())));
    }

    @Test
    void testUnavailableDriverExcluded() throws Exception {
        Driver driver = createDriverWithVehicle("unavail", VerificationStatus.APPROVED, false, VehicleType.SEDAN);
        driverLocationStore.saveLocation(new DriverLocation(
                driver.getId(), 12.9800, 77.5720, 0.0, 10.0, Instant.now()
        ));

        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", String.valueOf(PICKUP_LAT))
                        .param("longitude", String.valueOf(PICKUP_LON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testUnapprovedDriverExcluded() throws Exception {
        Driver pendingDriver = createDriverWithVehicle("pending", VerificationStatus.PENDING, true, VehicleType.SEDAN);
        driverLocationStore.saveLocation(new DriverLocation(
                pendingDriver.getId(), 12.9800, 77.5720, 0.0, 10.0, Instant.now()
        ));

        Driver rejectedDriver = createDriverWithVehicle("rejected", VerificationStatus.REJECTED, true, VehicleType.SEDAN);
        driverLocationStore.saveLocation(new DriverLocation(
                rejectedDriver.getId(), 12.9810, 77.5730, 0.0, 10.0, Instant.now()
        ));

        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", String.valueOf(PICKUP_LAT))
                        .param("longitude", String.valueOf(PICKUP_LON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testDriverMissingVehicleExcluded() throws Exception {
        // vehicleType = null -> driver has no registered vehicle
        Driver driverNoVehicle = createDriverWithVehicle("noVehicle", VerificationStatus.APPROVED, true, null);
        driverLocationStore.saveLocation(new DriverLocation(
                driverNoVehicle.getId(), 12.9800, 77.5720, 0.0, 10.0, Instant.now()
        ));

        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", String.valueOf(PICKUP_LAT))
                        .param("longitude", String.valueOf(PICKUP_LON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testActiveRidesExcluded() throws Exception {
        // Driver 1 with ACCEPTED ride -> excluded
        Driver driverAccepted = createDriverWithVehicle("accepted", VerificationStatus.APPROVED, true, VehicleType.SEDAN);
        driverLocationStore.saveLocation(new DriverLocation(driverAccepted.getId(), 12.9800, 77.5720, 0.0, 10.0, Instant.now()));
        createRideWithStatus(customerUser, driverAccepted, RideStatus.ACCEPTED);

        // Driver 2 with ARRIVED ride -> excluded
        Driver driverArrived = createDriverWithVehicle("arrived", VerificationStatus.APPROVED, true, VehicleType.SEDAN);
        driverLocationStore.saveLocation(new DriverLocation(driverArrived.getId(), 12.9810, 77.5730, 0.0, 10.0, Instant.now()));
        createRideWithStatus(customerUser, driverArrived, RideStatus.ARRIVED);

        // Driver 3 with IN_PROGRESS ride -> excluded
        Driver driverInProgress = createDriverWithVehicle("inprogress", VerificationStatus.APPROVED, true, VehicleType.SEDAN);
        driverLocationStore.saveLocation(new DriverLocation(driverInProgress.getId(), 12.9820, 77.5740, 0.0, 10.0, Instant.now()));
        createRideWithStatus(customerUser, driverInProgress, RideStatus.IN_PROGRESS);

        // Driver 4 with COMPLETED ride -> included!
        Driver driverCompleted = createDriverWithVehicle("completed", VerificationStatus.APPROVED, true, VehicleType.SEDAN);
        driverLocationStore.saveLocation(new DriverLocation(driverCompleted.getId(), 12.9830, 77.5750, 0.0, 10.0, Instant.now()));
        createRideWithStatus(customerUser, driverCompleted, RideStatus.COMPLETED);

        // Driver 5 with CANCELLED ride -> included!
        Driver driverCancelled = createDriverWithVehicle("cancelled", VerificationStatus.APPROVED, true, VehicleType.SEDAN);
        driverLocationStore.saveLocation(new DriverLocation(driverCancelled.getId(), 12.9840, 77.5760, 0.0, 10.0, Instant.now()));
        createRideWithStatus(customerUser, driverCancelled, RideStatus.CANCELLED);

        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", String.valueOf(PICKUP_LAT))
                        .param("longitude", String.valueOf(PICKUP_LON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.driverId == '" + driverCompleted.getId() + "')]").exists())
                .andExpect(jsonPath("$[?(@.driverId == '" + driverCancelled.getId() + "')]").exists());
    }

    @Test
    void testVehicleTypeFilter() throws Exception {
        Driver sedanDriver = createDriverWithVehicle("sedan", VerificationStatus.APPROVED, true, VehicleType.SEDAN);
        driverLocationStore.saveLocation(new DriverLocation(sedanDriver.getId(), 12.9800, 77.5720, 0.0, 10.0, Instant.now()));

        Driver autoDriver = createDriverWithVehicle("auto", VerificationStatus.APPROVED, true, VehicleType.AUTO);
        driverLocationStore.saveLocation(new DriverLocation(autoDriver.getId(), 12.9810, 77.5730, 0.0, 10.0, Instant.now()));

        // 1. Filter by SEDAN -> only sedanDriver
        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", String.valueOf(PICKUP_LAT))
                        .param("longitude", String.valueOf(PICKUP_LON))
                        .param("vehicleType", "SEDAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].driverId", is(sedanDriver.getId().toString())))
                .andExpect(jsonPath("$[0].vehicle.vehicleType", is("SEDAN")));

        // 2. Filter by AUTO -> only autoDriver
        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", String.valueOf(PICKUP_LAT))
                        .param("longitude", String.valueOf(PICKUP_LON))
                        .param("vehicleType", "AUTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].driverId", is(autoDriver.getId().toString())))
                .andExpect(jsonPath("$[0].vehicle.vehicleType", is("AUTO")));

        // 3. Filter by invalid vehicle type -> 400 Bad Request
        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", String.valueOf(PICKUP_LAT))
                        .param("longitude", String.valueOf(PICKUP_LON))
                        .param("vehicleType", "SPACESHIP"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }

    @Test
    void testInvalidCoordinates_returns400() throws Exception {
        // Latitude out of range (> 90)
        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", "95.0")
                        .param("longitude", "77.5946"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        // Longitude out of range (> 180)
        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("latitude", "12.9716")
                        .param("longitude", "185.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));

        // Missing required latitude
        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("longitude", "77.5946"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUnauthenticatedAccess_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/drivers/nearby")
                        .param("latitude", "12.9716")
                        .param("longitude", "77.5946"))
                .andExpect(status().isUnauthorized());
    }

    private Ride createRideWithStatus(User customer, Driver driver, RideStatus status) {
        Ride ride = new Ride(
                customer,
                12.9716, 77.5946,
                12.9352, 77.6245,
                "Pickup Point",
                "Dropoff Point",
                BigDecimal.valueOf(150.00),
                "INR"
        );
        ride.setDriver(driver);
        ride.setStatus(status);
        return rideRepository.save(ride);
    }
}

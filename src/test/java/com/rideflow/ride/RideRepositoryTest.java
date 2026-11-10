package com.rideflow.ride;

import com.rideflow.driver.Driver;
import com.rideflow.driver.DriverRepository;
import com.rideflow.driver.VerificationStatus;
import com.rideflow.user.Role;
import com.rideflow.user.User;
import com.rideflow.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RideRepositoryTest {

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Test
    void testSaveAndFindRideWithoutDriver() {
        User customer = userRepository.save(new User("Alice Rider", "alice.rider@example.com",
                "+1987654321", "hashedpw", Role.CUSTOMER));

        Ride ride = new Ride(
                customer,
                37.7749, -122.4194,
                37.7833, -122.4167,
                "123 Market St, San Francisco, CA",
                "456 Mission St, San Francisco, CA",
                new BigDecimal("25.50"),
                "USD"
        );

        Ride saved = rideRepository.saveAndFlush(ride);

        assertNotNull(saved.getId(), "Ride ID should be generated");
        assertEquals(customer.getId(), saved.getCustomer().getId());
        assertNull(saved.getDriver(), "Driver should initially be null");
        assertEquals(RideStatus.REQUESTED, saved.getStatus());
        assertEquals(37.7749, saved.getPickupLatitude());
        assertEquals(-122.4194, saved.getPickupLongitude());
        assertEquals(37.7833, saved.getDropoffLatitude());
        assertEquals(-122.4167, saved.getDropoffLongitude());
        assertEquals("123 Market St, San Francisco, CA", saved.getPickupAddress());
        assertEquals("456 Mission St, San Francisco, CA", saved.getDropoffAddress());
        assertEquals(new BigDecimal("25.50"), saved.getEstimatedFare());
        assertEquals("USD", saved.getCurrency());
        assertNotNull(saved.getCreatedAt(), "CreatedAt timestamp should be populated");
        assertNotNull(saved.getUpdatedAt(), "UpdatedAt timestamp should be populated");

        Optional<Ride> found = rideRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());

        List<Ride> customerRides = rideRepository.findByCustomerId(customer.getId());
        assertEquals(1, customerRides.size());
        assertEquals(saved.getId(), customerRides.get(0).getId());

        List<Ride> requestedRides = rideRepository.findByStatus(RideStatus.REQUESTED);
        assertTrue(requestedRides.stream().anyMatch(r -> r.getId().equals(saved.getId())));
    }

    @Test
    void testSaveRideWithAssignedDriver() {
        User customer = userRepository.save(new User("Bob Rider", "bob.rider@example.com",
                "+1987654322", "hashedpw", Role.CUSTOMER));
        User driverUser = userRepository.save(new User("Charlie Driver", "charlie.driver@example.com",
                "+1987654323", "hashedpw", Role.DRIVER));
        Driver driver = driverRepository.save(new Driver(driverUser, "DL-RIDE-01", VerificationStatus.APPROVED, true));

        Ride ride = new Ride(
                customer,
                12.9716, 77.5946,
                12.9352, 77.6245,
                "MG Road, Bangalore",
                "Koramangala, Bangalore",
                new BigDecimal("350.00"),
                "INR"
        );
        ride.setDriver(driver);
        ride.setStatus(RideStatus.ACCEPTED);

        Ride saved = rideRepository.saveAndFlush(ride);

        assertNotNull(saved.getId());
        assertNotNull(saved.getDriver());
        assertEquals(driver.getId(), saved.getDriver().getId());
        assertEquals(RideStatus.ACCEPTED, saved.getStatus());
        assertEquals("INR", saved.getCurrency());

        List<Ride> driverRides = rideRepository.findByDriverId(driver.getId());
        assertEquals(1, driverRides.size());
        assertEquals(saved.getId(), driverRides.get(0).getId());

        List<Ride> driverAccepted = rideRepository.findByDriverIdAndStatus(driver.getId(), RideStatus.ACCEPTED);
        assertEquals(1, driverAccepted.size());
        assertEquals(saved.getId(), driverAccepted.get(0).getId());
    }

    @Test
    void testLatitudeBoundaryValidation() {
        User customer = userRepository.save(new User("Coord User", "coord.user@example.com",
                "+1987654324", "hashedpw", Role.CUSTOMER));

        // Invalid pickup latitude > 90
        assertThrows(IllegalArgumentException.class, () -> {
            new Ride(customer, 90.001, 0.0, 0.0, 0.0, "Start", "End", BigDecimal.TEN, "USD");
        });

        // Invalid pickup latitude < -90
        assertThrows(IllegalArgumentException.class, () -> {
            new Ride(customer, -90.001, 0.0, 0.0, 0.0, "Start", "End", BigDecimal.TEN, "USD");
        });

        // Invalid dropoff latitude > 90
        assertThrows(IllegalArgumentException.class, () -> {
            new Ride(customer, 0.0, 0.0, 95.0, 0.0, "Start", "End", BigDecimal.TEN, "USD");
        });

        // Valid boundary latitudes -90 and 90
        Ride boundaryRide = new Ride(customer, -90.0, 0.0, 90.0, 0.0, "South Pole", "North Pole", BigDecimal.TEN, "USD");
        Ride saved = rideRepository.saveAndFlush(boundaryRide);
        assertNotNull(saved.getId());
    }

    @Test
    void testLongitudeBoundaryValidation() {
        User customer = userRepository.save(new User("Long User", "long.user@example.com",
                "+1987654325", "hashedpw", Role.CUSTOMER));

        // Invalid pickup longitude > 180
        assertThrows(IllegalArgumentException.class, () -> {
            new Ride(customer, 0.0, 180.001, 0.0, 0.0, "Start", "End", BigDecimal.TEN, "USD");
        });

        // Invalid pickup longitude < -180
        assertThrows(IllegalArgumentException.class, () -> {
            new Ride(customer, 0.0, -180.001, 0.0, 0.0, "Start", "End", BigDecimal.TEN, "USD");
        });

        // Invalid dropoff longitude > 180
        assertThrows(IllegalArgumentException.class, () -> {
            new Ride(customer, 0.0, 0.0, 0.0, 185.0, "Start", "End", BigDecimal.TEN, "USD");
        });

        // Valid boundary longitudes -180 and 180
        Ride boundaryRide = new Ride(customer, 0.0, -180.0, 0.0, 180.0, "West", "East", BigDecimal.TEN, "USD");
        Ride saved = rideRepository.saveAndFlush(boundaryRide);
        assertNotNull(saved.getId());
    }

    @Test
    void testNegativeEstimatedFareRejected() {
        User customer = userRepository.save(new User("Fare User", "fare.user@example.com",
                "+1987654326", "hashedpw", Role.CUSTOMER));

        assertThrows(IllegalArgumentException.class, () -> {
            new Ride(customer, 37.77, -122.41, 37.78, -122.42, "Start", "End", new BigDecimal("-0.01"), "USD");
        });

        Ride validRide = new Ride(customer, 37.77, -122.41, 37.78, -122.42, "Start", "End", BigDecimal.ZERO, "USD");
        assertThrows(IllegalArgumentException.class, () -> {
            validRide.setEstimatedFare(new BigDecimal("-10.00"));
        });
    }

    @Test
    void testRideLifecycleStatusTransitions() {
        User customer = userRepository.save(new User("Lifecycle User", "lifecycle.user@example.com",
                "+1987654327", "hashedpw", Role.CUSTOMER));
        User driverUser = userRepository.save(new User("Lifecycle Driver", "lifecycle.driver@example.com",
                "+1987654328", "hashedpw", Role.DRIVER));
        Driver driver = driverRepository.save(new Driver(driverUser, "DL-LIFE-01", VerificationStatus.APPROVED, true));

        Ride ride = new Ride(
                customer,
                40.7128, -74.0060,
                40.7580, -73.9855,
                "Times Square", "Central Park",
                new BigDecimal("15.00"), "USD"
        );
        Ride saved = rideRepository.saveAndFlush(ride);
        assertEquals(RideStatus.REQUESTED, saved.getStatus());

        saved.setDriver(driver);
        saved.setStatus(RideStatus.ACCEPTED);
        Ride accepted = rideRepository.saveAndFlush(saved);
        assertEquals(RideStatus.ACCEPTED, accepted.getStatus());

        accepted.setStatus(RideStatus.ARRIVED);
        Ride arrived = rideRepository.saveAndFlush(accepted);
        assertEquals(RideStatus.ARRIVED, arrived.getStatus());

        arrived.setStatus(RideStatus.IN_PROGRESS);
        Ride inProgress = rideRepository.saveAndFlush(arrived);
        assertEquals(RideStatus.IN_PROGRESS, inProgress.getStatus());

        inProgress.setStatus(RideStatus.COMPLETED);
        Ride completed = rideRepository.saveAndFlush(inProgress);
        assertEquals(RideStatus.COMPLETED, completed.getStatus());
    }

    @Test
    void testRideCancellation() {
        User customer = userRepository.save(new User("Cancel User", "cancel.user@example.com",
                "+1987654329", "hashedpw", Role.CUSTOMER));

        Ride ride = new Ride(
                customer,
                51.5074, -0.1278,
                51.5007, -0.1246,
                "Trafalgar Square", "Big Ben",
                new BigDecimal("12.50"), "GBP"
        );
        Ride saved = rideRepository.saveAndFlush(ride);
        assertEquals(RideStatus.REQUESTED, saved.getStatus());

        saved.setStatus(RideStatus.CANCELLED);
        Ride cancelled = rideRepository.saveAndFlush(saved);
        assertEquals(RideStatus.CANCELLED, cancelled.getStatus());
    }
}

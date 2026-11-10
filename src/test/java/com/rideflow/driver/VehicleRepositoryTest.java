package com.rideflow.driver;

import com.rideflow.user.Role;
import com.rideflow.user.User;
import com.rideflow.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindVehicle() {
        User user = userRepository.save(new User("Car Driver", "car.driver@example.com", "+1234567890", "hashedpw", Role.DRIVER));
        Driver driver = driverRepository.save(new Driver(user, "DL-CAR-01"));

        Vehicle vehicle = new Vehicle(driver, VehicleType.SEDAN, "Toyota", "Camry", "ka-05-mc-1234", "Silver");
        Vehicle saved = vehicleRepository.saveAndFlush(vehicle);

        assertNotNull(saved.getId(), "Vehicle ID should be generated");
        assertEquals(driver.getId(), saved.getDriver().getId());
        assertEquals(VehicleType.SEDAN, saved.getVehicleType());
        assertEquals("Toyota", saved.getMake());
        assertEquals("Camry", saved.getModel());
        assertEquals("KA-05-MC-1234", saved.getRegistrationNumber(), "Registration number should be uppercase normalized");
        assertEquals("Silver", saved.getColor());
        assertNotNull(saved.getCreatedAt(), "CreatedAt should be populated");
        assertNotNull(saved.getUpdatedAt(), "UpdatedAt should be populated");

        Optional<Vehicle> byId = vehicleRepository.findById(saved.getId());
        assertTrue(byId.isPresent());
        assertEquals("KA-05-MC-1234", byId.get().getRegistrationNumber());

        Optional<Vehicle> byDriverId = vehicleRepository.findByDriverId(driver.getId());
        assertTrue(byDriverId.isPresent());
        assertEquals(saved.getId(), byDriverId.get().getId());

        assertTrue(vehicleRepository.existsByDriverId(driver.getId()));
        assertTrue(vehicleRepository.existsByRegistrationNumber("KA-05-MC-1234"));
    }

    @Test
    void testDuplicateRegistrationNumberRejected() {
        User user1 = userRepository.save(new User("Driver A", "driver.a@example.com", "+1000000010", "hashedpw", Role.DRIVER));
        Driver driver1 = driverRepository.save(new Driver(user1, "DL-REG-1"));
        Vehicle vehicle1 = new Vehicle(driver1, VehicleType.SEDAN, "Honda", "Civic", "DL-01-AB-9999", "Black");
        vehicleRepository.saveAndFlush(vehicle1);

        User user2 = userRepository.save(new User("Driver B", "driver.b@example.com", "+1000000011", "hashedpw", Role.DRIVER));
        Driver driver2 = driverRepository.save(new Driver(user2, "DL-REG-2"));
        Vehicle vehicle2 = new Vehicle(driver2, VehicleType.SUV, "Hyundai", "Creta", "DL-01-AB-9999", "White");

        assertThrows(DataIntegrityViolationException.class, () -> {
            vehicleRepository.saveAndFlush(vehicle2);
        }, "Duplicate registration number should violate unique constraint");
    }

    @Test
    void testDuplicateVehicleForSameDriverRejected() {
        User user = userRepository.save(new User("Single Driver", "single.driver@example.com", "+1000000012", "hashedpw", Role.DRIVER));
        Driver driver = driverRepository.save(new Driver(user, "DL-REG-3"));

        Vehicle vehicle1 = new Vehicle(driver, VehicleType.HATCHBACK, "Maruti", "Swift", "MH-01-CD-1111", "Red");
        vehicleRepository.saveAndFlush(vehicle1);

        Vehicle vehicle2 = new Vehicle(driver, VehicleType.SEDAN, "Skoda", "Slavia", "MH-01-CD-2222", "Blue");

        assertThrows(DataIntegrityViolationException.class, () -> {
            vehicleRepository.saveAndFlush(vehicle2);
        }, "Driver cannot have more than one vehicle registered");
    }
}

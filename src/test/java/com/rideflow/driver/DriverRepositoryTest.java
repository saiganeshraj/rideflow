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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DriverRepositoryTest {

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindDriver() {
        User user = userRepository.save(new User("Bob Driver", "bob.driver@example.com", "+1234567899", "hashedpw", Role.DRIVER));
        Driver driver = new Driver(user, "DL-98765");
        Driver saved = driverRepository.saveAndFlush(driver);

        assertNotNull(saved.getId(), "Driver ID should be generated");
        assertEquals(user.getId(), saved.getUser().getId(), "Driver should reference correct user ID");
        assertEquals("DL-98765", saved.getLicenseNumber());
        assertEquals(VerificationStatus.PENDING, saved.getVerificationStatus(), "VerificationStatus should default to PENDING");
        assertFalse(saved.isAvailable(), "Driver should default to unavailable");
        assertNotNull(saved.getCreatedAt(), "CreatedAt should be populated");
        assertNotNull(saved.getUpdatedAt(), "UpdatedAt should be populated");

        Optional<Driver> found = driverRepository.findById(saved.getId());
        assertTrue(found.isPresent(), "Driver should be retrievable by ID");
        assertEquals("DL-98765", found.get().getLicenseNumber());

        Optional<Driver> byUserId = driverRepository.findByUserId(user.getId());
        assertTrue(byUserId.isPresent(), "Driver should be retrievable by user ID");
        assertEquals(saved.getId(), byUserId.get().getId());

        assertTrue(driverRepository.existsByUserId(user.getId()), "existsByUserId should return true");
        assertTrue(driverRepository.existsByLicenseNumber("DL-98765"), "existsByLicenseNumber should return true");
    }

    @Test
    void testDuplicateLicenseNumberRejected() {
        User user1 = userRepository.save(new User("Driver One", "driver1@example.com", "+1000000001", "hashedpw", Role.DRIVER));
        Driver driver1 = new Driver(user1, "DL-UNIQUE-123");
        driverRepository.saveAndFlush(driver1);

        User user2 = userRepository.save(new User("Driver Two", "driver2@example.com", "+1000000002", "hashedpw", Role.DRIVER));
        Driver driver2 = new Driver(user2, "DL-UNIQUE-123");

        assertThrows(DataIntegrityViolationException.class, () -> {
            driverRepository.saveAndFlush(driver2);
        }, "Saving driver with duplicate license number should throw DataIntegrityViolationException");
    }

    @Test
    void testDuplicateUserRejected() {
        User user = userRepository.save(new User("Single User", "single.user@example.com", "+1000000003", "hashedpw", Role.DRIVER));
        Driver driver1 = new Driver(user, "DL-AAA-111");
        driverRepository.saveAndFlush(driver1);

        Driver driver2 = new Driver(user, "DL-BBB-222");

        assertThrows(DataIntegrityViolationException.class, () -> {
            driverRepository.saveAndFlush(driver2);
        }, "Saving multiple driver profiles for the same user should throw DataIntegrityViolationException");
    }

    @Test
    void testAllowedVerificationStatuses() {
        User user1 = userRepository.save(new User("Approved Driver", "appr.driver@example.com", "+1000000004", "hashedpw", Role.DRIVER));
        Driver driver1 = new Driver(user1, "DL-APPROVED", VerificationStatus.APPROVED);
        Driver saved1 = driverRepository.saveAndFlush(driver1);
        assertEquals(VerificationStatus.APPROVED, saved1.getVerificationStatus());

        User user2 = userRepository.save(new User("Rejected Driver", "rej.driver@example.com", "+1000000005", "hashedpw", Role.DRIVER));
        Driver driver2 = new Driver(user2, "DL-REJECTED", VerificationStatus.REJECTED);
        Driver saved2 = driverRepository.saveAndFlush(driver2);
        assertEquals(VerificationStatus.REJECTED, saved2.getVerificationStatus());
    }

    @Test
    void testDriverDefaultsToUnavailableAndCanBeToggled() {
        User user = userRepository.save(new User("Toggle Driver", "toggle.driver@example.com", "+1000000006", "hashedpw", Role.DRIVER));
        Driver driver = new Driver(user, "DL-TOGGLE-1");
        Driver saved = driverRepository.saveAndFlush(driver);

        assertFalse(saved.isAvailable(), "New driver should default to unavailable");

        saved.setAvailable(true);
        Driver updated = driverRepository.saveAndFlush(saved);
        assertTrue(updated.isAvailable(), "Driver availability should be updated to true");

        updated.setAvailable(false);
        Driver reverted = driverRepository.saveAndFlush(updated);
        assertFalse(reverted.isAvailable(), "Driver availability should be reverted to false");
    }
}

package com.rideflow.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindByEmail() {
        User user = new User("Alice Smith", "  ALICE@example.COM  ", "+1234567890", "$2a$10$hashedpasswordplaceholder", Role.CUSTOMER);
        User saved = userRepository.save(user);

        assertNotNull(saved.getId(), "User ID should be generated");
        assertEquals("alice@example.com", saved.getEmail(), "Email should be trimmed and lowercased");
        assertNotNull(saved.getCreatedAt(), "CreatedAt timestamp should be populated");
        assertNotNull(saved.getUpdatedAt(), "UpdatedAt timestamp should be populated");
        assertTrue(saved.isActive(), "User should be active by default");
        assertEquals(Role.CUSTOMER, saved.getRole());

        Optional<User> found = userRepository.findByEmail("alice@example.com");
        assertTrue(found.isPresent(), "User should be retrievable by normalized email");
        assertTrue(userRepository.existsByEmail("alice@example.com"));
        assertTrue(userRepository.existsByPhoneNumber("+1234567890"));
    }
}

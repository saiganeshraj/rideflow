package com.rideflow.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.security.JwtTokenProvider;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testGetCurrentUserWithValidBearerToken() throws Exception {
        User user = new User("Authenticated User", "auth.user@example.com", "+1234567890",
                passwordEncoder.encode("secret123"), Role.CUSTOMER);
        User saved = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(saved);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().toString())))
                .andExpect(jsonPath("$.fullName", is("Authenticated User")))
                .andExpect(jsonPath("$.email", is("auth.user@example.com")))
                .andExpect(jsonPath("$.role", is("CUSTOMER")))
                .andExpect(jsonPath("$.active", is(true)))
                // Verify no credential fields exposed
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void testGetCurrentUserWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testGetCurrentUserWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer invalid.jwt.token.string")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testGetCurrentUserWithExpiredTokenReturns401() throws Exception {
        User user = new User("Expired User", "expired.jwt@example.com", null,
                passwordEncoder.encode("secret123"), Role.CUSTOMER);
        User saved = userRepository.save(user);

        // Generate token with expired lifetime
        JwtTokenProvider expiredProvider = new JwtTokenProvider(
                "test-jwt-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-256-testing!", -5000);
        String expiredToken = expiredProvider.generateToken(saved);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testGetCurrentUserWithNonBearerHeaderReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Basic dXNlcjpwYXNz")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testGetCurrentUserWithInactiveAccountReturns401() throws Exception {
        User inactive = new User("Disabled User", "disabled@example.com", null,
                passwordEncoder.encode("secret123"), Role.CUSTOMER);
        inactive.setActive(false);
        User saved = userRepository.save(inactive);

        String token = jwtTokenProvider.generateToken(saved);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.detail", is("Account is inactive")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testPublicEndpointsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}

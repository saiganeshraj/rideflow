package com.rideflow.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.rideflow.security.JwtTokenProvider jwtTokenProvider;

    @Test
    void testSuccessfulRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane.doe@example.com", "securePass123", "+1987654321");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.fullName", is("Jane Doe")))
                .andExpect(jsonPath("$.email", is("jane.doe@example.com")))
                .andExpect(jsonPath("$.role", is("CUSTOMER")))
                .andExpect(jsonPath("$.active", is(true)))
                // Verify safe response: no password or passwordHash leaked
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void testPasswordIsHashedInDatabase() throws Exception {
        String rawPassword = "mySecretPassword123";
        RegisterRequest request = new RegisterRequest("Secure User", "secure.user@example.com", rawPassword, null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        Optional<User> savedUser = userRepository.findByEmail("secure.user@example.com");
        assertTrue(savedUser.isPresent(), "User should be found in database");
        assertNotEquals(rawPassword, savedUser.get().getPasswordHash(), "Stored password must not be plaintext");
        assertTrue(passwordEncoder.matches(rawPassword, savedUser.get().getPasswordHash()), "BCrypt hash must match raw password");
    }

    @Test
    void testEmailNormalizationDuringRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest("Normalized User", "  MIXED.Case@Example.COM  ", "password123", null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("mixed.case@example.com")));

        assertTrue(userRepository.existsByEmail("mixed.case@example.com"), "Database should store lowercased email");
    }

    @Test
    void testDuplicateEmailReturns409Conflict() throws Exception {
        RegisterRequest first = new RegisterRequest("First User", "duplicate@example.com", "password123", null);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        // Attempt second registration with same email (different case and whitespace)
        RegisterRequest duplicate = new RegisterRequest("Second User", "  DUPLICATE@example.com  ", "password456", null);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Conflict")))
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void testInvalidInputReturns400BadRequest() throws Exception {
        // Blank full name
        RegisterRequest invalidName = new RegisterRequest("", "valid@example.com", "password123", null);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.fullName", notNullValue()));

        // Invalid email format
        RegisterRequest invalidEmail = new RegisterRequest("Valid Name", "not-an-email", "password123", null);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email", notNullValue()));

        // Short password (< 8 chars)
        RegisterRequest shortPassword = new RegisterRequest("Valid Name", "valid2@example.com", "short", null);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    void testRegistrationWithoutPhoneNumberSucceeds() throws Exception {
        RegisterRequest noPhone = new RegisterRequest("No Phone User", "nophone@example.com", "password123", null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noPhone)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", is("nophone@example.com")));
    }

    @Test
    void testSuccessfulLogin() throws Exception {
        RegisterRequest reg = new RegisterRequest("Login User", "login.success@example.com", "mySecret123", null);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest("login.success@example.com", "mySecret123");
        String responseContent = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(900)))
                .andExpect(jsonPath("$.user.email", is("login.success@example.com")))
                .andExpect(jsonPath("$.user.role", is("CUSTOMER")))
                .andExpect(jsonPath("$.user.active", is(true)))
                // Verify safe response: no password leaked
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LoginResponse loginResponse = objectMapper.readValue(responseContent, LoginResponse.class);
        String token = loginResponse.getAccessToken();

        assertTrue(jwtTokenProvider.validateToken(token), "Generated token should be valid");
        assertEquals("login.success@example.com", jwtTokenProvider.getEmail(token));
        assertEquals(Role.CUSTOMER, jwtTokenProvider.getRole(token));
    }

    @Test
    void testLoginWithIncorrectPasswordReturns401() throws Exception {
        RegisterRequest reg = new RegisterRequest("Wrong Pass User", "wrongpass@example.com", "correctPass123", null);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest("wrongpass@example.com", "wrongPassword!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.detail", is("Invalid email or password")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testLoginWithUnknownEmailReturns401() throws Exception {
        LoginRequest loginReq = new LoginRequest("nonexistent@example.com", "somePassword123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.detail", is("Invalid email or password")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testLoginWithInactiveAccountReturns401() throws Exception {
        // Create an inactive user directly
        User inactiveUser = new User("Inactive User", "inactive@example.com", null,
                passwordEncoder.encode("password123"), Role.CUSTOMER);
        inactiveUser.setActive(false);
        userRepository.save(inactiveUser);

        LoginRequest loginReq = new LoginRequest("inactive@example.com", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.detail", is("Account is inactive")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void testLoginWithNormalizedEmail() throws Exception {
        RegisterRequest reg = new RegisterRequest("Case User", "case.test@example.com", "password123", null);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest("  CASE.TEST@EXAMPLE.COM  ", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.user.email", is("case.test@example.com")));
    }

    @Test
    void testLoginWithInvalidInputReturns400() throws Exception {
        LoginRequest blankEmail = new LoginRequest("", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.email", notNullValue()));

        LoginRequest blankPassword = new LoginRequest("valid@example.com", "");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }
}

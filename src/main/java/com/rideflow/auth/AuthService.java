package com.rideflow.auth;

import com.rideflow.common.exception.DuplicateResourceException;
import com.rideflow.common.exception.UnauthorizedException;
import com.rideflow.security.JwtTokenProvider;
import com.rideflow.user.Role;
import com.rideflow.user.User;
import com.rideflow.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling authentication workflows including user registration and login.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Registers a new user with default role CUSTOMER.
     *
     * @param request the registration request payload
     * @return a safe user response containing account details
     * @throws DuplicateResourceException if the email is already registered
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null;

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        String phoneNumber = request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()
                ? request.getPhoneNumber().trim()
                : null;

        User user = new User(
                request.getFullName().trim(),
                normalizedEmail,
                phoneNumber,
                passwordHash,
                Role.CUSTOMER
        );

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.isActive()
        );
    }

    /**
     * Authenticates user credentials and returns a JWT access token.
     *
     * @param request login payload with email and password
     * @return login response containing token, expiry, and user profile
     * @throws UnauthorizedException if credentials are invalid or account is inactive
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is inactive");
        }

        String token = jwtTokenProvider.generateToken(user);
        long expiresIn = jwtTokenProvider.getExpiresInSeconds();

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isActive()
        );

        return new LoginResponse(token, "Bearer", expiresIn, userResponse);
    }
}

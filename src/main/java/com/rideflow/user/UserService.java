package com.rideflow.user;

import com.rideflow.auth.UserResponse;
import com.rideflow.common.exception.ResourceNotFoundException;
import com.rideflow.common.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service providing user profile retrieval and management operations.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieves the safe public profile of an authenticated user by ID.
     *
     * @param userId UUID of the authenticated user
     * @return safe UserResponse
     * @throws ResourceNotFoundException if user does not exist in the database
     * @throws UnauthorizedException     if the user account is inactive
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is inactive");
        }

        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isActive()
        );
    }
}

package com.rideflow.user;

import com.rideflow.auth.UserResponse;
import com.rideflow.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing endpoints for user operations and current profile retrieval.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile and account operations")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     * Identity is strictly obtained from the security context, never from request parameters.
     *
     * @param principal the authenticated user principal from the security context
     * @return safe UserResponse
     */
    @GetMapping("/me")
    @Operation(
            summary = "Get current user profile",
            description = "Returns profile details of the authenticated user identified by the Bearer token",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public UserResponse getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getCurrentUserProfile(principal.getId());
    }
}

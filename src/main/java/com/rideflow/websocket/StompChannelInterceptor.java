package com.rideflow.websocket;

import com.rideflow.driver.Driver;
import com.rideflow.driver.DriverRepository;
import com.rideflow.ride.Ride;
import com.rideflow.ride.RideRepository;
import com.rideflow.security.JwtTokenProvider;
import com.rideflow.security.UserPrincipal;
import com.rideflow.user.Role;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.Collections;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inbound STOMP channel interceptor enforcing JWT authentication on CONNECT frames
 * and fine-grained authorization on SUBSCRIBE frames to prevent unauthorized data access.
 */
@Component
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final Pattern RIDE_TOPIC_PATTERN = Pattern.compile("^/topic/rides/([0-9a-fA-F\\-]+)$");
    private static final Pattern RIDE_LOCATION_TOPIC_PATTERN = Pattern.compile("^/topic/rides/([0-9a-fA-F\\-]+)/location$");
    private static final Pattern DRIVER_OFFERS_TOPIC_PATTERN = Pattern.compile("^/topic/drivers/([0-9a-fA-F\\-]+)/offers$");

    private final JwtTokenProvider jwtTokenProvider;
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;

    public StompChannelInterceptor(JwtTokenProvider jwtTokenProvider,
                                   RideRepository rideRepository,
                                   DriverRepository driverRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            return handleConnect(message, accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
            throw new AccessDeniedException("Unauthorized: missing or invalid JWT authentication token in STOMP headers");
        }

        UUID userId = jwtTokenProvider.getUserId(token);
        String email = jwtTokenProvider.getEmail(token);
        Role role = jwtTokenProvider.getRole(token);

        UserPrincipal principal = new UserPrincipal(userId, email, role);
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.name());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(authority));

        if (accessor.isMutable()) {
            accessor.setUser(authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return message;
        } else {
            StompHeaderAccessor mutableAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            mutableAccessor.copyHeaders(accessor.toMap());
            mutableAccessor.setUser(authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            byte[] payload = message.getPayload() instanceof byte[] ? (byte[]) message.getPayload() : new byte[0];
            return MessageBuilder.createMessage(payload, mutableAccessor.getMessageHeaders());
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user == null) {
            user = SecurityContextHolder.getContext().getAuthentication();
        }

        if (!(user instanceof Authentication auth) || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("Unauthenticated subscription attempt");
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            throw new AccessDeniedException("Subscription destination cannot be null");
        }

        // Allow user-scoped private queues
        if (destination.startsWith("/user/") || destination.startsWith("/queue/")) {
            return;
        }

        // 1. Ride topic: /topic/rides/{rideId}
        Matcher rideMatcher = RIDE_TOPIC_PATTERN.matcher(destination);
        if (rideMatcher.matches()) {
            UUID rideId = parseUuid(rideMatcher.group(1), destination);
            authorizeRideSubscription(rideId, principal);
            return;
        }

        // 2. Ride location topic: /topic/rides/{rideId}/location
        Matcher locationMatcher = RIDE_LOCATION_TOPIC_PATTERN.matcher(destination);
        if (locationMatcher.matches()) {
            UUID rideId = parseUuid(locationMatcher.group(1), destination);
            authorizeRideSubscription(rideId, principal);
            return;
        }

        // 3. Driver offers topic: /topic/drivers/{driverId}/offers
        Matcher driverMatcher = DRIVER_OFFERS_TOPIC_PATTERN.matcher(destination);
        if (driverMatcher.matches()) {
            UUID driverId = parseUuid(driverMatcher.group(1), destination);
            authorizeDriverOfferSubscription(driverId, principal);
            return;
        }

        throw new AccessDeniedException("Unauthorized subscription destination: " + destination);
    }

    private void authorizeRideSubscription(UUID rideId, UserPrincipal principal) {
        Ride ride = rideRepository.findByIdWithCustomerAndDriver(rideId)
                .orElseThrow(() -> new AccessDeniedException("Ride not found for subscription: " + rideId));

        boolean isCustomer = ride.getCustomer().getId().equals(principal.getId());
        boolean isDriver = ride.getDriver() != null && ride.getDriver().getUser().getId().equals(principal.getId());

        if (!isCustomer && !isDriver) {
            throw new AccessDeniedException("User is not authorized to subscribe to ride " + rideId);
        }
    }

    private void authorizeDriverOfferSubscription(UUID driverId, UserPrincipal principal) {
        Driver driver = driverRepository.findByIdWithUser(driverId)
                .orElseThrow(() -> new AccessDeniedException("Driver not found for subscription: " + driverId));

        if (!driver.getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Driver is not authorized to subscribe to offers for driver " + driverId);
        }
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        String tokenHeader = accessor.getFirstNativeHeader("token");
        if (StringUtils.hasText(tokenHeader)) {
            return tokenHeader.startsWith("Bearer ") ? tokenHeader.substring(7) : tokenHeader;
        }
        return null;
    }

    private UUID parseUuid(String uuidStr, String destination) {
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException("Invalid UUID in destination: " + destination);
        }
    }
}

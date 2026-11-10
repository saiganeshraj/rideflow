package com.rideflow.websocket;

import com.rideflow.dispatch.DispatchService;
import com.rideflow.dispatch.RideOffer;
import com.rideflow.dispatch.RideOfferRepository;
import com.rideflow.dispatch.RideOfferStatus;
import com.rideflow.driver.Driver;
import com.rideflow.driver.DriverLocation;
import com.rideflow.driver.DriverLocationStore;
import com.rideflow.driver.DriverLocationUpdateRequest;
import com.rideflow.driver.DriverRepository;
import com.rideflow.driver.DriverService;
import com.rideflow.driver.Vehicle;
import com.rideflow.driver.VehicleRepository;
import com.rideflow.driver.VehicleType;
import com.rideflow.driver.VerificationStatus;
import com.rideflow.ride.Ride;
import com.rideflow.ride.RideRepository;
import com.rideflow.ride.RideService;
import com.rideflow.ride.RideStatus;
import com.rideflow.security.JwtTokenProvider;
import com.rideflow.security.UserPrincipal;
import com.rideflow.user.Role;
import com.rideflow.user.User;
import com.rideflow.user.UserRepository;
import com.rideflow.websocket.dto.DriverLocationUpdateEvent;
import com.rideflow.websocket.dto.RideLifecycleEvent;
import com.rideflow.websocket.dto.RideOfferEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class WebSocketIntegrationTest {

    @Autowired
    private StompChannelInterceptor stompChannelInterceptor;

    @Autowired
    private RideService rideService;

    @Autowired
    private DriverService driverService;

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private RideOfferRepository rideOfferRepository;

    @Autowired
    private DriverLocationStore driverLocationStore;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    private MessageChannel dummyChannel;

    private User customerUser;
    private String customerToken;

    private User driverUser;
    private Driver driver;
    private String driverToken;

    @BeforeEach
    void setUp() {
        dummyChannel = mock(MessageChannel.class);

        customerUser = userRepository.save(new User(
                "Cust WS", "cust.ws@example.com", "+919811000001",
                passwordEncoder.encode("secret123"), Role.CUSTOMER
        ));
        customerToken = jwtTokenProvider.generateToken(customerUser);

        driverUser = userRepository.save(new User(
                "Driver WS", "driver.ws@example.com", "+919811000002",
                passwordEncoder.encode("secret123"), Role.DRIVER
        ));
        driver = driverRepository.save(new Driver(driverUser, "DL-WS-" + UUID.randomUUID().toString().substring(0, 8),
                VerificationStatus.APPROVED, true));

        vehicleRepository.save(new Vehicle(driver, VehicleType.SEDAN, "Hyundai", "Verna",
                "KA-04-WS" + UUID.randomUUID().toString().substring(0, 3).toUpperCase(), "White"));

        driverToken = jwtTokenProvider.generateToken(driverUser);

        reset(messagingTemplate);
    }

    @AfterEach
    void tearDown() {
        rideOfferRepository.deleteAll();
        rideRepository.deleteAll();
        vehicleRepository.deleteAll();
        driverRepository.deleteAll();
        userRepository.deleteAll();
        driverLocationStore.clear();
    }

    private Ride createRequestedRide() {
        Ride ride = new Ride(
                customerUser,
                12.9772, 77.5708,
                12.9352, 77.6245,
                "Majestic", "Koramangala",
                BigDecimal.valueOf(150.00), "INR"
        );
        return rideRepository.save(ride);
    }

    // ==================== 1. STOMP CONNECT Authentication Tests ====================

    @Test
    void testConnect_withValidJwt_authenticatesSuccessfully() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.addNativeHeader("Authorization", "Bearer " + customerToken);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = stompChannelInterceptor.preSend(message, dummyChannel);
        assertNotNull(result);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertTrue(resultAccessor.getUser() instanceof Authentication);

        Authentication auth = (Authentication) resultAccessor.getUser();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertEquals(customerUser.getId(), principal.getId());
        assertEquals(Role.CUSTOMER, principal.getRole());
    }

    @Test
    void testConnect_withMissingJwt_rejectsConnection() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () ->
                stompChannelInterceptor.preSend(message, dummyChannel)
        );
    }

    @Test
    void testConnect_withInvalidJwt_rejectsConnection() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer invalid.jwt.signature");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () ->
                stompChannelInterceptor.preSend(message, dummyChannel)
        );
    }

    // ==================== 2. Subscription Authorization Tests ====================

    @Test
    void testSubscribe_rideTopic_authorizedForOwningCustomerAndDriver() {
        Ride ride = createRequestedRide();
        ride.setDriver(driver);
        ride.setStatus(RideStatus.ACCEPTED);
        rideRepository.save(ride);

        // 1. Customer subscribes to their own ride -> Allowed
        StompHeaderAccessor custAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        custAccessor.setDestination("/topic/rides/" + ride.getId());
        custAccessor.setUser(new UsernamePasswordAuthenticationToken(UserPrincipal.create(customerUser), null, Collections.emptyList()));
        Message<byte[]> custMessage = MessageBuilder.createMessage(new byte[0], custAccessor.getMessageHeaders());

        Message<?> custResult = stompChannelInterceptor.preSend(custMessage, dummyChannel);
        assertNotNull(custResult);

        // 2. Assigned Driver subscribes to the ride -> Allowed
        StompHeaderAccessor driverAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        driverAccessor.setDestination("/topic/rides/" + ride.getId());
        driverAccessor.setUser(new UsernamePasswordAuthenticationToken(UserPrincipal.create(driverUser), null, Collections.emptyList()));
        Message<byte[]> driverMessage = MessageBuilder.createMessage(new byte[0], driverAccessor.getMessageHeaders());

        Message<?> driverResult = stompChannelInterceptor.preSend(driverMessage, dummyChannel);
        assertNotNull(driverResult);
    }

    @Test
    void testSubscribe_rideTopic_forbiddenForUnrelatedUser() {
        Ride ride = createRequestedRide();

        User unrelatedUser = userRepository.save(new User(
                "Unrelated", "unrelated@example.com", "+919811000999",
                passwordEncoder.encode("secret"), Role.CUSTOMER
        ));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/rides/" + ride.getId());
        accessor.setUser(new UsernamePasswordAuthenticationToken(UserPrincipal.create(unrelatedUser), null, Collections.emptyList()));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () ->
                stompChannelInterceptor.preSend(message, dummyChannel)
        );
    }

    @Test
    void testSubscribe_driverOffersTopic_enforcesDriverIsolation() {
        // Driver 1 subscribes to their own offers -> Allowed
        StompHeaderAccessor driverAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        driverAccessor.setDestination("/topic/drivers/" + driver.getId() + "/offers");
        driverAccessor.setUser(new UsernamePasswordAuthenticationToken(UserPrincipal.create(driverUser), null, Collections.emptyList()));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], driverAccessor.getMessageHeaders());

        Message<?> result = stompChannelInterceptor.preSend(message, dummyChannel);
        assertNotNull(result);

        // Driver 2 tries to subscribe to Driver 1's offers -> Forbidden
        User anotherDriverUser = userRepository.save(new User(
                "Driver 2", "d2@example.com", "+919811000888",
                passwordEncoder.encode("secret"), Role.DRIVER
        ));
        driverRepository.save(new Driver(anotherDriverUser, "DL-2-" + UUID.randomUUID().toString().substring(0, 6),
                VerificationStatus.APPROVED, true));

        StompHeaderAccessor wrongAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        wrongAccessor.setDestination("/topic/drivers/" + driver.getId() + "/offers");
        wrongAccessor.setUser(new UsernamePasswordAuthenticationToken(UserPrincipal.create(anotherDriverUser), null, Collections.emptyList()));
        Message<byte[]> wrongMessage = MessageBuilder.createMessage(new byte[0], wrongAccessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () ->
                stompChannelInterceptor.preSend(wrongMessage, dummyChannel)
        );
    }

    @Test
    void testSubscribe_unauthenticated_rejects() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/rides/" + UUID.randomUUID());
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () ->
                stompChannelInterceptor.preSend(message, dummyChannel)
        );
    }

    // ==================== 3. Event Publication on Ride Lifecycle Tests ====================

    @Test
    void testEventPublication_onRideLifecycleProgression() {
        Ride ride = createRequestedRide();
        UserPrincipal driverPrincipal = UserPrincipal.create(driverUser);

        // 1. Accept ride -> Publishes ACCEPTED event
        rideService.acceptRide(ride.getId(), driverPrincipal);

        ArgumentCaptor<RideLifecycleEvent> eventCaptor = ArgumentCaptor.forClass(RideLifecycleEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/rides/" + ride.getId()), eventCaptor.capture());

        RideLifecycleEvent acceptedEvent = eventCaptor.getValue();
        assertEquals(ride.getId(), acceptedEvent.getRideId());
        assertEquals(RideStatus.ACCEPTED, acceptedEvent.getStatus());
        assertEquals(driver.getId(), acceptedEvent.getDriverId());
        reset(messagingTemplate);

        // 2. Mark arrived -> Publishes ARRIVED event
        rideService.markArrived(ride.getId(), driverPrincipal);
        verify(messagingTemplate).convertAndSend(eq("/topic/rides/" + ride.getId()), eventCaptor.capture());
        assertEquals(RideStatus.ARRIVED, eventCaptor.getValue().getStatus());
        reset(messagingTemplate);

        // 3. Start ride -> Publishes IN_PROGRESS event
        rideService.startRide(ride.getId(), driverPrincipal);
        verify(messagingTemplate).convertAndSend(eq("/topic/rides/" + ride.getId()), eventCaptor.capture());
        assertEquals(RideStatus.IN_PROGRESS, eventCaptor.getValue().getStatus());
        reset(messagingTemplate);

        // 4. Complete ride -> Publishes COMPLETED event
        rideService.completeRide(ride.getId(), driverPrincipal);
        verify(messagingTemplate).convertAndSend(eq("/topic/rides/" + ride.getId()), eventCaptor.capture());
        assertEquals(RideStatus.COMPLETED, eventCaptor.getValue().getStatus());
    }

    @Test
    void testEventPublication_onRideCancelled() {
        Ride ride = createRequestedRide();
        UserPrincipal customerPrincipal = UserPrincipal.create(customerUser);

        rideService.cancelRide(ride.getId(), customerPrincipal);

        ArgumentCaptor<RideLifecycleEvent> eventCaptor = ArgumentCaptor.forClass(RideLifecycleEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/rides/" + ride.getId()), eventCaptor.capture());

        RideLifecycleEvent cancelledEvent = eventCaptor.getValue();
        assertEquals(ride.getId(), cancelledEvent.getRideId());
        assertEquals(RideStatus.CANCELLED, cancelledEvent.getStatus());
    }

    // ==================== 4. Event Publication on Location Update ====================

    @Test
    void testEventPublication_onDriverLocationUpdate_forActiveRide() {
        Ride ride = createRequestedRide();
        ride.setDriver(driver);
        ride.setStatus(RideStatus.IN_PROGRESS);
        rideRepository.save(ride);

        UserPrincipal driverPrincipal = UserPrincipal.create(driverUser);
        DriverLocationUpdateRequest updateRequest = new DriverLocationUpdateRequest(12.9800, 77.5720, 180.0, 35.0);

        driverService.updateLocation(driverPrincipal, updateRequest);

        ArgumentCaptor<DriverLocationUpdateEvent> captor = ArgumentCaptor.forClass(DriverLocationUpdateEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/rides/" + ride.getId() + "/location"), captor.capture());

        DriverLocationUpdateEvent event = captor.getValue();
        assertEquals(driver.getId(), event.getDriverId());
        assertEquals(ride.getId(), event.getRideId());
        assertEquals(12.9800, event.getLatitude());
        assertEquals(77.5720, event.getLongitude());
        assertEquals(180.0, event.getHeading());
        assertEquals(35.0, event.getSpeed());
    }

    // ==================== 5. Event Publication on Dispatch & Offers ====================

    @Test
    void testEventPublication_onDispatchAndOfferAcceptance() {
        // Place driver close to pickup (12.9780, 77.5710)
        driverLocationStore.saveLocation(new DriverLocation(
                driver.getId(), 12.9780, 77.5710, 90.0, 20.0, Instant.now()
        ));

        Ride ride = createRequestedRide();
        UserPrincipal customerPrincipal = UserPrincipal.create(customerUser);

        // 1. Dispatch ride -> Publishes offer to driver
        dispatchService.dispatchRide(ride.getId(), customerPrincipal, null);

        ArgumentCaptor<RideOfferEvent> offerCaptor = ArgumentCaptor.forClass(RideOfferEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/drivers/" + driver.getId() + "/offers"), offerCaptor.capture());

        RideOfferEvent offerEvent = offerCaptor.getValue();
        assertEquals(ride.getId(), offerEvent.getRideId());
        assertEquals(driver.getId(), offerEvent.getDriverId());
        assertEquals(RideOfferStatus.PENDING, offerEvent.getStatus());
        reset(messagingTemplate);

        // 2. Driver accepts offer -> Publishes ride lifecycle event and offer update
        List<RideOffer> offers = rideOfferRepository.findByDriverIdAndStatus(driver.getId(), RideOfferStatus.PENDING);
        assertEquals(1, offers.size());

        UserPrincipal driverPrincipal = UserPrincipal.create(driverUser);
        dispatchService.acceptOffer(offers.get(0).getId(), driverPrincipal);

        verify(messagingTemplate).convertAndSend(eq("/topic/rides/" + ride.getId()), any(RideLifecycleEvent.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/drivers/" + driver.getId() + "/offers"), any(RideOfferEvent.class));
    }
}

package com.rideflow.websocket;

import com.rideflow.websocket.event.DriverLocationDomainEvent;
import com.rideflow.websocket.event.RideLifecycleDomainEvent;
import com.rideflow.websocket.event.RideOfferDomainEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener that publishes domain events to authorized WebSocket STOMP destinations
 * strictly after the originating database transaction successfully commits.
 */
@Component
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRideLifecycleEvent(RideLifecycleDomainEvent event) {
        messagingTemplate.convertAndSend("/topic/rides/" + event.getRideId(), event.toDto());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDriverLocationEvent(DriverLocationDomainEvent event) {
        if (event.getRideId() != null) {
            messagingTemplate.convertAndSend("/topic/rides/" + event.getRideId() + "/location", event.toDto());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRideOfferEvent(RideOfferDomainEvent event) {
        messagingTemplate.convertAndSend("/topic/drivers/" + event.getDriverId() + "/offers", event.toDto());
    }
}

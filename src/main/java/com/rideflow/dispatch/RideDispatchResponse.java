package com.rideflow.dispatch;

import java.time.Instant;
import java.util.UUID;

/**
 * Response payload returned after dispatching a ride request.
 */
public class RideDispatchResponse {

    private UUID rideId;
    private String status;
    private int offersCount;
    private String message;
    private Instant dispatchedAt;

    public RideDispatchResponse() {
    }

    public RideDispatchResponse(UUID rideId, String status, int offersCount, String message, Instant dispatchedAt) {
        this.rideId = rideId;
        this.status = status;
        this.offersCount = offersCount;
        this.message = message;
        this.dispatchedAt = dispatchedAt;
    }

    public UUID getRideId() {
        return rideId;
    }

    public void setRideId(UUID rideId) {
        this.rideId = rideId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getOffersCount() {
        return offersCount;
    }

    public void setOffersCount(int offersCount) {
        this.offersCount = offersCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(Instant dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }
}

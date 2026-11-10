package com.rideflow.dispatch;

import java.time.Instant;
import java.util.UUID;

/**
 * Result returned by dispatch recovery workflows indicating whether new offers
 * were created or why re-dispatch could not proceed.
 */
public class DispatchRecoveryResult {

    private final UUID rideId;
    private final String status;
    private final int offersCreated;
    private final int retryAttempt;
    private final String message;
    private final Instant timestamp;

    public DispatchRecoveryResult(UUID rideId, String status, int offersCreated, int retryAttempt, String message) {
        this.rideId = rideId;
        this.status = status;
        this.offersCreated = offersCreated;
        this.retryAttempt = retryAttempt;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public UUID getRideId() {
        return rideId;
    }

    public String getStatus() {
        return status;
    }

    public int getOffersCreated() {
        return offersCreated;
    }

    public int getRetryAttempt() {
        return retryAttempt;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "DispatchRecoveryResult{" +
                "rideId=" + rideId +
                ", status='" + status + '\'' +
                ", offersCreated=" + offersCreated +
                ", retryAttempt=" + retryAttempt +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

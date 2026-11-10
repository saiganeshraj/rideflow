package com.rideflow.dispatch;

/**
 * Optional request payload when a driver rejects a ride offer.
 */
public class RideOfferRejectRequest {

    private String reason;

    public RideOfferRejectRequest() {
    }

    public RideOfferRejectRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

package com.rideflow.payment;

/**
 * Status lifecycle of a simulated payment.
 */
public enum PaymentStatus {
    /**
     * Payment has been initiated and is awaiting simulated processing.
     */
    PENDING,

    /**
     * Payment has concluded successfully.
     */
    SUCCESS,

    /**
     * Payment processing failed.
     */
    FAILED
}

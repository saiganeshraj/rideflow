package com.rideflow.common.exception;

/**
 * Thrown when a client request violates business rules or contains invalid arguments.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}

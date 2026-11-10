package com.rideflow.common.exception;

/**
 * Thrown when an authentication attempt fails due to invalid credentials
 * or an inactive account.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}

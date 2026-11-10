package com.rideflow.common.exception;

/**
 * Thrown when an entity cannot be created or updated because a unique resource
 * already exists (e.g. duplicate email).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}

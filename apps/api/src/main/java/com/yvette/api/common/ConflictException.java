package com.yvette.api.common;

/** The request conflicts with current state (duplicate slug/SKU/email, business rule). Maps to 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

package com.yvette.api.common;

import java.util.List;

/** The request conflicts with current state (duplicate slug/SKU/email, business rule). Maps to 409. */
public class ConflictException extends RuntimeException {

    private final List<String> errors;

    public ConflictException(String message) {
        this(message, List.of());
    }

    public ConflictException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}

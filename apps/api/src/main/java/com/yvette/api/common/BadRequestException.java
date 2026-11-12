package com.yvette.api.common;

import java.util.List;

/** Business validation failed (e.g. product can't be published yet). Maps to 400 with a list of problems. */
public class BadRequestException extends RuntimeException {

    private final List<String> errors;

    public BadRequestException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public BadRequestException(String message) {
        this(message, List.of());
    }

    public List<String> getErrors() {
        return errors;
    }
}

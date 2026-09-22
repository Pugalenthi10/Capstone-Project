package com.trichyestates.estatehub.exception;

/** Thrown when a unique value (e-mail, mobile) is already taken. Maps to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {
    private final String field;

    public DuplicateResourceException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}

package com.trichyestates.estatehub.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(boolean success, String message, Instant timestamp, int status, Map<String, String> errors) {

    public static ErrorResponse of(HttpStatus status, String message, Map<String, String> errors) {
        return new ErrorResponse(false, message, Instant.now(), status.value(), errors);
    }
}

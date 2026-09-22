package com.trichyestates.estatehub.dto;

/** Simple success envelope for endpoints that only need to return a message. */
public record ApiResponse(boolean success, String message) {
    public static ApiResponse ok(String message) { return new ApiResponse(true, message); }
}

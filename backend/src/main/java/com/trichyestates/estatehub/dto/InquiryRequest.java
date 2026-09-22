package com.trichyestates.estatehub.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Note: no userId. The buyer is always the authenticated user. */
public record InquiryRequest(
        @NotNull(message = "Property is required") Long propertyId,
        @Size(max = 1000, message = "Message must be at most 1000 characters") String message) { }

package com.trichyestates.estatehub.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkInquiryRequest(
        @NotEmpty(message = "Select at least one property")
        @Size(max = 50, message = "You can enquire about at most 50 properties at once")
        List<@NotNull Long> propertyIds,
        @Size(max = 1000, message = "Message must be at most 1000 characters") String message) { }

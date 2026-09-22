package com.trichyestates.estatehub.dto;

import com.trichyestates.estatehub.entity.InquiryStatus;
import jakarta.validation.constraints.NotNull;

public record InquiryStatusRequest(@NotNull(message = "Status is required") InquiryStatus status) { }

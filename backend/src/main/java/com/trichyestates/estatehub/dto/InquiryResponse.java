package com.trichyestates.estatehub.dto;

import com.trichyestates.estatehub.entity.Inquiry;
import com.trichyestates.estatehub.entity.InquiryStatus;

import java.time.Instant;

public record InquiryResponse(
        Long id, Long propertyId, String propertyTitle, String message, InquiryStatus status,
        Instant createdAt, Long buyerId, String buyerName, String buyerEmail, String buyerMobile) {

    public static InquiryResponse from(Inquiry i) {
        return new InquiryResponse(i.getId(), i.getProperty().getId(), i.getProperty().getTitle(),
                i.getMessage(), i.getStatus(), i.getCreatedAt(), i.getUser().getId(),
                i.getUser().getName(), i.getUser().getEmail(), i.getUser().getMobile());
    }
}

package com.trichyestates.estatehub.dto;

import com.trichyestates.estatehub.entity.ListingType;
import com.trichyestates.estatehub.entity.Property;
import com.trichyestates.estatehub.entity.PropertyCategory;

import java.time.Instant;

public record PropertyResponse(
        Long id, String title, PropertyCategory category, String location, String city, String state,
        Long price, String priceLabel, Integer bedrooms, Integer bathrooms, Integer area,
        String description, String imageUrl, ListingType listingType, Long agentId, Instant createdAt) {

    public static PropertyResponse from(Property p) {
        // getAgent() is a lazy proxy; reading only its id does not trigger a query.
        Long agentId = p.getAgent() == null ? null : p.getAgent().getId();
        return new PropertyResponse(p.getId(), p.getTitle(), p.getCategory(), p.getLocation(), p.getCity(),
                p.getState(), p.getPrice(), p.getPriceLabel(), p.getBedrooms(), p.getBathrooms(), p.getArea(),
                p.getDescription(), p.getImageUrl(), p.getListingType(), agentId, p.getCreatedAt());
    }
}

package com.trichyestates.estatehub.dto;

import com.trichyestates.estatehub.entity.ListingType;
import com.trichyestates.estatehub.entity.PropertyCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PropertyRequest(
        @NotBlank(message = "Title is required") @Size(max = 200) String title,
        @NotNull(message = "Category is required (APARTMENT, HOUSE or FLAT)") PropertyCategory category,
        @NotBlank(message = "Location is required") @Size(max = 150) String location,
        @Size(max = 100) String city,
        @Size(max = 100) String state,
        @NotNull(message = "Price is required") @Positive(message = "Price must be positive") Long price,
        @Size(max = 50) String priceLabel,
        @NotNull(message = "Bedrooms is required") @Min(value = 0, message = "Bedrooms cannot be negative") Integer bedrooms,
        @NotNull(message = "Bathrooms is required") @Min(value = 0, message = "Bathrooms cannot be negative") Integer bathrooms,
        @NotNull(message = "Area is required") @Positive(message = "Area must be positive") Integer area,
        @NotBlank(message = "Description is required") @Size(max = 2000) String description,
        @Size(max = 1000) @Pattern(regexp = "^$|^https?://\\S+$", message = "Image URL must start with http:// or https://") String imageUrl,
        ListingType listingType) { }

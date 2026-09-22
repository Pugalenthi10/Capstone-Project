package com.trichyestates.estatehub.service;

import com.trichyestates.estatehub.entity.ListingType;
import com.trichyestates.estatehub.entity.Property;
import com.trichyestates.estatehub.entity.PropertyCategory;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds the dynamic WHERE clause for property search (all values are bound parameters: no SQL injection). */
public final class PropertySpecifications {

    private PropertySpecifications() { }

    public static Specification<Property> matching(PropertyCategory category, ListingType listingType,
                                                   String location, Long minPrice, Long maxPrice,
                                                   Integer minBedrooms, Integer minBathrooms) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (listingType != null) {
                predicates.add(cb.equal(root.get("listingType"), listingType));
            }
            if (location != null && !location.isBlank()) {
                String like = "%" + escapeLike(location.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("location")), like, '\\'),
                        cb.like(cb.lower(root.get("city")), like, '\\')));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (minBedrooms != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bedrooms"), minBedrooms));
            }
            if (minBathrooms != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bathrooms"), minBathrooms));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

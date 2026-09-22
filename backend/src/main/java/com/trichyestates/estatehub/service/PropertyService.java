package com.trichyestates.estatehub.service;

import com.trichyestates.estatehub.dto.PageResponse;
import com.trichyestates.estatehub.dto.PropertyRequest;
import com.trichyestates.estatehub.dto.PropertyResponse;
import com.trichyestates.estatehub.entity.ListingType;
import com.trichyestates.estatehub.entity.Property;
import com.trichyestates.estatehub.entity.PropertyCategory;
import com.trichyestates.estatehub.entity.User;
import com.trichyestates.estatehub.exception.BadRequestException;
import com.trichyestates.estatehub.exception.ResourceNotFoundException;
import com.trichyestates.estatehub.repository.PropertyRepository;
import com.trichyestates.estatehub.repository.UserRepository;
import com.trichyestates.estatehub.security.AppUserDetails;
import com.trichyestates.estatehub.util.PriceFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class PropertyService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public PropertyService(PropertyRepository propertyRepository, UserRepository userRepository) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<PropertyResponse> search(String category, String listingType, String location,
                                                 Long minPrice, Long maxPrice, Integer bedrooms, Integer bathrooms,
                                                 String sort, int page, int size) {
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new BadRequestException("minPrice cannot be greater than maxPrice");
        }
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE), toSort(sort));

        Page<Property> result = propertyRepository.findAll(PropertySpecifications.matching(
                parseEnum(PropertyCategory.class, category, "category"),
                parseEnum(ListingType.class, listingType, "listingType"),
                location, minPrice, maxPrice, bedrooms, bathrooms), pageRequest);
        return PageResponse.of(result, PropertyResponse::from);
    }

    @Transactional(readOnly = true)
    public PropertyResponse get(Long id) {
        return PropertyResponse.from(find(id));
    }

    public PropertyResponse create(PropertyRequest req, Long actingUserId) {
        User agent = userRepository.getReferenceById(actingUserId);
        Property property = new Property();
        apply(property, req);
        property.setAgent(agent);   // ownership always comes from the JWT, never from the request body
        return PropertyResponse.from(propertyRepository.save(property));
    }

    public PropertyResponse update(Long id, PropertyRequest req, AppUserDetails actor) {
        Property property = find(id);
        assertCanModify(property, actor);
        apply(property, req);
        return PropertyResponse.from(propertyRepository.save(property));
    }

    public void delete(Long id, AppUserDetails actor) {
        Property property = find(id);
        assertCanModify(property, actor);
        propertyRepository.delete(property);
    }

    // ---------------------------------------------------------------- helpers

    /** An agent may only touch their own listings; an admin may touch any. */
    private void assertCanModify(Property property, AppUserDetails actor) {
        if (actor.isAdmin()) {
            return;
        }
        Long ownerId = property.getAgent() == null ? null : property.getAgent().getId();
        if (ownerId == null || !ownerId.equals(actor.getId())) {
            throw new AccessDeniedException("Not the owner of this property");
        }
    }

    private void apply(Property p, PropertyRequest req) {
        p.setTitle(req.title().trim());
        p.setCategory(req.category());
        p.setLocation(req.location().trim());
        p.setCity(blankTo(req.city(), "Trichy"));
        p.setState(blankTo(req.state(), "Tamil Nadu"));
        p.setPrice(req.price());
        p.setPriceLabel(blankTo(req.priceLabel(), PriceFormatter.label(req.price())));
        p.setBedrooms(req.bedrooms());
        p.setBathrooms(req.bathrooms());
        p.setArea(req.area());
        p.setDescription(req.description().trim());
        p.setImageUrl(req.imageUrl() == null || req.imageUrl().isBlank() ? null : req.imageUrl().trim());
        p.setListingType(req.listingType() == null ? ListingType.SALE : req.listingType());
    }

    private Property find(Long id) {
        return propertyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Property not found"));
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /** Maps the frontend's sort dropdown values onto database ordering. "id" is the tie-breaker. */
    private static Sort toSort(String sort) {
        String key = sort == null || sort.isBlank() ? "featured" : sort.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "featured" -> Sort.by(Sort.Order.asc("id"));
            case "low" -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case "high" -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"));
            case "area" -> Sort.by(Sort.Order.desc("area"), Sort.Order.asc("id"));
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            default -> throw new BadRequestException(
                    "Unknown sort '" + sort + "'. Use featured, low, high, area or newest");
        };
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String param) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("all")) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid value '" + value + "' for " + param);
        }
    }
}

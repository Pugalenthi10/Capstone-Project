package com.trichyestates.estatehub.controller;

import com.trichyestates.estatehub.dto.PageResponse;
import com.trichyestates.estatehub.dto.PropertyRequest;
import com.trichyestates.estatehub.dto.PropertyResponse;
import com.trichyestates.estatehub.security.AppUserDetails;
import com.trichyestates.estatehub.service.PropertyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    /**
     * Public. Serves both GET /api/properties and GET /api/properties/search with the same filters.
     * category: apartment|house|flat|all   sort: featured|low|high|area|newest
     * bedrooms / bathrooms mean "at least this many".
     */
    @GetMapping({"", "/search"})
    public PageResponse<PropertyResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String listingType,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) Integer bathrooms,
            @RequestParam(defaultValue = "featured") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return propertyService.search(category, listingType, location, minPrice, maxPrice,
                bedrooms, bathrooms, sort, page, size);
    }

    @GetMapping("/{id}")
    public PropertyResponse get(@PathVariable Long id) {
        return propertyService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<PropertyResponse> create(@Valid @RequestBody PropertyRequest request,
                                                   @AuthenticationPrincipal AppUserDetails me) {
        PropertyResponse created = propertyService.create(request, me.getId());
        return ResponseEntity.created(URI.create("/api/properties/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public PropertyResponse update(@PathVariable Long id, @Valid @RequestBody PropertyRequest request,
                                   @AuthenticationPrincipal AppUserDetails me) {
        return propertyService.update(id, request, me);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me) {
        propertyService.delete(id, me);
    }
}

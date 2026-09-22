package com.trichyestates.estatehub.controller;

import com.trichyestates.estatehub.dto.ApiResponse;
import com.trichyestates.estatehub.dto.PropertyResponse;
import com.trichyestates.estatehub.security.AppUserDetails;
import com.trichyestates.estatehub.service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<PropertyResponse> list(@AuthenticationPrincipal AppUserDetails me) {
        return favoriteService.list(me.getId());
    }

    @PostMapping("/{propertyId}")
    public ResponseEntity<ApiResponse> add(@PathVariable Long propertyId, @AuthenticationPrincipal AppUserDetails me) {
        boolean created = favoriteService.add(me.getId(), propertyId);
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.ok(created ? "Added to favorites" : "Already in favorites"));
    }

    @DeleteMapping("/{propertyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long propertyId, @AuthenticationPrincipal AppUserDetails me) {
        favoriteService.remove(me.getId(), propertyId);
    }
}

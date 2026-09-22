package com.trichyestates.estatehub.controller;

import com.trichyestates.estatehub.dto.ApiResponse;
import com.trichyestates.estatehub.dto.PropertyResponse;
import com.trichyestates.estatehub.security.AppUserDetails;
import com.trichyestates.estatehub.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public List<PropertyResponse> list(@AuthenticationPrincipal AppUserDetails me) {
        return cartService.list(me.getId());
    }

    @PostMapping("/{propertyId}")
    public ResponseEntity<ApiResponse> add(@PathVariable Long propertyId, @AuthenticationPrincipal AppUserDetails me) {
        boolean created = cartService.add(me.getId(), propertyId);
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.ok(created ? "Added to cart" : "Already in cart"));
    }

    @DeleteMapping("/{propertyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long propertyId, @AuthenticationPrincipal AppUserDetails me) {
        cartService.remove(me.getId(), propertyId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal AppUserDetails me) {
        cartService.clear(me.getId());
    }
}

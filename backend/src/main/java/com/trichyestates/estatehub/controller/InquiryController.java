package com.trichyestates.estatehub.controller;

import com.trichyestates.estatehub.dto.BulkInquiryRequest;
import com.trichyestates.estatehub.dto.InquiryRequest;
import com.trichyestates.estatehub.dto.InquiryResponse;
import com.trichyestates.estatehub.dto.InquiryStatusRequest;
import com.trichyestates.estatehub.security.AppUserDetails;
import com.trichyestates.estatehub.service.InquiryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    /** Buyer interest in one property. The buyer is the authenticated user; the body has no userId. */
    @PostMapping
    public ResponseEntity<InquiryResponse> create(@Valid @RequestBody InquiryRequest request,
                                                  @AuthenticationPrincipal AppUserDetails me) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inquiryService.create(me.getId(), request.propertyId(), request.message()));
    }

    /** Used by the cart's "Buy / Contact Seller" button: one enquiry per shortlisted property, atomically. */
    @PostMapping("/bulk")
    public ResponseEntity<List<InquiryResponse>> createBulk(@Valid @RequestBody BulkInquiryRequest request,
                                                            @AuthenticationPrincipal AppUserDetails me) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inquiryService.createBulk(me.getId(), request.propertyIds(), request.message()));
    }

    @GetMapping("/my")
    public List<InquiryResponse> mine(@AuthenticationPrincipal AppUserDetails me) {
        return inquiryService.listMine(me.getId());
    }

    @GetMapping("/agent")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public List<InquiryResponse> forAgent(@AuthenticationPrincipal AppUserDetails me) {
        return inquiryService.listForAgent(me.getId());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<InquiryResponse> all() {
        return inquiryService.listAll();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public InquiryResponse updateStatus(@PathVariable Long id, @Valid @RequestBody InquiryStatusRequest request,
                                        @AuthenticationPrincipal AppUserDetails me) {
        return inquiryService.updateStatus(id, request.status(), me);
    }
}

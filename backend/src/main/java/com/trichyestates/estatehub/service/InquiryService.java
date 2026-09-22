package com.trichyestates.estatehub.service;

import com.trichyestates.estatehub.dto.InquiryResponse;
import com.trichyestates.estatehub.entity.Inquiry;
import com.trichyestates.estatehub.entity.InquiryStatus;
import com.trichyestates.estatehub.entity.Property;
import com.trichyestates.estatehub.entity.User;
import com.trichyestates.estatehub.exception.BadRequestException;
import com.trichyestates.estatehub.exception.ResourceNotFoundException;
import com.trichyestates.estatehub.repository.InquiryRepository;
import com.trichyestates.estatehub.repository.PropertyRepository;
import com.trichyestates.estatehub.repository.UserRepository;
import com.trichyestates.estatehub.security.AppUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** "Purchase interest / contact seller". No payment is taken and no property is marked as sold. */
@Service
@Transactional
public class InquiryService {

    static final String DEFAULT_MESSAGE =
            "I am interested in this property. Please contact me with availability and next steps.";

    private final InquiryRepository inquiryRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public InquiryService(InquiryRepository inquiryRepository, PropertyRepository propertyRepository,
                          UserRepository userRepository) {
        this.inquiryRepository = inquiryRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    public InquiryResponse create(Long userId, Long propertyId, String message) {
        return createBulk(userId, List.of(propertyId), message).get(0);
    }

    /** All-or-nothing: if any property is missing, no enquiry is stored. */
    public List<InquiryResponse> createBulk(Long userId, List<Long> propertyIds, String message) {
        User buyer = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String text = message == null || message.isBlank() ? DEFAULT_MESSAGE : message.trim();

        List<Inquiry> created = new ArrayList<>();
        for (Long propertyId : new LinkedHashSet<>(propertyIds)) {
            Property property = propertyRepository.findById(propertyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Property " + propertyId + " not found"));
            if (property.getAgent() != null && property.getAgent().getId().equals(userId)) {
                throw new BadRequestException("You cannot send an enquiry about your own listing");
            }
            created.add(inquiryRepository.save(new Inquiry(buyer, property, text)));
        }
        return created.stream().map(InquiryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> listMine(Long userId) {
        return inquiryRepository.findByUser(userId).stream().map(InquiryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> listForAgent(Long agentId) {
        return inquiryRepository.findByPropertyAgent(agentId).stream().map(InquiryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> listAll() {
        return inquiryRepository.findAllWithDetails().stream().map(InquiryResponse::from).toList();
    }

    /** The owning agent (or an admin) can move an enquiry through NEW -> CONTACTED -> CLOSED. */
    public InquiryResponse updateStatus(Long inquiryId, InquiryStatus status, AppUserDetails actor) {
        Inquiry inquiry = inquiryRepository.findByIdWithDetails(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found"));
        User owner = inquiry.getProperty().getAgent();
        boolean isOwner = owner != null && owner.getId().equals(actor.getId());
        if (!actor.isAdmin() && !isOwner) {
            throw new AccessDeniedException("Not allowed to update this enquiry");
        }
        inquiry.setStatus(status);
        return InquiryResponse.from(inquiryRepository.save(inquiry));
    }
}

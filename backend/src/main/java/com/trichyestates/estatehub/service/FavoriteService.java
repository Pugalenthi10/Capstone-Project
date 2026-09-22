package com.trichyestates.estatehub.service;

import com.trichyestates.estatehub.dto.PropertyResponse;
import com.trichyestates.estatehub.entity.Favorite;
import com.trichyestates.estatehub.entity.Property;
import com.trichyestates.estatehub.exception.ResourceNotFoundException;
import com.trichyestates.estatehub.repository.FavoriteRepository;
import com.trichyestates.estatehub.repository.PropertyRepository;
import com.trichyestates.estatehub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Database-backed favorites. Every method takes the user id from the authenticated principal, so users only ever see their own rows. */
@Service
@Transactional
public class FavoriteService {

    private final FavoriteRepository repository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public FavoriteService(FavoriteRepository repository, PropertyRepository propertyRepository,
                  UserRepository userRepository) {
        this.repository = repository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> list(Long userId) {
        return repository.findPropertiesByUserId(userId).stream().map(PropertyResponse::from).toList();
    }

    /** Idempotent. Returns true if a new row was created, false if it already existed. */
    public boolean add(Long userId, Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        if (repository.existsByUserIdAndPropertyId(userId, propertyId)) {
            return false;
        }
        repository.save(new Favorite(userRepository.getReferenceById(userId), property));
        return true;
    }

    /** Idempotent: removing something that is not there is not an error. */
    public void remove(Long userId, Long propertyId) {
        repository.deleteByUserAndProperty(userId, propertyId);
    }

    public void clear(Long userId) {
        repository.deleteAllByUser(userId);
    }
}

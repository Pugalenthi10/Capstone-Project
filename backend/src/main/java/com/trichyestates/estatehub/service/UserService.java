package com.trichyestates.estatehub.service;

import com.trichyestates.estatehub.dto.PageResponse;
import com.trichyestates.estatehub.dto.UpdateProfileRequest;
import com.trichyestates.estatehub.dto.UserResponse;
import com.trichyestates.estatehub.entity.Role;
import com.trichyestates.estatehub.entity.User;
import com.trichyestates.estatehub.exception.BadRequestException;
import com.trichyestates.estatehub.exception.DuplicateResourceException;
import com.trichyestates.estatehub.exception.ResourceNotFoundException;
import com.trichyestates.estatehub.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(find(userId));
    }

    /** Name and mobile only. Role, email and password can never be changed here. */
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = find(userId);
        String mobile = req.mobile().trim();
        if (userRepository.existsByMobileAndIdNot(mobile, userId)) {
            throw new DuplicateResourceException("mobile", "An account with this mobile number already exists");
        }
        user.setName(req.name().trim());
        user.setMobile(mobile);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id"));
        return PageResponse.of(userRepository.findAll(pr), UserResponse::from);
    }

    public UserResponse changeRole(Long actingAdminId, Long targetUserId, Role role) {
        if (actingAdminId.equals(targetUserId)) {
            throw new BadRequestException("You cannot change your own role");
        }
        User user = find(targetUserId);
        user.setRole(role);
        return UserResponse.from(userRepository.save(user));
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

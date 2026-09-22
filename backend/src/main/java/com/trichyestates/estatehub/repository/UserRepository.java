package com.trichyestates.estatehub.repository;

import com.trichyestates.estatehub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);
    boolean existsByMobileAndIdNot(String mobile, Long id);
}

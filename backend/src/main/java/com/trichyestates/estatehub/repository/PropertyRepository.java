package com.trichyestates.estatehub.repository;

import com.trichyestates.estatehub.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {
    boolean existsBySeedCode(String seedCode);
}

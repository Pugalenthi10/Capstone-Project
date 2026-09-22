package com.trichyestates.estatehub.repository;

import com.trichyestates.estatehub.entity.Favorite;
import com.trichyestates.estatehub.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserIdAndPropertyId(Long userId, Long propertyId);

    @Query("select x.property from Favorite x where x.user.id = :userId order by x.createdAt desc, x.id desc")
    List<Property> findPropertiesByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from Favorite x where x.user.id = :userId and x.property.id = :propertyId")
    int deleteByUserAndProperty(@Param("userId") Long userId, @Param("propertyId") Long propertyId);

    @Modifying
    @Query("delete from Favorite x where x.user.id = :userId")
    int deleteAllByUser(@Param("userId") Long userId);
}

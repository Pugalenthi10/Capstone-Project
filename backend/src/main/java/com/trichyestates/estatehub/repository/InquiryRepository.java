package com.trichyestates.estatehub.repository;

import com.trichyestates.estatehub.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    String FETCH = "select i from Inquiry i join fetch i.user join fetch i.property p ";
    String ORDER = " order by i.createdAt desc, i.id desc";

    @Query(FETCH + "where i.user.id = :userId" + ORDER)
    List<Inquiry> findByUser(@Param("userId") Long userId);

    @Query(FETCH + "where p.agent.id = :agentId" + ORDER)
    List<Inquiry> findByPropertyAgent(@Param("agentId") Long agentId);

    @Query(FETCH + ORDER)
    List<Inquiry> findAllWithDetails();

    @Query(FETCH + "where i.id = :id")
    Optional<Inquiry> findByIdWithDetails(@Param("id") Long id);
}

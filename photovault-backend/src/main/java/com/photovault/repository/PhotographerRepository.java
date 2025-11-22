package com.photovault.repository;

import com.photovault.entity.Photographer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhotographerRepository extends JpaRepository<Photographer, UUID> {

    Optional<Photographer> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Photographer> findByCustomDomain(String customDomain);

    @Query("SELECT p FROM Photographer p WHERE p.email = :email AND p.deletedAt IS NULL")
    Optional<Photographer> findActiveByEmail(String email);

    @Query("SELECT COUNT(p) FROM Photographer p WHERE p.plan = :plan AND p.deletedAt IS NULL")
    long countByPlan(String plan);
}

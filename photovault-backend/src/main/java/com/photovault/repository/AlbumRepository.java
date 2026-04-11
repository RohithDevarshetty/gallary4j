package com.photovault.repository;

import com.photovault.entity.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlbumRepository extends JpaRepository<Album, UUID> {

    Optional<Album> findBySlug(String slug);

    @Query("SELECT a FROM Album a WHERE a.photographer.id = :photographerId AND a.deletedAt IS NULL ORDER BY a.createdAt DESC")
    Page<Album> findByPhotographerId(UUID photographerId, Pageable pageable);

    @Query("SELECT a FROM Album a WHERE a.slug = :slug AND a.deletedAt IS NULL")
    Optional<Album> findActiveBySlug(String slug);

    @Query("SELECT a FROM Album a WHERE a.photographer.id = :photographerId AND a.isActive = true AND a.deletedAt IS NULL")
    List<Album> findActiveAlbumsByPhotographer(UUID photographerId);

    @Query("SELECT a FROM Album a WHERE a.deletedAt IS NULL ORDER BY a.viewCount DESC")
    List<Album> findMostViewedAlbums(Pageable pageable);

    @Query("SELECT COUNT(a) FROM Album a WHERE a.photographer.id = :photographerId AND a.deletedAt IS NULL")
    long countByPhotographerId(UUID photographerId);

    boolean existsBySlug(String slug);
}

package com.photovault.repository;

import com.photovault.entity.Selection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SelectionRepository extends JpaRepository<Selection, UUID> {

    @Query("SELECT s FROM Selection s WHERE s.albumId = :albumId")
    List<Selection> findByAlbumId(UUID albumId);

    @Query("SELECT s FROM Selection s WHERE s.albumId = :albumId AND s.clientEmail = :clientEmail")
    List<Selection> findByAlbumIdAndClientEmail(UUID albumId, String clientEmail);

    @Query("SELECT s FROM Selection s WHERE s.albumId = :albumId AND s.mediaId = :mediaId AND s.clientEmail = :clientEmail")
    Optional<Selection> findByAlbumIdAndMediaIdAndClientEmail(UUID albumId, UUID mediaId, String clientEmail);

    @Query("SELECT COUNT(s) FROM Selection s WHERE s.albumId = :albumId")
    long countByAlbumId(UUID albumId);

    @Query("SELECT COUNT(s) FROM Selection s WHERE s.albumId = :albumId AND s.clientEmail = :clientEmail")
    long countByAlbumIdAndClientEmail(UUID albumId, String clientEmail);

    boolean existsByAlbumIdAndMediaIdAndClientEmail(UUID albumId, UUID mediaId, String clientEmail);
}

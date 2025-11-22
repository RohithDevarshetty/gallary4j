package com.photovault.repository;

import com.photovault.entity.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {

    @Query("SELECT m FROM Media m WHERE m.album.id = :albumId AND m.deletedAt IS NULL ORDER BY m.sortOrder ASC, m.uploadedAt ASC")
    Page<Media> findByAlbumId(UUID albumId, Pageable pageable);

    @Query("SELECT m FROM Media m WHERE m.album.id = :albumId AND m.deletedAt IS NULL ORDER BY m.sortOrder ASC, m.uploadedAt ASC")
    List<Media> findAllByAlbumId(UUID albumId);

    @Query("SELECT m FROM Media m WHERE m.processingStatus = :status")
    List<Media> findByProcessingStatus(Media.ProcessingStatus status);

    @Query("SELECT COUNT(m) FROM Media m WHERE m.album.id = :albumId AND m.deletedAt IS NULL")
    long countByAlbumId(UUID albumId);

    @Query("SELECT SUM(m.fileSizeBytes) FROM Media m WHERE m.photographer.id = :photographerId AND m.deletedAt IS NULL")
    Long getTotalStorageByPhotographer(UUID photographerId);

    @Query("SELECT m FROM Media m WHERE m.photographer.id = :photographerId AND m.processingStatus = 'PENDING' ORDER BY m.uploadedAt ASC")
    List<Media> findPendingMediaByPhotographer(UUID photographerId, Pageable pageable);
}

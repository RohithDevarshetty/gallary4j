package com.photovault.repository;

import com.photovault.entity.UploadSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {

    @Query("SELECT s FROM UploadSession s WHERE s.status = 'ACTIVE' AND s.expiresAt < :now")
    List<UploadSession> findExpiredSessions(Instant now);

    @Query("SELECT s FROM UploadSession s WHERE s.photographer.id = :photographerId ORDER BY s.createdAt DESC")
    List<UploadSession> findByPhotographerId(UUID photographerId);

    @Query("SELECT s FROM UploadSession s WHERE s.albumId = :albumId ORDER BY s.createdAt DESC")
    List<UploadSession> findByAlbumId(UUID albumId);
}

package com.photovault.repository;

import com.photovault.entity.AnalyticsEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    /**
     * Find events by album
     */
    Page<AnalyticsEvent> findByAlbumIdOrderByCreatedAtDesc(UUID albumId, Pageable pageable);

    /**
     * Find events by photographer
     */
    Page<AnalyticsEvent> findByPhotographerIdOrderByCreatedAtDesc(UUID photographerId, Pageable pageable);

    /**
     * Find events by album and type
     */
    List<AnalyticsEvent> findByAlbumIdAndEventType(UUID albumId, AnalyticsEvent.EventType eventType);

    /**
     * Count events by album and type
     */
    long countByAlbumIdAndEventType(UUID albumId, AnalyticsEvent.EventType eventType);

    /**
     * Count events by album within date range
     */
    @Query("SELECT COUNT(e) FROM AnalyticsEvent e WHERE e.albumId = :albumId " +
           "AND e.createdAt BETWEEN :startDate AND :endDate")
    long countByAlbumAndDateRange(
        @Param("albumId") UUID albumId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Get unique visitors by album (distinct IPs)
     */
    @Query("SELECT COUNT(DISTINCT e.clientIp) FROM AnalyticsEvent e " +
           "WHERE e.albumId = :albumId AND e.eventType = 'VIEW'")
    long countUniqueVisitorsByAlbum(@Param("albumId") UUID albumId);

    /**
     * Get album statistics grouped by event type
     */
    @Query("SELECT e.eventType as eventType, COUNT(e) as count " +
           "FROM AnalyticsEvent e WHERE e.albumId = :albumId " +
           "GROUP BY e.eventType")
    List<Map<String, Object>> getAlbumStatsByType(@Param("albumId") UUID albumId);

    /**
     * Get daily statistics for album
     */
    @Query("SELECT DATE(e.createdAt) as date, e.eventType as eventType, COUNT(e) as count " +
           "FROM AnalyticsEvent e WHERE e.albumId = :albumId " +
           "AND e.createdAt >= :startDate " +
           "GROUP BY DATE(e.createdAt), e.eventType " +
           "ORDER BY DATE(e.createdAt) DESC")
    List<Map<String, Object>> getDailyStatsByAlbum(
        @Param("albumId") UUID albumId,
        @Param("startDate") Instant startDate
    );

    /**
     * Get popular media by views
     */
    @Query("SELECT e.mediaId as mediaId, COUNT(e) as views " +
           "FROM AnalyticsEvent e WHERE e.albumId = :albumId " +
           "AND e.eventType = 'VIEW' AND e.mediaId IS NOT NULL " +
           "GROUP BY e.mediaId " +
           "ORDER BY COUNT(e) DESC")
    List<Map<String, Object>> getPopularMediaByAlbum(@Param("albumId") UUID albumId, Pageable pageable);

    /**
     * Get most downloaded media
     */
    @Query("SELECT e.mediaId as mediaId, COUNT(e) as downloads " +
           "FROM AnalyticsEvent e WHERE e.albumId = :albumId " +
           "AND e.eventType = 'DOWNLOAD' AND e.mediaId IS NOT NULL " +
           "GROUP BY e.mediaId " +
           "ORDER BY COUNT(e) DESC")
    List<Map<String, Object>> getMostDownloadedMedia(@Param("albumId") UUID albumId, Pageable pageable);

    /**
     * Delete old events (for cleanup jobs)
     */
    void deleteByCreatedAtBefore(Instant date);
}

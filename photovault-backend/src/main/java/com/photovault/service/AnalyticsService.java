package com.photovault.service;

import com.photovault.dto.AlbumAnalyticsDTO;
import com.photovault.entity.AnalyticsEvent;
import com.photovault.repository.AnalyticsEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AnalyticsEventRepository analyticsEventRepository;

    /**
     * Track album view event
     */
    @Async
    @Transactional
    public void trackAlbumView(UUID albumId, UUID photographerId, HttpServletRequest request) {
        trackEvent(AnalyticsEvent.EventType.VIEW, albumId, null, photographerId, request, null);
        log.debug("Tracked album view: {}", albumId);
    }

    /**
     * Track media view event
     */
    @Async
    @Transactional
    public void trackMediaView(UUID albumId, UUID mediaId, UUID photographerId, HttpServletRequest request) {
        trackEvent(AnalyticsEvent.EventType.VIEW, albumId, mediaId, photographerId, request, null);
        log.debug("Tracked media view: {}", mediaId);
    }

    /**
     * Track media download event
     */
    @Async
    @Transactional
    public void trackMediaDownload(UUID albumId, UUID mediaId, UUID photographerId, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("downloadedAt", Instant.now().toString());
        trackEvent(AnalyticsEvent.EventType.DOWNLOAD, albumId, mediaId, photographerId, request, details);
        log.debug("Tracked media download: {}", mediaId);
    }

    /**
     * Track album share event
     */
    @Async
    @Transactional
    public void trackAlbumShare(UUID albumId, UUID photographerId, String shareMethod, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("shareMethod", shareMethod);
        trackEvent(AnalyticsEvent.EventType.SHARE, albumId, null, photographerId, request, details);
        log.debug("Tracked album share: {} via {}", albumId, shareMethod);
    }

    /**
     * Track media favorite event
     */
    @Async
    @Transactional
    public void trackMediaFavorite(UUID albumId, UUID mediaId, UUID photographerId, String clientEmail, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("clientEmail", clientEmail);
        trackEvent(AnalyticsEvent.EventType.FAVORITE, albumId, mediaId, photographerId, request, details);
        log.debug("Tracked media favorite: {} by {}", mediaId, clientEmail);
    }

    /**
     * Track media upload event
     */
    @Async
    @Transactional
    public void trackMediaUpload(UUID albumId, UUID mediaId, UUID photographerId, long fileSizeBytes) {
        Map<String, Object> details = new HashMap<>();
        details.put("fileSizeBytes", fileSizeBytes);
        trackEvent(AnalyticsEvent.EventType.UPLOAD, albumId, mediaId, photographerId, null, details);
        log.debug("Tracked media upload: {}", mediaId);
    }

    /**
     * Track album created event
     */
    @Async
    @Transactional
    public void trackAlbumCreated(UUID albumId, UUID photographerId) {
        trackEvent(AnalyticsEvent.EventType.ALBUM_CREATED, albumId, null, photographerId, null, null);
        log.debug("Tracked album created: {}", albumId);
    }

    /**
     * Track album deleted event
     */
    @Async
    @Transactional
    public void trackAlbumDeleted(UUID albumId, UUID photographerId) {
        trackEvent(AnalyticsEvent.EventType.ALBUM_DELETED, albumId, null, photographerId, null, null);
        log.debug("Tracked album deleted: {}", albumId);
    }

    /**
     * Generic event tracking method
     */
    private void trackEvent(AnalyticsEvent.EventType eventType, UUID albumId, UUID mediaId,
                           UUID photographerId, HttpServletRequest request, Map<String, Object> details) {
        AnalyticsEvent.AnalyticsEventBuilder builder = AnalyticsEvent.builder()
                .eventType(eventType)
                .albumId(albumId)
                .mediaId(mediaId)
                .photographerId(photographerId)
                .details(details);

        if (request != null) {
            builder.clientIp(getClientIp(request))
                   .userAgent(request.getHeader("User-Agent"))
                   .referer(request.getHeader("Referer"));
        }

        AnalyticsEvent event = builder.build();
        analyticsEventRepository.save(event);
    }

    /**
     * Get album analytics summary
     */
    @Transactional(readOnly = true)
    public AlbumAnalyticsDTO getAlbumAnalytics(UUID albumId) {
        long totalViews = analyticsEventRepository.countByAlbumIdAndEventType(
                albumId, AnalyticsEvent.EventType.VIEW);
        long totalDownloads = analyticsEventRepository.countByAlbumIdAndEventType(
                albumId, AnalyticsEvent.EventType.DOWNLOAD);
        long totalShares = analyticsEventRepository.countByAlbumIdAndEventType(
                albumId, AnalyticsEvent.EventType.SHARE);
        long totalFavorites = analyticsEventRepository.countByAlbumIdAndEventType(
                albumId, AnalyticsEvent.EventType.FAVORITE);
        long uniqueVisitors = analyticsEventRepository.countUniqueVisitorsByAlbum(albumId);

        // Get last 7 days views
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        long recentViews = analyticsEventRepository.countByAlbumAndDateRange(
                albumId, sevenDaysAgo, Instant.now());

        // Get popular media
        var popularMedia = analyticsEventRepository.getPopularMediaByAlbum(
                albumId, PageRequest.of(0, 10));

        return AlbumAnalyticsDTO.builder()
                .albumId(albumId)
                .totalViews(totalViews)
                .totalDownloads(totalDownloads)
                .totalShares(totalShares)
                .totalFavorites(totalFavorites)
                .uniqueVisitors(uniqueVisitors)
                .recentViews(recentViews)
                .popularMedia(popularMedia)
                .build();
    }

    /**
     * Get photographer analytics summary
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPhotographerAnalytics(UUID photographerId, int days) {
        Instant startDate = Instant.now().minus(days, ChronoUnit.DAYS);

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("photographerId", photographerId);
        analytics.put("period", days + " days");

        // Total events by type
        for (AnalyticsEvent.EventType eventType : AnalyticsEvent.EventType.values()) {
            long count = analyticsEventRepository.countByAlbumIdAndEventType(null, eventType);
            analytics.put(eventType.name().toLowerCase() + "s", count);
        }

        return analytics;
    }

    /**
     * Extract client IP from request (handles proxies)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs (take first one)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Cleanup old analytics events (scheduled job)
     */
    @Transactional
    public void cleanupOldEvents(int retentionDays) {
        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        analyticsEventRepository.deleteByCreatedAtBefore(cutoffDate);
        log.info("Cleaned up analytics events older than {} days", retentionDays);
    }
}

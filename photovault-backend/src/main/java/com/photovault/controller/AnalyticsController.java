package com.photovault.controller;

import com.photovault.dto.AlbumAnalyticsDTO;
import com.photovault.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get album analytics
     * GET /api/v1/analytics/albums/{albumId}
     */
    @GetMapping("/albums/{albumId}")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<AlbumAnalyticsDTO> getAlbumAnalytics(@PathVariable UUID albumId) {
        log.debug("Fetching analytics for album: {}", albumId);
        AlbumAnalyticsDTO analytics = analyticsService.getAlbumAnalytics(albumId);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get photographer analytics
     * GET /api/v1/analytics/photographer
     */
    @GetMapping("/photographer")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<Map<String, Object>> getPhotographerAnalytics(
            @RequestParam(defaultValue = "30") int days) {
        // TODO: Get photographer ID from authenticated user
        UUID photographerId = UUID.randomUUID();  // Placeholder
        Map<String, Object> analytics = analyticsService.getPhotographerAnalytics(photographerId, days);
        return ResponseEntity.ok(analytics);
    }
}

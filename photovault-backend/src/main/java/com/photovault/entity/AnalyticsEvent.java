package com.photovault.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "analytics_events", indexes = {
    @Index(name = "idx_analytics_album_date", columnList = "album_id,created_at"),
    @Index(name = "idx_analytics_photographer", columnList = "photographer_id,created_at"),
    @Index(name = "idx_analytics_event_type", columnList = "event_type,created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    // Related entities
    @Column(name = "album_id")
    private UUID albumId;

    @Column(name = "media_id")
    private UUID mediaId;

    @Column(name = "photographer_id")
    private UUID photographerId;

    // Client info
    @Column(name = "client_ip", columnDefinition = "inet")
    private String clientIp;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "referer", columnDefinition = "TEXT")
    private String referer;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    // Event details (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum EventType {
        VIEW,           // Album or media viewed
        DOWNLOAD,       // Media downloaded
        SHARE,          // Album shared
        FAVORITE,       // Media added to favorites
        UPLOAD,         // Media uploaded
        ALBUM_CREATED,  // Album created
        ALBUM_DELETED   // Album deleted
    }
}

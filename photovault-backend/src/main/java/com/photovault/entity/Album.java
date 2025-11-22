package com.photovault.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "albums", indexes = {
    @Index(name = "idx_albums_photographer", columnList = "photographer_id"),
    @Index(name = "idx_albums_slug", columnList = "slug", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photographer_id", nullable = false)
    private Photographer photographer;

    // Basic Info
    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "event_date")
    private LocalDate eventDate;

    // Client Info
    @Column(name = "client_name")
    private String clientName;

    @Column(name = "client_email")
    private String clientEmail;

    @Column(name = "client_phone", length = 20)
    private String clientPhone;

    // Security
    @Column(name = "password_hash")
    private String passwordHash;

    @Builder.Default
    @Column(name = "requires_password")
    private Boolean requiresPassword = false;

    // Settings
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_public")
    private Boolean isPublic = true;

    @Builder.Default
    @Column(name = "allow_downloads")
    private Boolean allowDownloads = true;

    @Builder.Default
    @Column(name = "allow_sharing")
    private Boolean allowSharing = true;

    @Builder.Default
    @Column(name = "enable_selection")
    private Boolean enableSelection = false;

    @Column(name = "max_selections")
    private Integer maxSelections;

    @Builder.Default
    @Column(name = "watermark_photos")
    private Boolean watermarkPhotos = false;

    // Analytics
    @Builder.Default
    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Builder.Default
    @Column(name = "unique_visitors")
    private Integer uniqueVisitors = 0;

    @Builder.Default
    @Column(name = "total_downloads")
    private Integer totalDownloads = 0;

    @Column(name = "last_viewed_at")
    private Instant lastViewedAt;

    // Metadata
    @Builder.Default
    @Column(name = "media_count")
    private Integer mediaCount = 0;

    @Builder.Default
    @Column(name = "total_size_bytes")
    private Long totalSizeBytes = 0L;

    @Column(name = "cover_photo_id")
    private UUID coverPhotoId;

    // Organization
    @ElementCollection
    @CollectionTable(name = "album_tags", joinColumns = @JoinColumn(name = "album_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(length = 100)
    private String category;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (slug == null || slug.isEmpty()) {
            slug = generateSlug(title);
        }
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "")
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

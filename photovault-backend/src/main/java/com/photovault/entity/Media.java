package com.photovault.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "media", indexes = {
    @Index(name = "idx_media_album", columnList = "album_id"),
    @Index(name = "idx_media_processing", columnList = "processing_status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photographer_id", nullable = false)
    private Photographer photographer;

    // File Info
    @Column(nullable = false, length = 500)
    private String filename;

    @Column(name = "original_filename", length = 500)
    private String originalFilename;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    // Storage URLs
    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "optimized_url", columnDefinition = "TEXT")
    private String optimizedUrl;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "preview_url", columnDefinition = "TEXT")
    private String previewUrl;

    // Video specific
    @Column(name = "video_thumbnail_url", columnDefinition = "TEXT")
    private String videoThumbnailUrl;

    @Column(name = "video_duration_seconds")
    private Integer videoDurationSeconds;

    @Column(name = "video_codec", length = 50)
    private String videoCodec;

    // Image Metadata
    private Integer width;
    private Integer height;

    @Column(name = "aspect_ratio", precision = 5, scale = 2)
    private BigDecimal aspectRatio;

    @Column(length = 20)
    private String orientation;

    // EXIF Data
    @Column(name = "camera_make", length = 100)
    private String cameraMake;

    @Column(name = "camera_model", length = 100)
    private String cameraModel;

    @Column(name = "lens_model", length = 100)
    private String lensModel;

    @Column(name = "focal_length")
    private Integer focalLength;

    @Column(precision = 3, scale = 1)
    private BigDecimal aperture;

    @Column(name = "shutter_speed", length = 20)
    private String shutterSpeed;

    private Integer iso;

    @Column(name = "taken_at")
    private Instant takenAt;

    @Column(name = "gps_latitude", precision = 10, scale = 8)
    private BigDecimal gpsLatitude;

    @Column(name = "gps_longitude", precision = 11, scale = 8)
    private BigDecimal gpsLongitude;

    // AI Detection
    @Builder.Default
    @Column(name = "faces_detected")
    private Integer facesDetected = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "face_data", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> faceData = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "media_auto_tags", joinColumns = @JoinColumn(name = "media_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tagsAuto = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "media_color_palette", joinColumns = @JoinColumn(name = "media_id"))
    @Column(name = "color", length = 7)
    @Builder.Default
    private List<String> colorPalette = new ArrayList<>();

    // Organization
    @Builder.Default
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "is_cover")
    private Boolean isCover = false;

    @Builder.Default
    @Column(name = "is_hidden")
    private Boolean isHidden = false;

    // Analytics
    @Builder.Default
    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Builder.Default
    @Column(name = "download_count")
    private Integer downloadCount = 0;

    // Status
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "processing_status", length = 50)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    // Blur hash for lazy loading
    @Column(name = "blur_hash", length = 50)
    private String blurHash;

    // Timestamps
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant uploadedAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}

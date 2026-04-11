package com.photovault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaDTO {
    private UUID id;
    private UUID albumId;
    private String filename;
    private String originalFilename;
    private String mimeType;
    private Long fileSizeBytes;
    private String originalUrl;
    private String optimizedUrl;
    private String thumbnailUrl;
    private String previewUrl;
    private String videoThumbnailUrl;
    private Integer videoDurationSeconds;
    private Integer width;
    private Integer height;
    private BigDecimal aspectRatio;
    private String orientation;
    private String cameraMake;
    private String cameraModel;
    private Integer focalLength;
    private BigDecimal aperture;
    private Integer iso;
    private Instant takenAt;
    private List<String> tagsAuto;
    private List<String> colorPalette;
    private Integer sortOrder;
    private Boolean isCover;
    private Integer viewCount;
    private Integer downloadCount;
    private String processingStatus;
    private String blurHash;
    private Instant uploadedAt;
    private Instant processedAt;
}

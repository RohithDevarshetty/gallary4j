package com.photovault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlbumDTO {
    private UUID id;
    private String title;
    private String description;
    private String slug;
    private LocalDate eventDate;
    private String clientName;
    private String clientEmail;
    private Boolean requiresPassword;
    private Instant expiresAt;
    private Boolean isActive;
    private Boolean isPublic;
    private Boolean allowDownloads;
    private Boolean allowSharing;
    private Boolean enableSelection;
    private Integer maxSelections;
    private Boolean watermarkPhotos;
    private Integer viewCount;
    private Integer uniqueVisitors;
    private Integer totalDownloads;
    private Integer mediaCount;
    private Long totalSizeBytes;
    private UUID coverPhotoId;
    private String coverPhotoUrl;
    private List<String> tags;
    private String category;
    private Instant createdAt;
    private Instant publishedAt;
}

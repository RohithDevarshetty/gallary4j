package com.photovault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadSessionDTO {
    private UUID id;
    private UUID albumId;
    private Integer totalFiles;
    private Integer uploadedFiles;
    private Long totalBytes;
    private Long uploadedBytes;
    private String status;
    private Instant expiresAt;
    private Instant completedAt;
    private List<PresignedUrlDTO> presignedUrls;
    private Double progress;
}

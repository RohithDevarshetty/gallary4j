package com.photovault.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadSessionCreateRequest {
    @NotNull(message = "Album ID is required")
    private UUID albumId;

    @NotNull(message = "Total files is required")
    @Min(value = 1, message = "At least one file is required")
    private Integer totalFiles;

    @Min(value = 0, message = "Total bytes cannot be negative")
    private Long totalBytes;

    private String clientType;  // web, ios, android
    private String clientVersion;
    private List<String> fileNames;  // For presigned URLs
}

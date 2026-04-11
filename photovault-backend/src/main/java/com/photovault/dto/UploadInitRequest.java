package com.photovault.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class UploadInitRequest {

    @NotNull
    private UUID albumId;

    @NotNull
    @Positive
    private Integer fileCount;

    @NotNull
    @Positive
    private Long totalSize;

    @NotNull
    private List<FileInfo> files;

    private String clientType;
    private String clientVersion;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private String filename;
        private String mimeType;
        private Long size;
    }
}

package com.photovault.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadChunkRequest {
    @NotNull(message = "Chunk number is required")
    @Min(value = 1, message = "Chunk number must be at least 1")
    private Integer chunkNumber;

    @NotNull(message = "Total chunks is required")
    @Min(value = 1, message = "Total chunks must be at least 1")
    private Integer totalChunks;

    @NotNull(message = "Filename is required")
    private String filename;

    private Long chunkSize;
}

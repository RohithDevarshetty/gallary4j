package com.photovault.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ProcessingJobDTO {
    private UUID id;
    private UUID mediaId;
    private UUID sessionId;
    private UUID albumId;
    private String filename;
    private String mimeType;
    private String jobType;
    private String status;
    private Integer attemptCount;
    private Integer maxAttempts;
    private Instant queuedAt;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    /** Processing duration in milliseconds, null if not yet complete. */
    private Long durationMs;
}

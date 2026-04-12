package com.photovault.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BulkJobStatusDTO {

    private UUID sessionId;
    private UUID albumId;

    // Aggregate counts
    private int total;
    private int queued;
    private int processing;
    private int completed;
    private int failed;
    private int retrying;

    /** 0–100 */
    private double progressPercent;

    /** True when all jobs are terminal (COMPLETED or FAILED with no retries left). */
    private boolean done;

    private Instant startedAt;
    private Instant completedAt;

    private List<ProcessingJobDTO> jobs;
}

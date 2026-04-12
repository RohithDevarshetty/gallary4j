package com.photovault.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processing_jobs", indexes = {
    @Index(name = "idx_pjob_session", columnList = "session_id"),
    @Index(name = "idx_pjob_status",  columnList = "status,queued_at"),
    @Index(name = "idx_pjob_album",   columnList = "album_id,status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    /** Null for single-file uploads not tied to a session. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private UploadSession session;

    @Column(name = "photographer_id", nullable = false)
    private UUID photographerId;

    @Column(name = "album_id", nullable = false)
    private UUID albumId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 20)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.QUEUED;

    @Builder.Default
    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @Builder.Default
    @Column(name = "max_attempts")
    private Integer maxAttempts = 3;

    @Builder.Default
    @Column(name = "queued_at", nullable = false, updatable = false)
    private Instant queuedAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "kafka_topic", length = 100)
    private String kafkaTopic;

    @Column(name = "kafka_partition")
    private Integer kafkaPartition;

    @Column(name = "kafka_offset")
    private Long kafkaOffset;

    public enum JobType {
        IMAGE, VIDEO, THUMBNAIL
    }

    public enum JobStatus {
        QUEUED, PROCESSING, COMPLETED, FAILED, RETRYING
    }

    public boolean canRetry() {
        return attemptCount < maxAttempts
            && (status == JobStatus.FAILED || status == JobStatus.RETRYING);
    }
}

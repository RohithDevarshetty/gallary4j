package com.photovault.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "upload_sessions", indexes = {
    @Index(name = "idx_upload_session_status", columnList = "status,expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photographer_id", nullable = false)
    private Photographer photographer;

    @Column(name = "album_id", nullable = false)
    private UUID albumId;

    // Session info
    @Column(name = "total_files", nullable = false)
    private Integer totalFiles;

    @Builder.Default
    @Column(name = "uploaded_files")
    private Integer uploadedFiles = 0;

    @Column(name = "total_bytes", nullable = false)
    private Long totalBytes;

    @Builder.Default
    @Column(name = "uploaded_bytes")
    private Long uploadedBytes = 0L;

    // Status
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 50)
    private Status status = Status.ACTIVE;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Metadata
    @Column(name = "client_type", length = 50)
    private String clientType;

    @Column(name = "client_version", length = 20)
    private String clientVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    @Builder.Default
    private Instant expiresAt = Instant.now().plus(Duration.ofHours(24));

    @Column(name = "completed_at")
    private Instant completedAt;

    public enum Status {
        ACTIVE,
        COMPLETED,
        FAILED,
        EXPIRED
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public double getProgress() {
        if (totalFiles == 0) return 0.0;
        return (double) uploadedFiles / totalFiles * 100.0;
    }
}

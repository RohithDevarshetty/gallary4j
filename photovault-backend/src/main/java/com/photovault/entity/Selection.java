package com.photovault.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "selections",
    uniqueConstraints = @UniqueConstraint(columnNames = {"album_id", "media_id", "client_email"}),
    indexes = {
        @Index(name = "idx_selections_album", columnList = "album_id"),
        @Index(name = "idx_selections_client", columnList = "client_email")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Selection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "album_id", nullable = false)
    private UUID albumId;

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;

    // Client identification
    @Column(name = "client_email")
    private String clientEmail;

    @Column(name = "client_session_id")
    private String clientSessionId;

    @Column(name = "client_ip")
    private String clientIp;

    // Selection details
    @Column(name = "selected_at")
    @Builder.Default
    private Instant selectedAt = Instant.now();

    @Column(name = "selection_type", length = 50)
    @Builder.Default
    private String selectionType = "favorite";

    @Column(columnDefinition = "TEXT")
    private String notes;
}

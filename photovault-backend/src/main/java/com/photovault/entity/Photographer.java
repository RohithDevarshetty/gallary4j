package com.photovault.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "photographers", indexes = {
    @Index(name = "idx_photographer_email", columnList = "email"),
    @Index(name = "idx_photographer_plan", columnList = "plan")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Photographer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "studio_name")
    private String studioName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String phone;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    // Subscription
    @Builder.Default
    @Column(length = 50)
    private String plan = "trial";

    @Column(name = "plan_expires_at")
    private Instant planExpiresAt;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    // Limits
    @Builder.Default
    @Column(name = "storage_used_bytes")
    private Long storageUsedBytes = 0L;

    @Builder.Default
    @Column(name = "storage_limit_bytes")
    private Long storageLimitBytes = 10737418240L; // 10GB

    @Builder.Default
    @Column(name = "albums_count")
    private Integer albumsCount = 0;

    @Builder.Default
    @Column(name = "albums_limit")
    private Integer albumsLimit = 10;

    // Branding
    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "watermark_url", columnDefinition = "TEXT")
    private String watermarkUrl;

    @Column(name = "custom_domain", unique = true)
    private String customDomain;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "brand_colors", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> brandColors = new HashMap<>();

    // Settings
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> settings = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notification_preferences", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> notificationPreferences = new HashMap<>();

    // Metadata
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

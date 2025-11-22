# gallary4j



# PhotoVault - Enterprise Photo/Video Gallery Platform

## Executive Summary
Enterprise-grade photo and video gallery platform built for scale, serving photographers from freelancers to large studios. Architecture designed to handle 100,000+ concurrent users with native iOS/Android apps and web galleries.

## System Architecture

### Core Components
```yaml
API Backend: Spring Boot 3.2 (Java 21)
Admin Dashboard: Next.js 14 with TypeScript  
Client Gallery: Next.js with SSG/ISR
iOS App: Swift 5.9 / SwiftUI 5
Android App: Kotlin (Phase 2)
Database: PostgreSQL 16 with read replicas
Cache: Hazelcast IMDG (distributed)
Storage: Cloudflare R2 + Global CDN
Message Queue: RabbitMQ (AMQP)
Orchestration: Kubernetes (AWS EKS/GCP GKE)
Service Mesh: Istio (optional)
Monitoring: Prometheus + Grafana
Logging: ELK Stack
```

### Architecture Diagram
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   iOS App   │     │ Android App │     │Web Gallery  │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                    │
       └───────────────────┴────────────────────┘
                           │
                    ┌──────▼──────┐
                    │ Kong/Nginx  │ API Gateway
                    │Load Balancer│
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐      ┌──────▼──────┐    ┌─────▼─────┐
   │Spring   │      │Spring Boot  │    │Next.js    │
   │Boot API │◄────►│Media Service│◄──►│SSR/API    │
   └────┬────┘      └──────┬──────┘    └───────────┘
        │                  │
        │           ┌──────▼──────┐
        │           │  RabbitMQ   │
        │           │Message Queue│
        │           └──────┬──────┘
        │                  │
   ┌────▼────────────────────▼────┐
   │                              │
   │  Hazelcast Distributed Cache │
   │                              │
   └────┬────────────────────┬────┘
        │                    │
   ┌────▼────┐          ┌────▼────┐
   │PostgreSQL│         │PostgreSQL│
   │  Master  │────────►│ Read     │
   └─────────┘         │ Replicas │
                       └──────────┘
```

## Database Schema

### PostgreSQL Schema Design
```sql
-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_partman";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Photographers/Studios table
CREATE TABLE photographers (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    studio_name VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    country_code VARCHAR(2),
    
    -- Subscription
    plan VARCHAR(50) DEFAULT 'trial', -- trial, starter, pro, studio, enterprise
    plan_expires_at TIMESTAMP WITH TIME ZONE,
    stripe_customer_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    
    -- Limits
    storage_used_bytes BIGINT DEFAULT 0,
    storage_limit_bytes BIGINT DEFAULT 10737418240, -- 10GB default
    albums_count INTEGER DEFAULT 0,
    albums_limit INTEGER DEFAULT 10,
    
    -- Branding
    logo_url TEXT,
    watermark_url TEXT,
    custom_domain VARCHAR(255) UNIQUE,
    brand_colors JSONB DEFAULT '{}',
    
    -- Settings
    settings JSONB DEFAULT '{}',
    notification_preferences JSONB DEFAULT '{}',
    
    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    -- Indexes
    INDEX idx_photographer_email (email),
    INDEX idx_photographer_plan (plan),
    INDEX idx_photographer_deleted (deleted_at)
);

-- Albums/Galleries table (partitioned by month)
CREATE TABLE albums (
    id UUID DEFAULT uuid_generate_v4(),
    photographer_id UUID NOT NULL REFERENCES photographers(id),
    
    -- Basic Info
    title VARCHAR(500) NOT NULL,
    description TEXT,
    slug VARCHAR(255) UNIQUE NOT NULL,
    event_date DATE,
    
    -- Client Info
    client_name VARCHAR(255),
    client_email VARCHAR(255),
    client_phone VARCHAR(20),
    
    -- Security
    password_hash VARCHAR(255), -- Optional PIN/password
    requires_password BOOLEAN DEFAULT false,
    
    -- Settings
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true,
    is_public BOOLEAN DEFAULT true,
    allow_downloads BOOLEAN DEFAULT true,
    allow_sharing BOOLEAN DEFAULT true,
    enable_selection BOOLEAN DEFAULT false,
    max_selections INTEGER,
    watermark_photos BOOLEAN DEFAULT false,
    
    -- Analytics
    view_count INTEGER DEFAULT 0,
    unique_visitors INTEGER DEFAULT 0,
    total_downloads INTEGER DEFAULT 0,
    last_viewed_at TIMESTAMP WITH TIME ZONE,
    
    -- Metadata
    media_count INTEGER DEFAULT 0,
    total_size_bytes BIGINT DEFAULT 0,
    cover_photo_id UUID,
    
    -- Organization
    tags TEXT[],
    category VARCHAR(100),
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create monthly partitions
CREATE TABLE albums_2024_01 PARTITION OF albums
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
-- Auto-create future partitions with pg_partman

-- Media table (photos & videos)
CREATE TABLE media (
    id UUID DEFAULT uuid_generate_v4(),
    album_id UUID NOT NULL,
    photographer_id UUID NOT NULL REFERENCES photographers(id),
    
    -- File Info
    filename VARCHAR(500) NOT NULL,
    original_filename VARCHAR(500),
    mime_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    
    -- Storage URLs
    original_url TEXT NOT NULL, -- R2 URL
    optimized_url TEXT, -- CDN URL for web viewing
    thumbnail_url TEXT, -- Small preview
    preview_url TEXT, -- Medium preview
    
    -- Video specific
    video_thumbnail_url TEXT,
    video_duration_seconds INTEGER,
    video_codec VARCHAR(50),
    
    -- Image Metadata
    width INTEGER,
    height INTEGER,
    aspect_ratio DECIMAL(5,2),
    orientation VARCHAR(20), -- portrait, landscape, square
    
    -- EXIF Data
    camera_make VARCHAR(100),
    camera_model VARCHAR(100),
    lens_model VARCHAR(100),
    focal_length INTEGER,
    aperture DECIMAL(3,1),
    shutter_speed VARCHAR(20),
    iso INTEGER,
    taken_at TIMESTAMP WITH TIME ZONE,
    gps_latitude DECIMAL(10,8),
    gps_longitude DECIMAL(11,8),
    
    -- AI Detection (future)
    faces_detected INTEGER DEFAULT 0,
    face_data JSONB, -- Face coordinates
    tags_auto TEXT[], -- AI-generated tags
    color_palette VARCHAR(7)[],
    
    -- Organization
    sort_order INTEGER DEFAULT 0,
    is_cover BOOLEAN DEFAULT false,
    is_hidden BOOLEAN DEFAULT false,
    
    -- Analytics
    view_count INTEGER DEFAULT 0,
    download_count INTEGER DEFAULT 0,
    
    -- Status
    processing_status VARCHAR(50) DEFAULT 'pending', -- pending, processing, completed, failed
    processing_error TEXT,
    
    -- Timestamps
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    PRIMARY KEY (id, uploaded_at),
    FOREIGN KEY (album_id, photographer_id) 
        REFERENCES albums(id, photographer_id) ON DELETE CASCADE
) PARTITION BY RANGE (uploaded_at);

-- Selections/Favorites
CREATE TABLE selections (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    album_id UUID NOT NULL,
    media_id UUID NOT NULL,
    
    -- Client identification
    client_email VARCHAR(255),
    client_session_id VARCHAR(255),
    client_ip INET,
    
    -- Selection details
    selected_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    selection_type VARCHAR(50) DEFAULT 'favorite', -- favorite, print, download
    notes TEXT,
    
    UNIQUE(album_id, media_id, client_email),
    INDEX idx_selections_album (album_id),
    INDEX idx_selections_client (client_email)
);

-- Analytics Events (partitioned by day)
CREATE TABLE analytics_events (
    id UUID DEFAULT uuid_generate_v4(),
    event_type VARCHAR(50) NOT NULL, -- view, download, share, favorite
    
    -- Related entities
    album_id UUID,
    media_id UUID,
    photographer_id UUID,
    
    -- Client info
    client_ip INET,
    user_agent TEXT,
    referer TEXT,
    country_code VARCHAR(2),
    
    -- Event details
    details JSONB,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Upload Sessions (for chunked uploads)
CREATE TABLE upload_sessions (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    photographer_id UUID NOT NULL REFERENCES photographers(id),
    album_id UUID NOT NULL,
    
    -- Session info
    total_files INTEGER NOT NULL,
    uploaded_files INTEGER DEFAULT 0,
    total_bytes BIGINT NOT NULL,
    uploaded_bytes BIGINT DEFAULT 0,
    
    -- Status
    status VARCHAR(50) DEFAULT 'active', -- active, completed, failed, expired
    error_message TEXT,
    
    -- Metadata
    client_type VARCHAR(50), -- web, ios, android
    client_version VARCHAR(20),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() + INTERVAL '24 hours',
    completed_at TIMESTAMP WITH TIME ZONE,
    
    INDEX idx_upload_session_status (status, expires_at)
);

-- Indexes for performance
CREATE INDEX idx_albums_photographer ON albums(photographer_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_albums_slug ON albums(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_media_album ON media(album_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_media_processing ON media(processing_status) WHERE processing_status = 'pending';
CREATE INDEX idx_analytics_album_date ON analytics_events(album_id, created_at);

-- Functions for automatic updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_photographers_updated_at BEFORE UPDATE ON photographers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_albums_updated_at BEFORE UPDATE ON albums
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

## Spring Boot Backend Implementation

### Project Structure
```
photovault-backend/
├── src/main/java/com/photovault/
│   ├── PhotoVaultApplication.java
│   ├── config/
│   │   ├── HazelcastConfig.java
│   │   ├── RabbitMQConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── S3Config.java
│   │   └── AsyncConfig.java
│   ├── controller/
│   │   ├── AlbumController.java
│   │   ├── MediaController.java
│   │   ├── UploadController.java
│   │   └── AuthController.java
│   ├── service/
│   │   ├── AlbumService.java
│   │   ├── MediaService.java
│   │   ├── StorageService.java
│   │   ├── ImageProcessingService.java
│   │   └── VideoProcessingService.java
│   ├── repository/
│   │   ├── AlbumRepository.java
│   │   ├── MediaRepository.java
│   │   └── PhotographerRepository.java
│   ├── entity/
│   │   ├── Photographer.java
│   │   ├── Album.java
│   │   ├── Media.java
│   │   └── UploadSession.java
│   ├── dto/
│   │   ├── AlbumDTO.java
│   │   ├── MediaDTO.java
│   │   └── UploadRequestDTO.java
│   ├── messaging/
│   │   ├── MediaProcessingProducer.java
│   │   ├── MediaProcessingConsumer.java
│   │   └── events/
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       └── custom/
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── application-prod.yml
└── pom.xml
```

### Core Dependencies (pom.xml)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    
    <groupId>com.photovault</groupId>
    <artifactId>photovault-backend</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <java.version>21</java.version>
        <hazelcast.version>5.3.6</hazelcast.version>
        <aws.sdk.version>2.21.0</aws.sdk.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Core -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        
        <!-- Hazelcast -->
        <dependency>
            <groupId>com.hazelcast</groupId>
            <artifactId>hazelcast-spring</artifactId>
            <version>${hazelcast.version}</version>
        </dependency>
        <dependency>
            <groupId>com.hazelcast</groupId>
            <artifactId>hazelcast-hibernate53</artifactId>
            <version>${hazelcast.version}</version>
        </dependency>
        
        <!-- RabbitMQ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        
        <!-- AWS SDK for S3/R2 -->
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>s3</artifactId>
            <version>${aws.sdk.version}</version>
        </dependency>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>s3-transfer-manager</artifactId>
            <version>${aws.sdk.version}</version>
        </dependency>
        
        <!-- Image Processing -->
        <dependency>
            <groupId>com.twelvemonkeys.imageio</groupId>
            <artifactId>imageio-jpeg</artifactId>
            <version>3.10.0</version>
        </dependency>
        <dependency>
            <groupId>net.coobird</groupId>
            <artifactId>thumbnailator</artifactId>
            <version>0.4.20</version>
        </dependency>
        
        <!-- Video Processing -->
        <dependency>
            <groupId>com.github.kokorin.jaffree</groupId>
            <artifactId>jaffree</artifactId>
            <version>2023.09.10</version>
        </dependency>
        
        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        
        <!-- Monitoring -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- Utils -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>1.5.5.Final</version>
        </dependency>
    </dependencies>
</project>
```

### Application Configuration (application.yml)
```yaml
spring:
  application:
    name: photovault-api
  
  datasource:
    # Master database for writes
    primary:
      url: jdbc:postgresql://master.db.photovault.com:5432/photovault
      username: ${DB_USERNAME}
      password: ${DB_PASSWORD}
      hikari:
        maximum-pool-size: 20
        minimum-idle: 5
        connection-timeout: 30000
    
    # Read replicas for queries
    readonly:
      url: jdbc:postgresql://read.db.photovault.com:5432/photovault
      username: ${DB_RO_USERNAME}
      password: ${DB_RO_PASSWORD}
      hikari:
        maximum-pool-size: 30
        minimum-idle: 10
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 25
        order_inserts: true
        cache:
          use_second_level_cache: true
          region.factory_class: com.hazelcast.hibernate.HazelcastCacheRegionFactory
  
  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: 5672
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
    virtual-host: photovault
    
    # Configure queues
    queues:
      media-processing: media.processing.queue
      thumbnail-generation: thumbnail.generation.queue
      video-transcoding: video.transcoding.queue
      analytics-events: analytics.events.queue

# Hazelcast Configuration
hazelcast:
  cluster-name: photovault-cluster
  network:
    port: 5701
    port-auto-increment: true
    join:
      kubernetes:
        enabled: true
        service-name: hazelcast-service
        namespace: photovault
  cache:
    albums:
      time-to-live-seconds: 3600
      max-idle-seconds: 1800
      eviction:
        eviction-policy: LRU
        max-size-policy: USED_HEAP_PERCENTAGE
        size: 20
    media:
      time-to-live-seconds: 7200
      max-idle-seconds: 3600

# Storage Configuration
storage:
  r2:
    endpoint: ${R2_ENDPOINT}
    access-key: ${R2_ACCESS_KEY}
    secret-key: ${R2_SECRET_KEY}
    bucket: photovault-media
    region: auto
  cdn:
    url: https://cdn.photovault.com
    
# Upload Configuration
upload:
  max-file-size: 5GB
  allowed-image-types: [jpg, jpeg, png, raw, dng, heic]
  allowed-video-types: [mp4, mov, avi, mkv]
  chunk-size: 10MB
  
# Processing Configuration
processing:
  image:
    thumbnail-size: 300
    preview-size: 800
    optimized-size: 1920
    quality: 85
    format: webp
  video:
    thumbnail-at-second: 3
    max-resolution: 1080p
    codec: h264
    
# Security
security:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000 # 24 hours
    refresh-expiration: 604800000 # 7 days
  cors:
    allowed-origins:
      - https://photovault.com
      - https://app.photovault.com
      - capacitor://localhost # iOS
      - http://localhost:3000 # Dev
```

### Core Services Implementation

#### StorageService.java
```java
@Service
@Slf4j
public class StorageService {
    
    private final S3Client s3Client;
    private final S3TransferManager transferManager;
    private final String bucketName;
    private final String cdnUrl;
    
    @Autowired
    public StorageService(S3Client s3Client, 
                         @Value("${storage.r2.bucket}") String bucketName,
                         @Value("${storage.cdn.url}") String cdnUrl) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.cdnUrl = cdnUrl;
        this.transferManager = S3TransferManager.builder()
            .s3Client(s3Client)
            .build();
    }
    
    /**
     * Generate presigned URL for direct upload from iOS/Web
     */
    public PresignedUploadUrl generatePresignedUploadUrl(String albumId, String filename) {
        String key = generateS3Key(albumId, filename);
        
        S3Presigner presigner = S3Presigner.create();
        PutObjectRequest objectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();
            
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofHours(24))
            .putObjectRequest(objectRequest)
            .build();
            
        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
        
        return PresignedUploadUrl.builder()
            .uploadUrl(presignedRequest.url().toString())
            .key(key)
            .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
            .build();
    }
    
    /**
     * Multipart upload for large files
     */
    @Async
    public CompletableFuture<String> uploadLargeFile(MultipartFile file, String albumId) {
        String key = generateS3Key(albumId, file.getOriginalFilename());
        
        try {
            Upload upload = transferManager.upload(UploadRequest.builder()
                .putObjectRequest(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build())
                .requestBody(AsyncRequestBody.fromInputStream(
                    file.getInputStream(), 
                    file.getSize(), 
                    Executors.newFixedThreadPool(10)))
                .build());
                
            CompletedUpload completedUpload = upload.completionFuture().join();
            log.info("Large file uploaded successfully: {}", key);
            
            return CompletableFuture.completedFuture(getCdnUrl(key));
            
        } catch (Exception e) {
            log.error("Failed to upload large file", e);
            throw new StorageException("Upload failed", e);
        }
    }
    
    /**
     * Download multiple files as ZIP
     */
    public byte[] createZipDownload(List<String> keys, String albumName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            
            for (String key : keys) {
                GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
                    
                ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getRequest);
                
                String filename = extractFilename(key);
                ZipEntry entry = new ZipEntry(albumName + "/" + filename);
                zos.putNextEntry(entry);
                zos.write(objectBytes.asByteArray());
                zos.closeEntry();
            }
            
            zos.finish();
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Failed to create ZIP download", e);
            throw new StorageException("ZIP creation failed", e);
        }
    }
    
    private String generateS3Key(String albumId, String filename) {
        String timestamp = Instant.now().toString();
        String sanitizedFilename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");
        return String.format("%s/originals/%s_%s", albumId, timestamp, sanitizedFilename);
    }
    
    private String getCdnUrl(String key) {
        return cdnUrl + "/" + key;
    }
}
```

#### ImageProcessingService.java
```java
@Service
@Slf4j
public class ImageProcessingService {
    
    @Autowired
    private StorageService storageService;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private HazelcastInstance hazelcastInstance;
    
    private static final int[] SIZES = {300, 800, 1920}; // thumbnail, preview, optimized
    
    /**
     * Process uploaded image asynchronously
     */
    @Async
    @Transactional
    public CompletableFuture<ProcessedMedia> processImage(Media media) {
        log.info("Processing image: {}", media.getId());
        
        try {
            // Download original from R2
            byte[] originalBytes = storageService.downloadFile(media.getOriginalUrl());
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
            
            // Extract metadata
            MediaMetadata metadata = extractMetadata(originalImage, originalBytes);
            media.setWidth(metadata.getWidth());
            media.setHeight(metadata.getHeight());
            media.setAspectRatio(metadata.getAspectRatio());
            
            // Generate multiple sizes
            Map<String, String> generatedUrls = new HashMap<>();
            
            for (int targetSize : SIZES) {
                BufferedImage resized = resizeImage(originalImage, targetSize);
                byte[] optimizedBytes = convertToWebP(resized);
                
                String key = generateOptimizedKey(media.getId(), targetSize);
                String url = storageService.uploadBytes(optimizedBytes, key, "image/webp");
                
                if (targetSize == 300) {
                    generatedUrls.put("thumbnail", url);
                    media.setThumbnailUrl(url);
                } else if (targetSize == 800) {
                    generatedUrls.put("preview", url);
                    media.setPreviewUrl(url);
                } else {
                    generatedUrls.put("optimized", url);
                    media.setOptimizedUrl(url);
                }
            }
            
            // Generate blur hash for lazy loading
            String blurHash = generateBlurHash(originalImage);
            media.setBlurHash(blurHash);
            
            // Update processing status
            media.setProcessingStatus(ProcessingStatus.COMPLETED);
            media.setProcessedAt(Instant.now());
            
            // Cache processed URLs
            IMap<String, ProcessedMedia> cache = hazelcastInstance.getMap("processedMedia");
            cache.put(media.getId().toString(), toProcessedMedia(media), 1, TimeUnit.HOURS);
            
            // Send completion event
            rabbitTemplate.convertAndSend("media.processed", new MediaProcessedEvent(media.getId()));
            
            log.info("Image processing completed: {}", media.getId());
            return CompletableFuture.completedFuture(toProcessedMedia(media));
            
        } catch (Exception e) {
            log.error("Failed to process image: {}", media.getId(), e);
            media.setProcessingStatus(ProcessingStatus.FAILED);
            media.setProcessingError(e.getMessage());
            throw new ProcessingException("Image processing failed", e);
        }
    }
    
    /**
     * Resize image maintaining aspect ratio
     */
    private BufferedImage resizeImage(BufferedImage original, int targetSize) throws IOException {
        int width = original.getWidth();
        int height = original.getHeight();
        
        // Calculate new dimensions
        double scale = Math.min((double) targetSize / width, (double) targetSize / height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        
        // Use Thumbnailator for high-quality resizing
        return Thumbnails.of(original)
            .size(newWidth, newHeight)
            .outputQuality(0.9)
            .asBufferedImage();
    }
    
    /**
     * Convert to WebP format for better compression
     */
    private byte[] convertToWebP(BufferedImage image) throws IOException {
        // Using imageio-webp plugin
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "webp", baos);
        return baos.toByteArray();
    }
    
    /**
     * Extract EXIF metadata
     */
    private MediaMetadata extractMetadata(BufferedImage image, byte[] originalBytes) {
        MediaMetadata metadata = new MediaMetadata();
        metadata.setWidth(image.getWidth());
        metadata.setHeight(image.getHeight());
        metadata.setAspectRatio((double) image.getWidth() / image.getHeight());
        
        try {
            // Extract EXIF using metadata-extractor
            Metadata exif = ImageMetadataReader.readMetadata(new ByteArrayInputStream(originalBytes));
            
            Directory directory = exif.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (directory != null) {
                metadata.setCameraMake(directory.getString(ExifIFD0Directory.TAG_MAKE));
                metadata.setCameraModel(directory.getString(ExifIFD0Directory.TAG_MODEL));
            }
            
            ExifSubIFDDirectory subDirectory = exif.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subDirectory != null) {
                metadata.setIso(subDirectory.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
                metadata.setFocalLength(subDirectory.getDouble(ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
                metadata.setAperture(subDirectory.getDouble(ExifSubIFDDirectory.TAG_APERTURE));
                Date dateTaken = subDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (dateTaken != null) {
                    metadata.setTakenAt(dateTaken.toInstant());
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to extract EXIF data", e);
        }
        
        return metadata;
    }
}
```

#### MediaController.java
```java
@RestController
@RequestMapping("/api/v1/media")
@Slf4j
@RequiredArgsConstructor
public class MediaController {
    
    private final MediaService mediaService;
    private final StorageService storageService;
    private final ImageProcessingService imageProcessingService;
    private final HazelcastInstance hazelcastInstance;
    
    /**
     * Initialize chunked upload session (for iOS/Android apps)
     */
    @PostMapping("/upload/init")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<UploadSessionResponse> initializeUpload(
            @Valid @RequestBody UploadInitRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Initializing upload session for {} files", request.getFileCount());
        
        UploadSession session = mediaService.createUploadSession(
            userDetails.getUsername(),
            request.getAlbumId(),
            request.getFileCount(),
            request.getTotalSize()
        );
        
        // Generate presigned URLs for each file
        List<PresignedUploadUrl> uploadUrls = new ArrayList<>();
        for (FileInfo fileInfo : request.getFiles()) {
            PresignedUploadUrl url = storageService.generatePresignedUploadUrl(
                request.getAlbumId(),
                fileInfo.getFilename()
            );
            uploadUrls.add(url);
        }
        
        return ResponseEntity.ok(UploadSessionResponse.builder()
            .sessionId(session.getId())
            .uploadUrls(uploadUrls)
            .expiresAt(session.getExpiresAt())
            .build());
    }
    
    /**
     * Direct file upload (for web)
     */
    @PostMapping("/upload/direct")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<MediaUploadResponse> uploadDirect(
            @RequestParam("file") MultipartFile file,
            @RequestParam("albumId") String albumId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // Validate file
        validateFile(file);
        
        // Save media record
        Media media = mediaService.createMedia(albumId, file, userDetails.getUsername());
        
        // Process asynchronously
        CompletableFuture.runAsync(() -> {
            if (isImage(file)) {
                imageProcessingService.processImage(media);
            } else if (isVideo(file)) {
                videoProcessingService.processVideo(media);
            }
        });
        
        return ResponseEntity.ok(MediaUploadResponse.builder()
            .mediaId(media.getId())
            .status("processing")
            .build());
    }
    
    /**
     * Confirm upload completion (from iOS/Android after direct R2 upload)
     */
    @PostMapping("/upload/complete")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<Void> confirmUpload(
            @Valid @RequestBody UploadCompleteRequest request) {
        
        mediaService.confirmUpload(request.getSessionId(), request.getFileKey());
        
        // Trigger processing
        rabbitTemplate.convertAndSend(
            "media.processing.queue",
            new ProcessMediaCommand(request.getFileKey())
        );
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get processed media URLs (cached)
     */
    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaDTO> getMedia(@PathVariable UUID mediaId) {
        
        // Check cache first
        IMap<String, MediaDTO> cache = hazelcastInstance.getMap("media");
        String cacheKey = mediaId.toString();
        
        MediaDTO cached = cache.get(cacheKey);
        if (cached != null) {
            return ResponseEntity.ok(cached);
        }
        
        // Load from database
        MediaDTO media = mediaService.getMedia(mediaId);
        
        // Cache for future requests
        cache.put(cacheKey, media, 1, TimeUnit.HOURS);
        
        return ResponseEntity.ok(media);
    }
    
    /**
     * Bulk download as ZIP
     */
    @PostMapping("/download/bulk")
    public ResponseEntity<byte[]> downloadBulk(@RequestBody BulkDownloadRequest request) {
        
        // Verify access
        Album album = albumService.getAlbum(request.getAlbumId());
        if (album.isRequiresPassword() && !verifyPassword(request.getPassword(), album)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Generate ZIP
        byte[] zipBytes = storageService.createZipDownload(
            request.getMediaIds(),
            album.getTitle()
        );
        
        // Track analytics
        analyticsService.trackBulkDownload(album.getId(), request.getMediaIds().size());
        
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"" + album.getSlug() + ".zip\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(zipBytes);
    }
    
    /**
     * Add to favorites
     */
    @PostMapping("/{mediaId}/favorite")
    public ResponseEntity<Void> addFavorite(
            @PathVariable UUID mediaId,
            @RequestBody FavoriteRequest request) {
        
        mediaService.addToFavorites(mediaId, request.getClientEmail());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get album media with pagination
     */
    @GetMapping("/album/{albumId}")
    public ResponseEntity<Page<MediaDTO>> getAlbumMedia(
            @PathVariable UUID albumId,
            @PageableDefault(size = 50) Pageable pageable) {
        
        Page<MediaDTO> media = mediaService.getAlbumMedia(albumId, pageable);
        
        return ResponseEntity.ok(media);
    }
    
    private void validateFile(MultipartFile file) {
        // Check file size
        if (file.getSize() > 5L * 1024 * 1024 * 1024) { // 5GB
            throw new ValidationException("File too large");
        }
        
        // Check file type
        String contentType = file.getContentType();
        if (!isAllowedType(contentType)) {
            throw new ValidationException("File type not allowed");
        }
    }
}
```

### Hazelcast Cache Configuration
```java
@Configuration
@EnableCaching
public class HazelcastConfig {
    
    @Bean
    public Config hazelcastConfig() {
        Config config = new Config();
        config.setClusterName("photovault-cluster");
        
        // Network configuration for Kubernetes
        NetworkConfig network = config.getNetworkConfig();
        JoinConfig join = network.getJoin();
        join.getMulticastConfig().setEnabled(false);
        
        KubernetesConfig kubernetesConfig = join.getKubernetesConfig();
        kubernetesConfig.setEnabled(true)
            .setProperty("namespace", "photovault")
            .setProperty("service-name", "hazelcast-service");
        
        // Configure distributed maps
        MapConfig albumCacheConfig = new MapConfig("albums");
        albumCacheConfig.setTimeToLiveSeconds(3600) // 1 hour
            .setMaxIdleSeconds(1800) // 30 minutes
            .setEvictionConfig(new EvictionConfig()
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setMaxSizePolicy(MaxSizePolicy.USED_HEAP_PERCENTAGE)
                .setSize(20));
        config.addMapConfig(albumCacheConfig);
        
        MapConfig mediaCacheConfig = new MapConfig("media");
        mediaCacheConfig.setTimeToLiveSeconds(7200) // 2 hours
            .setMaxIdleSeconds(3600) // 1 hour
            .setEvictionConfig(new EvictionConfig()
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setMaxSizePolicy(MaxSizePolicy.USED_HEAP_PERCENTAGE)
                .setSize(30));
        config.addMapConfig(mediaCacheConfig);
        
        // Configure distributed queue for processing
        QueueConfig processingQueueConfig = new QueueConfig("processing-queue");
        processingQueueConfig.setMaxSize(10000)
            .setBackupCount(1);
        config.addQueueConfig(processingQueueConfig);
        
        // Enable management center
        ManagementCenterConfig managementCenterConfig = new ManagementCenterConfig();
        managementCenterConfig.setScriptingEnabled(false)
            .addTrustedInterface("10.0.0.*");
        config.setManagementCenterConfig(managementCenterConfig);
        
        return config;
    }
    
    @Bean
    public HazelcastInstance hazelcastInstance(Config config) {
        return Hazelcast.newHazelcastInstance(config);
    }
    
    @Bean
    public CacheManager cacheManager(HazelcastInstance hazelcastInstance) {
        return new com.hazelcast.spring.cache.HazelcastCacheManager(hazelcastInstance);
    }
}
```

### RabbitMQ Message Queue Configuration
```java
@Configuration
@EnableRabbit
public class RabbitMQConfig {
    
    @Value("${spring.rabbitmq.queues.media-processing}")
    private String mediaProcessingQueue;
    
    @Value("${spring.rabbitmq.queues.thumbnail-generation}")
    private String thumbnailQueue;
    
    @Value("${spring.rabbitmq.queues.video-transcoding}")
    private String videoQueue;
    
    @Bean
    public Queue mediaProcessingQueue() {
        return QueueBuilder.durable(mediaProcessingQueue)
            .withArgument("x-message-ttl", 3600000) // 1 hour TTL
            .withArgument("x-max-retries", 3)
            .build();
    }
    
    @Bean
    public Queue thumbnailGenerationQueue() {
        return QueueBuilder.durable(thumbnailQueue)
            .withArgument("x-priority", 10) // High priority
            .build();
    }
    
    @Bean
    public Queue videoTranscodingQueue() {
        return QueueBuilder.durable(videoQueue)
            .withArgument("x-message-ttl", 7200000) // 2 hour TTL
            .build();
    }
    
    @Bean
    public TopicExchange mediaExchange() {
        return new TopicExchange("media.exchange");
    }
    
    @Bean
    public Binding mediaProcessingBinding() {
        return BindingBuilder
            .bind(mediaProcessingQueue())
            .to(mediaExchange())
            .with("media.uploaded");
    }
    
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("Message failed to deliver: {}", cause);
            }
        });
        return template;
    }
    
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(5);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(1);
        return factory;
    }
}
```

### Message Consumers
```java
@Component
@Slf4j
@RequiredArgsConstructor
public class MediaProcessingConsumer {
    
    private final ImageProcessingService imageProcessingService;
    private final VideoProcessingService videoProcessingService;
    private final MediaRepository mediaRepository;
    
    @RabbitListener(queues = "${spring.rabbitmq.queues.media-processing}")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 5000))
    public void processMedia(ProcessMediaCommand command) {
        log.info("Processing media: {}", command.getMediaId());
        
        try {
            Media media = mediaRepository.findById(command.getMediaId())
                .orElseThrow(() -> new EntityNotFoundException("Media not found"));
            
            if (media.getMimeType().startsWith("image/")) {
                imageProcessingService.processImage(media);
            } else if (media.getMimeType().startsWith("video/")) {
                videoProcessingService.processVideo(media);
            }
            
            log.info("Media processing completed: {}", command.getMediaId());
            
        } catch (Exception e) {
            log.error("Failed to process media: {}", command.getMediaId(), e);
            throw new ProcessingException("Processing failed", e);
        }
    }
    
    @RabbitListener(queues = "${spring.rabbitmq.queues.thumbnail-generation}")
    public void generateThumbnail(GenerateThumbnailCommand command) {
        log.info("Generating thumbnail: {}", command.getMediaId());
        
        imageProcessingService.generateThumbnail(
            command.getMediaId(),
            command.getSize()
        );
    }
    
    @RabbitListener(queues = "${spring.rabbitmq.queues.video-transcoding}")
    public void transcodeVideo(TranscodeVideoCommand command) {
        log.info("Transcoding video: {}", command.getMediaId());
        
        videoProcessingService.transcodeVideo(
            command.getMediaId(),
            command.getTargetResolution(),
            command.getCodec()
        );
    }
}
```

## Kubernetes Deployment

### deployment.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: photovault-api
  namespace: photovault
spec:
  replicas: 3
  selector:
    matchLabels:
      app: photovault-api
  template:
    metadata:
      labels:
        app: photovault-api
    spec:
      containers:
      - name: photovault-api
        image: photovault/api:latest
        ports:
        - containerPort: 8080
        - containerPort: 5701 # Hazelcast
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: password
        resources:
          requests:
            memory: "2Gi"
            cpu: "500m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: photovault-api-service
  namespace: photovault
spec:
  selector:
    app: photovault-api
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
---
apiVersion: v1
kind: Service
metadata:
  name: hazelcast-service
  namespace: photovault
spec:
  selector:
    app: photovault-api
  ports:
  - port: 5701
    targetPort: 5701
  type: ClusterIP
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: photovault-api-hpa
  namespace: photovault
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: photovault-api
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

## iOS App Integration

### Swift SDK
```swift
import Foundation
import Alamofire

class PhotoVaultSDK {
    private let baseURL: String
    private var authToken: String?
    
    init(baseURL: String = "https://api.photovault.com") {
        self.baseURL = baseURL
    }
    
    // MARK: - Authentication
    func login(email: String, password: String) async throws -> AuthResponse {
        let parameters = ["email": email, "password": password]
        
        return try await AF.request("\(baseURL)/auth/login",
                                   method: .post,
                                   parameters: parameters,
                                   encoding: JSONEncoding.default)
            .serializingDecodable(AuthResponse.self)
            .value
    }
    
    // MARK: - Album Management
    func createAlbum(title: String, clientEmail: String?) async throws -> Album {
        let parameters: [String: Any] = [
            "title": title,
            "clientEmail": clientEmail ?? ""
        ]
        
        return try await AF.request("\(baseURL)/api/v1/albums",
                                   method: .post,
                                   parameters: parameters,
                                   headers: authHeaders())
            .serializingDecodable(Album.self)
            .value
    }
    
    // MARK: - Upload with Background Support
    func uploadPhotos(to albumId: String, photos: [UIImage]) async throws {
        // Initialize upload session
        let session = try await initializeUploadSession(
            albumId: albumId,
            fileCount: photos.count
        )
        
        // Upload each photo in background
        for (index, photo) in photos.enumerated() {
            let imageData = photo.jpegData(compressionQuality: 0.9)!
            let uploadUrl = session.uploadUrls[index]
            
            try await uploadToR2(data: imageData, url: uploadUrl.url)
            
            // Confirm upload
            try await confirmUpload(
                sessionId: session.sessionId,
                fileKey: uploadUrl.key
            )
        }
    }
    
    private func uploadToR2(data: Data, url: String) async throws {
        try await AF.upload(data, to: url, method: .put)
            .serializingData()
            .value
    }
    
    // MARK: - Background Upload Session
    func createBackgroundUploadSession() -> URLSession {
        let config = URLSessionConfiguration.background(
            withIdentifier: "com.photovault.upload"
        )
        config.isDiscretionary = false
        config.sessionSendsLaunchEvents = true
        return URLSession(configuration: config, delegate: self, delegateQueue: nil)
    }
}

// MARK: - URLSessionDelegate for Background Uploads
extension PhotoVaultSDK: URLSessionDelegate, URLSessionTaskDelegate {
    func urlSession(_ session: URLSession,
                   task: URLSessionTask,
                   didCompleteWithError error: Error?) {
        if let error = error {
            print("Upload failed: \(error)")
        } else {
            print("Upload completed")
            // Send local notification
            UNUserNotificationCenter.current().add(uploadCompleteNotification())
        }
    }
    
    func urlSession(_ session: URLSession,
                   task: URLSessionTask,
                   didSendBodyData bytesSent: Int64,
                   totalBytesSent: Int64,
                   totalBytesExpectedToSend: Int64) {
        let progress = Double(totalBytesSent) / Double(totalBytesExpectedToSend)
        NotificationCenter.default.post(
            name: .uploadProgress,
            object: nil,
            userInfo: ["progress": progress]
        )
    }
}
```

## Performance Optimizations

### Database Optimizations
```sql
-- Create indexes for common queries
CREATE INDEX CONCURRENTLY idx_albums_photographer_active 
ON albums(photographer_id, is_active) 
WHERE deleted_at IS NULL;

CREATE INDEX CONCURRENTLY idx_media_album_sorted 
ON media(album_id, sort_order) 
WHERE deleted_at IS NULL;

-- Partial indexes for processing queue
CREATE INDEX CONCURRENTLY idx_media_pending 
ON media(processing_status) 
WHERE processing_status = 'pending';

-- Create materialized view for analytics
CREATE MATERIALIZED VIEW album_analytics AS
SELECT 
    a.id,
    a.photographer_id,
    COUNT(DISTINCT ae.client_ip) as unique_visitors,
    COUNT(CASE WHEN ae.event_type = 'view' THEN 1 END) as total_views,
    COUNT(CASE WHEN ae.event_type = 'download' THEN 1 END) as total_downloads,
    DATE_TRUNC('day', ae.created_at) as date
FROM albums a
LEFT JOIN analytics_events ae ON a.id = ae.album_id
GROUP BY a.id, a.photographer_id, DATE_TRUNC('day', ae.created_at);

CREATE INDEX ON album_analytics(photographer_id, date);

-- Refresh materialized view periodically
CREATE OR REPLACE FUNCTION refresh_album_analytics()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY album_analytics;
END;
$$ LANGUAGE plpgsql;

-- Schedule refresh every hour
SELECT cron.schedule('refresh-analytics', '0 * * * *', 'SELECT refresh_album_analytics()');
```

### Hazelcast Cache Warming
```java
@Component
@Slf4j
public class CacheWarmer {
    
    @Autowired
    private HazelcastInstance hazelcastInstance;
    
    @Autowired
    private AlbumRepository albumRepository;
    
    @EventListener(ApplicationReadyEvent.class)
    public void warmCache() {
        log.info("Warming up cache...");
        
        // Load popular albums into cache
        IMap<String, Album> albumCache = hazelcastInstance.getMap("albums");
        
        List<Album> popularAlbums = albumRepository.findMostViewedAlbums(
            PageRequest.of(0, 100)
        );
        
        for (Album album : popularAlbums) {
            albumCache.put(album.getId().toString(), album, 1, TimeUnit.HOURS);
        }
        
        log.info("Cache warmed with {} albums", popularAlbums.size());
    }
}
```

## Monitoring and Observability

### Prometheus Metrics
```java
@Component
public class MetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    // Custom metrics
    private final Counter uploadCounter;
    private final Gauge activeUploads;
    private final Timer processingTimer;
    
    public MetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.uploadCounter = Counter.builder("photovault.uploads.total")
            .description("Total number of uploads")
            .register(meterRegistry);
            
        this.activeUploads = Gauge.builder("photovault.uploads.active", this, MetricsCollector::getActiveUploadCount)
            .description("Currently active uploads")
            .register(meterRegistry);
            
        this.processingTimer = Timer.builder("photovault.processing.duration")
            .description("Image processing duration")
            .register(meterRegistry);
    }
    
    public void recordUpload(String fileType) {
        uploadCounter.increment();
        meterRegistry.counter("photovault.uploads.by.type", "type", fileType).increment();
    }
    
    public Timer.Sample startProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordProcessingTime(Timer.Sample sample) {
        sample.stop(processingTimer);
    }
}
```

## Pricing & Business Model

### SaaS Tiers
```yaml
Free Trial:
  duration: 14 days
  albums: 3
  storage: 1GB
  features: Basic

Starter: $9/month
  albums: 10
  storage: 10GB
  features:
    - Client favorites
    - Basic analytics
    - Email support

Pro: $29/month (Most Popular)
  albums: Unlimited
  storage: 100GB
  features:
    - Everything in Starter
    - Custom branding
    - Priority processing
    - API access
    - Custom domain

Studio: $99/month
  albums: Unlimited
  storage: 500GB
  features:
    - Everything in Pro
    - Multiple photographers
    - Advanced analytics
    - SLA guarantee
    - Phone support

Enterprise: Custom
  albums: Unlimited
  storage: Custom
  features:
    - Everything in Studio
    - Dedicated infrastructure
    - Custom integrations
    - On-premise option
```

## Cost Analysis at Scale

### Infrastructure Costs
```yaml
100 Photographers ($2,900 MRR):
  AWS/GCP: $200/month
  R2 Storage (1TB): $15/month
  R2 Bandwidth (5TB): $90/month
  Total: $305/month
  Margin: 89%

1,000 Photographers ($29,000 MRR):
  AWS/GCP: $800/month
  R2 Storage (10TB): $150/month
  R2 Bandwidth (50TB): $900/month
  Total: $1,850/month
  Margin: 94%

10,000 Photographers ($290,000 MRR):
  AWS/GCP: $5,000/month
  R2 Storage (100TB): $1,500/month
  R2 Bandwidth (500TB): $9,000/month
  Total: $15,500/month
  Margin: 95%
```

## Launch Strategy

### Phase 1: MVP (Month 1)
- Core upload/gallery features
- Basic iOS app
- 50 beta photographers

### Phase 2: Growth (Month 2-3)
- Advanced processing features
- Android app
- 500 photographers

### Phase 3: Scale (Month 4-6)
- Enterprise features
- White-label option
- 5,000 photographers

### Phase 4: Market Leader (Year 2)
- AI features (face detection, auto-tagging)
- Video editing tools
- 50,000 photographers

## Success Metrics

### Technical KPIs
- Upload success rate: >99.5%
- Processing time: <10s for images, <60s for videos
- Gallery load time: <2s
- Uptime: 99.9%

### Business KPIs
- MRR: $50K by month 6
- CAC: <$50
- LTV: >$500
- Churn: <5% monthly
- NPS: >50

## Summary

This architecture provides:
1. **Scalability**: Handle 100,000+ concurrent users
2. **Performance**: Sub-second response times with Hazelcast caching
3. **Reliability**: 99.9% uptime with Kubernetes orchestration
4. **Cost-effective**: 95% margins at scale
5. **Developer-friendly**: Clean separation of concerns, well-documented APIs

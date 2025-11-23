package com.photovault.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class R2StorageService {

    private final S3Client r2Client;

    @Value("${storage.r2.bucket:photovault-media}")
    private String r2Bucket;

    @Value("${storage.cdn.url:}")
    private String cdnUrl;

    public R2StorageService(@Qualifier("r2Client") S3Client r2Client) {
        this.r2Client = r2Client;
    }

    /**
     * Upload file to R2
     */
    public String uploadFile(MultipartFile file, String albumId, String prefix) throws IOException {
        if (r2Client == null) {
            throw new IllegalStateException("R2 client not configured");
        }

        String filename = generateUniqueFilename(file.getOriginalFilename());
        String key = String.format("%s/%s/%s", albumId, prefix, filename);

        log.info("Uploading to R2: bucket={}, key={}", r2Bucket, key);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("original-filename", file.getOriginalFilename());
        metadata.put("upload-time", Instant.now().toString());

        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(r2Bucket)
            .key(key)
            .contentType(file.getContentType())
            .contentLength(file.getSize())
            .metadata(metadata)
            .build();

        r2Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        log.info("Successfully uploaded to R2: {}", key);
        return buildUrl(key);
    }

    /**
     * Upload bytes to R2
     */
    public String uploadBytes(byte[] bytes, String albumId, String prefix, String filename, String contentType) {
        if (r2Client == null) {
            throw new IllegalStateException("R2 client not configured");
        }

        String uniqueFilename = generateUniqueFilename(filename);
        String key = String.format("%s/%s/%s", albumId, prefix, uniqueFilename);

        log.info("Uploading bytes to R2: bucket={}, key={}, size={}", r2Bucket, key, bytes.length);

        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(r2Bucket)
            .key(key)
            .contentType(contentType)
            .contentLength((long) bytes.length)
            .build();

        r2Client.putObject(putRequest, RequestBody.fromBytes(bytes));

        log.info("Successfully uploaded bytes to R2: {}", key);
        return buildUrl(key);
    }

    /**
     * Download file from R2
     */
    public byte[] downloadFile(String url) throws IOException {
        if (r2Client == null) {
            throw new IllegalStateException("R2 client not configured");
        }

        String key = extractKeyFromUrl(url);
        log.info("Downloading from R2: bucket={}, key={}", r2Bucket, key);

        GetObjectRequest getRequest = GetObjectRequest.builder()
            .bucket(r2Bucket)
            .key(key)
            .build();

        try {
            return r2Client.getObjectAsBytes(getRequest).asByteArray();
        } catch (Exception e) {
            log.error("Failed to download from R2: {}", key, e);
            throw new IOException("Failed to download file from R2", e);
        }
    }

    /**
     * Delete file from R2
     */
    public void deleteFile(String url) {
        if (r2Client == null) {
            throw new IllegalStateException("R2 client not configured");
        }

        String key = extractKeyFromUrl(url);
        log.info("Deleting from R2: bucket={}, key={}", r2Bucket, key);

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
            .bucket(r2Bucket)
            .key(key)
            .build();

        r2Client.deleteObject(deleteRequest);
        log.info("Successfully deleted from R2: {}", key);
    }

    /**
     * Check if file exists in R2
     */
    public boolean fileExists(String key) {
        if (r2Client == null) {
            return false;
        }

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(r2Bucket)
                .key(key)
                .build();

            r2Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /**
     * List all files in album
     */
    public ListObjectsV2Response listAlbumFiles(String albumId) {
        if (r2Client == null) {
            throw new IllegalStateException("R2 client not configured");
        }

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
            .bucket(r2Bucket)
            .prefix(albumId + "/")
            .build();

        return r2Client.listObjectsV2(listRequest);
    }

    /**
     * Generate presigned URL for direct upload (optional advanced feature)
     */
    public String generatePresignedUploadUrl(String albumId, String filename, int expirationMinutes) {
        if (r2Client == null) {
            throw new IllegalStateException("R2 client not configured");
        }

        String key = String.format("%s/originals/%s", albumId, generateUniqueFilename(filename));

        // Note: For presigned URLs, you'd need S3Presigner
        // This is a simplified version - full implementation would use S3Presigner
        log.info("Presigned URL requested for key: {}", key);

        return buildUrl(key);
    }

    private String buildUrl(String key) {
        if (cdnUrl != null && !cdnUrl.isEmpty()) {
            return cdnUrl + "/" + key;
        }
        // Fallback to R2 public URL (if public bucket)
        return String.format("https://%s.r2.cloudflarestorage.com/%s", r2Bucket, key);
    }

    private String extractKeyFromUrl(String url) {
        if (url.startsWith(cdnUrl)) {
            return url.substring(cdnUrl.length() + 1);
        }
        // Extract key from R2 URL
        int lastSlash = url.lastIndexOf(r2Bucket + "/");
        if (lastSlash >= 0) {
            return url.substring(lastSlash + r2Bucket.length() + 1);
        }
        return url;
    }

    private String generateUniqueFilename(String originalFilename) {
        String timestamp = Instant.now().toString().replaceAll("[^0-9]", "");
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        return timestamp + "_" + uuid + extension;
    }

    public boolean isConfigured() {
        return r2Client != null;
    }
}

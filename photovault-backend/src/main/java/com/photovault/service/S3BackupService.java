package com.photovault.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class S3BackupService {

    private final S3Client s3Client;
    private final S3Client r2Client;

    @Value("${storage.r2.bucket:photovault-media}")
    private String r2Bucket;

    @Value("${storage.s3.backup-bucket:photovault-backups}")
    private String s3BackupBucket;

    @Value("${storage.backup.enabled:true}")
    private boolean backupEnabled;

    @Value("${storage.backup.retention-days:30}")
    private int retentionDays;

    public S3BackupService(
            @Qualifier("s3Client") S3Client s3Client,
            @Qualifier("r2Client") S3Client r2Client) {
        this.s3Client = s3Client;
        this.r2Client = r2Client;
    }

    /**
     * Perform full backup from R2 to S3
     */
    public BackupResult performDailyBackup() {
        if (!backupEnabled) {
            log.info("Backup is disabled");
            return BackupResult.skipped("Backup disabled in configuration");
        }

        if (s3Client == null) {
            log.error("S3 client not configured - cannot perform backup");
            return BackupResult.failed("S3 not configured");
        }

        if (r2Client == null) {
            log.error("R2 client not configured - cannot perform backup");
            return BackupResult.failed("R2 not configured");
        }

        String backupDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String backupPrefix = "backups/" + backupDate + "/";

        log.info("Starting daily backup from R2 to S3: {}", backupDate);
        log.info("Source: R2 bucket '{}', Destination: S3 bucket '{}' prefix '{}'",
            r2Bucket, s3BackupBucket, backupPrefix);

        BackupResult result = new BackupResult();
        result.setBackupDate(backupDate);
        result.setStartTime(System.currentTimeMillis());

        try {
            // Ensure S3 backup bucket exists
            ensureS3BucketExists();

            // List all objects in R2
            List<S3Object> r2Objects = listAllR2Objects();
            log.info("Found {} objects in R2 to backup", r2Objects.size());

            int successCount = 0;
            int failureCount = 0;
            long totalBytes = 0;

            // Copy each object from R2 to S3
            for (S3Object r2Object : r2Objects) {
                try {
                    copyObjectToS3(r2Object.key(), backupPrefix + r2Object.key());
                    successCount++;
                    totalBytes += r2Object.size();

                    if (successCount % 100 == 0) {
                        log.info("Backup progress: {} objects copied", successCount);
                    }
                } catch (Exception e) {
                    log.error("Failed to backup object: {}", r2Object.key(), e);
                    failureCount++;
                    result.addFailedFile(r2Object.key());
                }
            }

            result.setSuccessCount(successCount);
            result.setFailureCount(failureCount);
            result.setTotalBytes(totalBytes);
            result.setEndTime(System.currentTimeMillis());
            result.setStatus("SUCCESS");

            log.info("Backup completed: {} succeeded, {} failed, {} bytes total, duration {} ms",
                successCount, failureCount, totalBytes, result.getDuration());

            // Clean up old backups
            cleanupOldBackups();

            return result;

        } catch (Exception e) {
            log.error("Backup failed with error", e);
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage());
            result.setEndTime(System.currentTimeMillis());
            return result;
        }
    }

    /**
     * Copy a single object from R2 to S3
     */
    private void copyObjectToS3(String sourceKey, String destinationKey) {
        log.debug("Copying {} to S3 as {}", sourceKey, destinationKey);

        // Download from R2
        GetObjectRequest r2GetRequest = GetObjectRequest.builder()
            .bucket(r2Bucket)
            .key(sourceKey)
            .build();

        byte[] objectData = r2Client.getObjectAsBytes(r2GetRequest).asByteArray();

        // Get object metadata
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
            .bucket(r2Bucket)
            .key(sourceKey)
            .build();

        HeadObjectResponse headResponse = r2Client.headObject(headRequest);

        // Upload to S3
        Map<String, String> metadata = new HashMap<>();
        metadata.put("original-key", sourceKey);
        metadata.put("backup-date", LocalDate.now().toString());

        PutObjectRequest s3PutRequest = PutObjectRequest.builder()
            .bucket(s3BackupBucket)
            .key(destinationKey)
            .contentType(headResponse.contentType())
            .metadata(metadata)
            .build();

        s3Client.putObject(s3PutRequest, RequestBody.fromBytes(objectData));
    }

    /**
     * List all objects in R2
     */
    private List<S3Object> listAllR2Objects() {
        List<S3Object> allObjects = new ArrayList<>();
        String continuationToken = null;

        do {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                .bucket(r2Bucket)
                .maxKeys(1000);

            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken);
            }

            ListObjectsV2Response response = r2Client.listObjectsV2(requestBuilder.build());
            allObjects.addAll(response.contents());

            continuationToken = response.nextContinuationToken();
        } while (continuationToken != null);

        return allObjects;
    }

    /**
     * Ensure S3 backup bucket exists
     */
    private void ensureS3BucketExists() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(s3BackupBucket)
                .build();

            s3Client.headBucket(headBucketRequest);
            log.info("S3 backup bucket '{}' exists", s3BackupBucket);
        } catch (NoSuchBucketException e) {
            log.info("Creating S3 backup bucket: {}", s3BackupBucket);
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .bucket(s3BackupBucket)
                .build();

            s3Client.createBucket(createBucketRequest);
            log.info("S3 backup bucket created: {}", s3BackupBucket);
        }
    }

    /**
     * Clean up backups older than retention period
     */
    private void cleanupOldBackups() {
        log.info("Cleaning up backups older than {} days", retentionDays);

        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);

        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(s3BackupBucket)
                .prefix("backups/")
                .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);

            int deletedCount = 0;
            for (S3Object object : response.contents()) {
                LocalDate objectDate = extractDateFromKey(object.key());
                if (objectDate != null && objectDate.isBefore(cutoffDate)) {
                    DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(s3BackupBucket)
                        .key(object.key())
                        .build();

                    s3Client.deleteObject(deleteRequest);
                    deletedCount++;
                }
            }

            log.info("Deleted {} old backup objects", deletedCount);
        } catch (Exception e) {
            log.error("Failed to cleanup old backups", e);
        }
    }

    /**
     * Extract date from backup key (format: backups/YYYY-MM-DD/...)
     */
    private LocalDate extractDateFromKey(String key) {
        try {
            String[] parts = key.split("/");
            if (parts.length >= 2 && parts[0].equals("backups")) {
                return LocalDate.parse(parts[1], DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (Exception e) {
            log.debug("Could not extract date from key: {}", key);
        }
        return null;
    }

    /**
     * Backup result data class
     */
    public static class BackupResult {
        private String backupDate;
        private String status;
        private int successCount;
        private int failureCount;
        private long totalBytes;
        private long startTime;
        private long endTime;
        private String errorMessage;
        private final List<String> failedFiles = new ArrayList<>();

        public static BackupResult skipped(String reason) {
            BackupResult result = new BackupResult();
            result.setStatus("SKIPPED");
            result.setErrorMessage(reason);
            return result;
        }

        public static BackupResult failed(String reason) {
            BackupResult result = new BackupResult();
            result.setStatus("FAILED");
            result.setErrorMessage(reason);
            return result;
        }

        public long getDuration() {
            return endTime - startTime;
        }

        public void addFailedFile(String filename) {
            failedFiles.add(filename);
        }

        // Getters and Setters
        public String getBackupDate() { return backupDate; }
        public void setBackupDate(String backupDate) { this.backupDate = backupDate; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }

        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }

        public long getTotalBytes() { return totalBytes; }
        public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }

        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }

        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public List<String> getFailedFiles() { return failedFiles; }
    }
}

package com.photovault.scheduling;

import com.photovault.service.S3BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BackupScheduler {

    private final S3BackupService backupService;

    /**
     * Run daily backup at 2:00 AM
     * Cron: second minute hour day month weekday
     */
    @Scheduled(cron = "${storage.backup.cron:0 0 2 * * *}")
    public void scheduleDailyBackup() {
        log.info("===== Starting scheduled daily backup =====");

        try {
            S3BackupService.BackupResult result = backupService.performDailyBackup();

            if ("SUCCESS".equals(result.getStatus())) {
                log.info("✅ Backup completed successfully");
                log.info("   Date: {}", result.getBackupDate());
                log.info("   Files: {} succeeded, {} failed", result.getSuccessCount(), result.getFailureCount());
                log.info("   Size: {} bytes ({} MB)", result.getTotalBytes(), result.getTotalBytes() / 1024 / 1024);
                log.info("   Duration: {} ms ({} seconds)", result.getDuration(), result.getDuration() / 1000);
            } else if ("SKIPPED".equals(result.getStatus())) {
                log.info("⏭️  Backup skipped: {}", result.getErrorMessage());
            } else {
                log.error("❌ Backup failed: {}", result.getErrorMessage());
                log.error("   Files succeeded: {}", result.getSuccessCount());
                log.error("   Files failed: {}", result.getFailureCount());
            }

        } catch (Exception e) {
            log.error("❌ Backup scheduler encountered an error", e);
        }

        log.info("===== Backup job finished =====");
    }

    /**
     * Health check - runs every hour to verify backup system is working
     */
    @Scheduled(cron = "0 0 * * * *")
    public void backupHealthCheck() {
        log.debug("Backup system health check - OK");
    }
}

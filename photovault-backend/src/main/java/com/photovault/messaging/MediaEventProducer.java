package com.photovault.messaging;

import com.photovault.entity.Media;
import com.photovault.repository.MediaRepository;
import com.photovault.service.ImageProcessingService;
import com.photovault.service.ProcessingJobService;
import com.photovault.service.VideoProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * In-process async dispatcher for media processing. Replaces the prior
 * Kafka producer/consumer pair — same public entry points, but work runs
 * on the Spring @Async executor instead of a broker. Class name preserved
 * so existing callers and test mocks keep working.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MediaEventProducer {

    private final ImageProcessingService imageProcessingService;
    private final VideoProcessingService videoProcessingService;
    private final MediaRepository mediaRepository;
    // @Lazy breaks the cycle: ProcessingJobService depends on this bean for retries.
    @Lazy private final ProcessingJobService processingJobService;

    @Async
    public void sendProcessingEvent(Media media, UUID jobId, UUID sessionId) {
        String mimeType = media.getMimeType();
        if (mimeType == null) return;
        if (mimeType.startsWith("image/")) runImage(media.getId(), jobId);
        else if (mimeType.startsWith("video/")) runVideo(media.getId(), jobId);
    }

    @Async
    public void sendImageEvent(UUID mediaId, UUID albumId, String mimeType, String originalUrl) {
        runImage(mediaId, null);
    }

    @Async
    public void sendVideoEvent(UUID mediaId, UUID albumId, String mimeType, String originalUrl) {
        runVideo(mediaId, null);
    }

    @Async
    public void sendImageEvent(UUID mediaId, UUID albumId, String mimeType, String originalUrl,
                               UUID jobId, UUID sessionId) {
        runImage(mediaId, jobId);
    }

    @Async
    public void sendVideoEvent(UUID mediaId, UUID albumId, String mimeType, String originalUrl,
                               UUID jobId, UUID sessionId) {
        runVideo(mediaId, jobId);
    }

    @Async
    public void sendThumbnailEvent(UUID mediaId, UUID albumId, String originalUrl) {
        runImage(mediaId, null);
    }

    private void runImage(UUID mediaId, UUID jobId) {
        log.info("Dispatching image processing — mediaId: {}, jobId: {}", mediaId, jobId);
        if (jobId != null) processingJobService.markProcessing(jobId, "inproc.image", 0, 0);
        try {
            Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

            imageProcessingService.processImage(media)
                .thenAccept(processed -> {
                    mediaRepository.save(processed);
                    if (jobId != null) processingJobService.markCompleted(jobId);
                    log.info("Image processing completed — mediaId: {}, jobId: {}", mediaId, jobId);
                })
                .exceptionally(ex -> {
                    log.error("Image processing failed — mediaId: {}, jobId: {}", mediaId, jobId, ex);
                    media.setProcessingStatus(Media.ProcessingStatus.FAILED);
                    media.setProcessingError(ex.getMessage());
                    mediaRepository.save(media);
                    if (jobId != null) processingJobService.markFailed(jobId, ex.getMessage());
                    return null;
                });
        } catch (Exception e) {
            log.error("Error dispatching image processing", e);
            if (jobId != null) processingJobService.markFailed(jobId, e.getMessage());
        }
    }

    private void runVideo(UUID mediaId, UUID jobId) {
        log.info("Dispatching video processing — mediaId: {}, jobId: {}", mediaId, jobId);
        if (jobId != null) processingJobService.markProcessing(jobId, "inproc.video", 0, 0);
        try {
            Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

            videoProcessingService.processVideo(media)
                .thenAccept(processed -> {
                    mediaRepository.save(processed);
                    if (jobId != null) processingJobService.markCompleted(jobId);
                    log.info("Video processing completed — mediaId: {}, jobId: {}", mediaId, jobId);
                })
                .exceptionally(ex -> {
                    log.error("Video processing failed — mediaId: {}, jobId: {}", mediaId, jobId, ex);
                    media.setProcessingStatus(Media.ProcessingStatus.FAILED);
                    media.setProcessingError(ex.getMessage());
                    mediaRepository.save(media);
                    if (jobId != null) processingJobService.markFailed(jobId, ex.getMessage());
                    return null;
                });
        } catch (Exception e) {
            log.error("Error dispatching video processing", e);
            if (jobId != null) processingJobService.markFailed(jobId, e.getMessage());
        }
    }
}

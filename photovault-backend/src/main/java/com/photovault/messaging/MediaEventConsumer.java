package com.photovault.messaging;

import com.photovault.config.KafkaConfig;
import com.photovault.entity.Media;
import com.photovault.messaging.event.MediaProcessingEvent;
import com.photovault.repository.MediaRepository;
import com.photovault.service.ImageProcessingService;
import com.photovault.service.ProcessingJobService;
import com.photovault.service.VideoProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class MediaEventConsumer {

    private final ImageProcessingService imageProcessingService;
    private final VideoProcessingService videoProcessingService;
    private final MediaRepository mediaRepository;
    private final ProcessingJobService processingJobService;

    @KafkaListener(
        topics = KafkaConfig.MEDIA_PROCESSING_TOPIC,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeImageProcessingEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        UUID mediaId = extractUUID(event, "mediaId");
        UUID jobId   = extractUUID(event, "jobId");

        log.info("Image event — mediaId: {}, jobId: {}, partition: {}, offset: {}",
            mediaId, jobId, partition, offset);

        if (jobId != null) {
            processingJobService.markProcessing(jobId, KafkaConfig.MEDIA_PROCESSING_TOPIC, partition, offset);
        }

        try {
            Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

            if (media.getMimeType().startsWith("image/")) {
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
            } else {
                if (jobId != null) processingJobService.markCompleted(jobId);
            }
        } catch (Exception e) {
            log.error("Error consuming image processing event", e);
            if (jobId != null) processingJobService.markFailed(jobId, e.getMessage());
        }
    }

    @KafkaListener(
        topics = KafkaConfig.VIDEO_TRANSCODING_TOPIC,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeVideoTranscodingEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        UUID mediaId = extractUUID(event, "mediaId");
        UUID jobId   = extractUUID(event, "jobId");

        log.info("Video event — mediaId: {}, jobId: {}, partition: {}, offset: {}",
            mediaId, jobId, partition, offset);

        if (jobId != null) {
            processingJobService.markProcessing(jobId, KafkaConfig.VIDEO_TRANSCODING_TOPIC, partition, offset);
        }

        try {
            Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

            if (media.getMimeType().startsWith("video/")) {
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
            } else {
                if (jobId != null) processingJobService.markCompleted(jobId);
            }
        } catch (Exception e) {
            log.error("Error consuming video transcoding event", e);
            if (jobId != null) processingJobService.markFailed(jobId, e.getMessage());
        }
    }

    @KafkaListener(
        topics = KafkaConfig.THUMBNAIL_GENERATION_TOPIC,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeThumbnailEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        UUID mediaId = extractUUID(event, "mediaId");
        UUID jobId   = extractUUID(event, "jobId");

        log.info("Thumbnail event — mediaId: {}, jobId: {}, partition: {}, offset: {}",
            mediaId, jobId, partition, offset);

        try {
            Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

            if (media.getMimeType().startsWith("image/")) {
                imageProcessingService.processImage(media)
                    .thenAccept(processed -> mediaRepository.save(processed));
            } else if (media.getMimeType().startsWith("video/")) {
                videoProcessingService.processVideo(media)
                    .thenAccept(processed -> mediaRepository.save(processed));
            }
        } catch (Exception e) {
            log.error("Error consuming thumbnail event", e);
        }
    }

    // -------------------------------------------------------------------------

    @KafkaListener(
        topics = KafkaConfig.MEDIA_PROCESSING_DLQ_TOPIC,
        groupId = "${spring.kafka.consumer.group-id}-dlq",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDeadLetterEvent(@Payload Map<String, Object> event) {
        UUID mediaId = extractUUID(event, "mediaId");
        UUID jobId   = extractUUID(event, "jobId");
        log.error("DLQ — permanently failed event: mediaId={}, jobId={}", mediaId, jobId);
        if (jobId != null) {
            processingJobService.markFailed(jobId, "Permanently failed — moved to DLQ");
        }
    }

    // -------------------------------------------------------------------------

    private static UUID extractUUID(Map<String, Object> event, String key) {
        Object val = event.get(key);
        if (val == null) return null;
        try {
            return UUID.fromString(val.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

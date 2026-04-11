package com.photovault.messaging;

import com.photovault.config.KafkaConfig;
import com.photovault.entity.Media;
import com.photovault.messaging.event.MediaProcessingEvent;
import com.photovault.repository.MediaRepository;
import com.photovault.service.ImageProcessingService;
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

    /**
     * Consume image processing events
     */
    @KafkaListener(
        topics = KafkaConfig.MEDIA_PROCESSING_TOPIC,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeImageProcessingEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        try {
            UUID mediaId = UUID.fromString((String) event.get("mediaId"));
            log.info("Consuming image processing event - mediaId: {}, partition: {}, offset: {}",
                mediaId, partition, offset);

            Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

            if (media.getMimeType().startsWith("image/")) {
                imageProcessingService.processImage(media)
                    .thenAccept(processedMedia -> {
                        mediaRepository.save(processedMedia);
                        log.info("Image processing completed for media: {}", mediaId);
                    })
                    .exceptionally(ex -> {
                        log.error("Image processing failed for media: {}", mediaId, ex);
                        media.setProcessingStatus(Media.ProcessingStatus.FAILED);
                        media.setProcessingError(ex.getMessage());
                        mediaRepository.save(media);
                        return null;
                    });
            }

        } catch (Exception e) {
            log.error("Error consuming image processing event", e);
        }
    }

    /**
     * Consume video transcoding events
     */
    @KafkaListener(
        topics = KafkaConfig.VIDEO_TRANSCODING_TOPIC,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeVideoTranscodingEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        try {
            UUID mediaId = UUID.fromString((String) event.get("mediaId"));
            log.info("Consuming video transcoding event - mediaId: {}, partition: {}, offset: {}",
                mediaId, partition, offset);

            Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

            if (media.getMimeType().startsWith("video/")) {
                videoProcessingService.processVideo(media)
                    .thenAccept(processedMedia -> {
                        mediaRepository.save(processedMedia);
                        log.info("Video processing completed for media: {}", mediaId);
                    })
                    .exceptionally(ex -> {
                        log.error("Video processing failed for media: {}", mediaId, ex);
                        media.setProcessingStatus(Media.ProcessingStatus.FAILED);
                        media.setProcessingError(ex.getMessage());
                        mediaRepository.save(media);
                        return null;
                    });
            }

        } catch (Exception e) {
            log.error("Error consuming video transcoding event", e);
        }
    }

    /**
     * Consume thumbnail generation events
     */
    @KafkaListener(
        topics = KafkaConfig.THUMBNAIL_GENERATION_TOPIC,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeThumbnailEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        try {
            UUID mediaId = UUID.fromString((String) event.get("mediaId"));
            log.info("Consuming thumbnail generation event - mediaId: {}, partition: {}, offset: {}",
                mediaId, partition, offset);

            Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

            // Generate thumbnail based on media type
            if (media.getMimeType().startsWith("image/")) {
                imageProcessingService.processImage(media)
                    .thenAccept(processedMedia -> mediaRepository.save(processedMedia));
            } else if (media.getMimeType().startsWith("video/")) {
                videoProcessingService.processVideo(media)
                    .thenAccept(processedMedia -> mediaRepository.save(processedMedia));
            }

        } catch (Exception e) {
            log.error("Error consuming thumbnail event", e);
        }
    }
}

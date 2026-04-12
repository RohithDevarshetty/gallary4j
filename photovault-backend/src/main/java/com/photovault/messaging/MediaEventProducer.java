package com.photovault.messaging;

import com.photovault.config.KafkaConfig;
import com.photovault.entity.Media;
import com.photovault.messaging.event.MediaProcessingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class MediaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Primary entry point called from MediaService after a job is created.
     * Determines topic by mimeType and attaches jobId + sessionId for tracking.
     */
    public void sendProcessingEvent(Media media, UUID jobId, UUID sessionId) {
        String mimeType = media.getMimeType();
        if (mimeType == null) return;

        if (mimeType.startsWith("image/")) {
            sendImageEvent(media.getId(), media.getAlbum().getId(), mimeType,
                media.getOriginalUrl(), jobId, sessionId);
        } else if (mimeType.startsWith("video/")) {
            sendVideoEvent(media.getId(), media.getAlbum().getId(), mimeType,
                media.getOriginalUrl(), jobId, sessionId);
        }
    }

    /** Legacy overload — used by paths that don't yet have a job. */
    public void sendImageEvent(UUID mediaId, UUID albumId, String mimeType, String originalUrl) {
        sendImageEvent(mediaId, albumId, mimeType, originalUrl, null, null);
    }

    /** Legacy overload — used by paths that don't yet have a job. */
    public void sendVideoEvent(UUID mediaId, UUID albumId, String mimeType, String originalUrl) {
        sendVideoEvent(mediaId, albumId, mimeType, originalUrl, null, null);
    }

    public void sendImageEvent(UUID mediaId, UUID albumId, String mimeType, String originalUrl,
                               UUID jobId, UUID sessionId) {
        MediaProcessingEvent event = MediaProcessingEvent.builder()
            .mediaId(mediaId)
            .albumId(albumId)
            .mimeType(mimeType)
            .originalUrl(originalUrl)
            .jobId(jobId)
            .sessionId(sessionId)
            .type(MediaProcessingEvent.ProcessingType.IMAGE)
            .build();
        sendMediaProcessingEvent(event);
    }

    public void sendVideoEvent(UUID mediaId, UUID albumId, String mimeType, String originalUrl,
                               UUID jobId, UUID sessionId) {
        MediaProcessingEvent event = MediaProcessingEvent.builder()
            .mediaId(mediaId)
            .albumId(albumId)
            .mimeType(mimeType)
            .originalUrl(originalUrl)
            .jobId(jobId)
            .sessionId(sessionId)
            .type(MediaProcessingEvent.ProcessingType.VIDEO)
            .build();
        sendMediaProcessingEvent(event);
    }

    public void sendThumbnailEvent(UUID mediaId, UUID albumId, String originalUrl) {
        MediaProcessingEvent event = MediaProcessingEvent.builder()
            .mediaId(mediaId)
            .albumId(albumId)
            .originalUrl(originalUrl)
            .type(MediaProcessingEvent.ProcessingType.THUMBNAIL)
            .build();
        sendMediaProcessingEvent(event);
    }

    private void sendMediaProcessingEvent(MediaProcessingEvent event) {
        String key   = event.getMediaId().toString();
        String topic = determineTopicByType(event.getType());

        log.info("Sending {} event — topic: {}, mediaId: {}, jobId: {}",
            event.getType(), topic, event.getMediaId(), event.getJobId());

        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Sent to {}, partition: {}, offset: {}",
                    topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send to topic: {}, jobId: {}", topic, event.getJobId(), ex);
            }
        });
    }

    private String determineTopicByType(MediaProcessingEvent.ProcessingType type) {
        return switch (type) {
            case IMAGE     -> KafkaConfig.MEDIA_PROCESSING_TOPIC;
            case VIDEO     -> KafkaConfig.VIDEO_TRANSCODING_TOPIC;
            case THUMBNAIL -> KafkaConfig.THUMBNAIL_GENERATION_TOPIC;
        };
    }
}

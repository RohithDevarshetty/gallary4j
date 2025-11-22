package com.photovault.messaging;

import com.photovault.config.KafkaConfig;
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
     * Send media processing event to Kafka
     */
    public void sendMediaProcessingEvent(MediaProcessingEvent event) {
        String key = event.getMediaId().toString();
        String topic = determineTopicByType(event.getType());

        log.info("Sending media processing event to topic: {}, mediaId: {}", topic, event.getMediaId());

        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Message sent successfully to topic: {}, partition: {}, offset: {}",
                    topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send message to topic: {}", topic, ex);
            }
        });
    }

    /**
     * Send thumbnail generation event
     */
    public void sendThumbnailEvent(UUID mediaId, UUID albumId, String originalUrl) {
        MediaProcessingEvent event = MediaProcessingEvent.builder()
            .mediaId(mediaId)
            .albumId(albumId)
            .originalUrl(originalUrl)
            .type(MediaProcessingEvent.ProcessingType.THUMBNAIL)
            .build();

        sendMediaProcessingEvent(event);
    }

    /**
     * Send video transcoding event
     */
    public void sendVideoEvent(UUID mediaId, UUID albumId, String mimeType, String originalUrl) {
        MediaProcessingEvent event = MediaProcessingEvent.builder()
            .mediaId(mediaId)
            .albumId(albumId)
            .mimeType(mimeType)
            .originalUrl(originalUrl)
            .type(MediaProcessingEvent.ProcessingType.VIDEO)
            .build();

        sendMediaProcessingEvent(event);
    }

    /**
     * Send image processing event
     */
    public void sendImageEvent(UUID mediaId, UUID albumId, String mimeType, String originalUrl) {
        MediaProcessingEvent event = MediaProcessingEvent.builder()
            .mediaId(mediaId)
            .albumId(albumId)
            .mimeType(mimeType)
            .originalUrl(originalUrl)
            .type(MediaProcessingEvent.ProcessingType.IMAGE)
            .build();

        sendMediaProcessingEvent(event);
    }

    private String determineTopicByType(MediaProcessingEvent.ProcessingType type) {
        return switch (type) {
            case IMAGE -> KafkaConfig.MEDIA_PROCESSING_TOPIC;
            case VIDEO -> KafkaConfig.VIDEO_TRANSCODING_TOPIC;
            case THUMBNAIL -> KafkaConfig.THUMBNAIL_GENERATION_TOPIC;
        };
    }
}

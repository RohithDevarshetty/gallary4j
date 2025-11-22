package com.photovault.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaProcessingEvent {
    private UUID mediaId;
    private UUID albumId;
    private String mimeType;
    private String originalUrl;
    private ProcessingType type;

    public enum ProcessingType {
        IMAGE,
        VIDEO,
        THUMBNAIL
    }
}

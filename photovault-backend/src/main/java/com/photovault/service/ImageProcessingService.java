package com.photovault.service;

import com.photovault.entity.Media;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageProcessingService {

    private final StorageService storageService;

    @Value("${processing.image.thumbnail-size:300}")
    private int thumbnailSize;

    @Value("${processing.image.preview-size:800}")
    private int previewSize;

    @Value("${processing.image.optimized-size:1920}")
    private int optimizedSize;

    @Value("${processing.image.quality:85}")
    private int quality;

    @Async
    public CompletableFuture<Media> processImage(Media media) {
        log.info("Processing image: {}", media.getId());

        try {
            // Download original from storage
            byte[] originalBytes = storageService.downloadFile(media.getOriginalUrl());
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalBytes));

            if (originalImage == null) {
                throw new IOException("Failed to read image");
            }

            // Extract metadata
            media.setWidth(originalImage.getWidth());
            media.setHeight(originalImage.getHeight());
            BigDecimal aspectRatio = BigDecimal.valueOf((double) originalImage.getWidth() / originalImage.getHeight())
                .setScale(2, RoundingMode.HALF_UP);
            media.setAspectRatio(aspectRatio);
            media.setOrientation(determineOrientation(originalImage.getWidth(), originalImage.getHeight()));

            // Generate thumbnail
            String thumbnailUrl = generateThumbnail(media, originalImage, thumbnailSize);
            media.setThumbnailUrl(thumbnailUrl);

            // Generate preview
            String previewUrl = generateThumbnail(media, originalImage, previewSize);
            media.setPreviewUrl(previewUrl);

            // Generate optimized version
            String optimizedUrl = generateThumbnail(media, originalImage, optimizedSize);
            media.setOptimizedUrl(optimizedUrl);

            // Update processing status
            media.setProcessingStatus(Media.ProcessingStatus.COMPLETED);
            media.setProcessedAt(Instant.now());

            log.info("Image processing completed: {}", media.getId());
            return CompletableFuture.completedFuture(media);

        } catch (Exception e) {
            log.error("Failed to process image: {}", media.getId(), e);
            media.setProcessingStatus(Media.ProcessingStatus.FAILED);
            media.setProcessingError(e.getMessage());
            return CompletableFuture.completedFuture(media);
        }
    }

    private String generateThumbnail(Media media, BufferedImage original, int targetSize) throws IOException {
        int width = original.getWidth();
        int height = original.getHeight();

        // Calculate new dimensions maintaining aspect ratio
        double scale = Math.min((double) targetSize / width, (double) targetSize / height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(original)
            .size(newWidth, newHeight)
            .outputQuality((double) quality / 100)
            .outputFormat("jpg")
            .toOutputStream(baos);

        byte[] thumbnailBytes = baos.toByteArray();
        String filename = String.format("%s_%dx%d.jpg", media.getId(), newWidth, newHeight);

        return storageService.uploadBytes(
            thumbnailBytes,
            media.getAlbum().getId().toString(),
            "processed",
            filename,
            "image/jpeg"
        );
    }

    private String determineOrientation(int width, int height) {
        if (width > height) {
            return "landscape";
        } else if (height > width) {
            return "portrait";
        } else {
            return "square";
        }
    }
}

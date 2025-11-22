package com.photovault.service;

import com.photovault.entity.Media;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoProcessingService {

    private final StorageService storageService;

    @Value("${processing.video.thumbnail-at-second:3}")
    private int thumbnailAtSecond;

    @Value("${processing.video.max-resolution:1080}")
    private String maxResolution;

    @Value("${processing.video.quality:23}")
    private int quality; // CRF value for H.264 (0-51, lower is better quality)

    @Value("${storage.local.path:./uploads}")
    private String uploadPath;

    /**
     * Process uploaded video asynchronously
     */
    @Async
    public CompletableFuture<Media> processVideo(Media media) {
        log.info("Processing video: {}", media.getId());

        try {
            // Download original video
            byte[] videoBytes = storageService.downloadFile(media.getOriginalUrl());

            // Create temp file
            Path tempVideoPath = createTempFile(videoBytes, ".mp4");

            // Extract video metadata
            VideoMetadata metadata = extractVideoMetadata(tempVideoPath.toString());
            updateMediaWithMetadata(media, metadata);

            // Generate thumbnail from video
            String thumbnailUrl = generateVideoThumbnail(media, tempVideoPath.toString());
            media.setVideoThumbnailUrl(thumbnailUrl);

            // Transcode video if needed (compress, standardize format)
            String optimizedUrl = transcodeVideo(media, tempVideoPath.toString());
            media.setOptimizedUrl(optimizedUrl);

            // Clean up temp file
            Files.deleteIfExists(tempVideoPath);

            // Update processing status
            media.setProcessingStatus(Media.ProcessingStatus.COMPLETED);
            media.setProcessedAt(Instant.now());

            log.info("Video processing completed: {}", media.getId());
            return CompletableFuture.completedFuture(media);

        } catch (Exception e) {
            log.error("Failed to process video: {}", media.getId(), e);
            media.setProcessingStatus(Media.ProcessingStatus.FAILED);
            media.setProcessingError(e.getMessage());
            return CompletableFuture.completedFuture(media);
        }
    }

    /**
     * Extract video metadata using FFprobe
     */
    private VideoMetadata extractVideoMetadata(String videoPath) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("ffprobe");
        command.add("-v");
        command.add("error");
        command.add("-show_entries");
        command.add("stream=width,height,duration,codec_name");
        command.add("-of");
        command.add("default=noprint_wrappers=1");
        command.add(videoPath);

        Process process = new ProcessBuilder(command).start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        VideoMetadata metadata = new VideoMetadata();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("width=")) {
                metadata.setWidth(Integer.parseInt(line.substring(6)));
            } else if (line.startsWith("height=")) {
                metadata.setHeight(Integer.parseInt(line.substring(7)));
            } else if (line.startsWith("duration=")) {
                try {
                    metadata.setDuration((int) Double.parseDouble(line.substring(9)));
                } catch (NumberFormatException e) {
                    log.warn("Could not parse duration: {}", line);
                }
            } else if (line.startsWith("codec_name=")) {
                String codec = line.substring(11);
                if (codec.equals("h264") || codec.equals("hevc") || codec.equals("vp9")) {
                    metadata.setCodec(codec);
                }
            }
        }

        process.waitFor(30, TimeUnit.SECONDS);
        return metadata;
    }

    /**
     * Generate thumbnail from video at specified second
     */
    private String generateVideoThumbnail(Media media, String videoPath) throws IOException, InterruptedException {
        String outputFilename = media.getId() + "_thumb.jpg";
        Path outputPath = Paths.get(uploadPath, "temp", outputFilename);
        Files.createDirectories(outputPath.getParent());

        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-ss");
        command.add(String.valueOf(thumbnailAtSecond));
        command.add("-i");
        command.add(videoPath);
        command.add("-vframes");
        command.add("1");
        command.add("-vf");
        command.add("scale=800:-1"); // Width 800px, maintain aspect ratio
        command.add("-q:v");
        command.add("2"); // High quality JPEG
        command.add("-y"); // Overwrite output file
        command.add(outputPath.toString());

        Process process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();

        boolean completed = process.waitFor(60, TimeUnit.SECONDS);

        if (!completed || process.exitValue() != 0) {
            throw new IOException("FFmpeg thumbnail generation failed");
        }

        // Upload thumbnail to storage
        byte[] thumbnailBytes = Files.readAllBytes(outputPath);
        String thumbnailUrl = storageService.uploadBytes(
            thumbnailBytes,
            media.getAlbum().getId().toString(),
            "thumbnails",
            outputFilename,
            "image/jpeg"
        );

        // Clean up temp file
        Files.deleteIfExists(outputPath);

        return thumbnailUrl;
    }

    /**
     * Transcode video to optimized format
     */
    private String transcodeVideo(Media media, String videoPath) throws IOException, InterruptedException {
        String outputFilename = media.getId() + "_optimized.mp4";
        Path outputPath = Paths.get(uploadPath, "temp", outputFilename);
        Files.createDirectories(outputPath.getParent());

        // Determine target resolution
        String scale = determineScale(media.getWidth(), media.getHeight());

        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(videoPath);
        command.add("-c:v");
        command.add("libx264"); // H.264 codec
        command.add("-crf");
        command.add(String.valueOf(quality)); // Quality (23 is default, lower = better)
        command.add("-preset");
        command.add("medium"); // Encoding speed/quality tradeoff
        command.add("-vf");
        command.add("scale=" + scale);
        command.add("-c:a");
        command.add("aac"); // AAC audio codec
        command.add("-b:a");
        command.add("128k"); // Audio bitrate
        command.add("-movflags");
        command.add("+faststart"); // Enable streaming
        command.add("-y");
        command.add(outputPath.toString());

        log.info("Starting video transcoding: {}", media.getId());

        Process process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();

        // Read output for debugging
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            log.debug("FFmpeg: {}", line);
        }

        boolean completed = process.waitFor(600, TimeUnit.SECONDS); // 10 minutes max

        if (!completed || process.exitValue() != 0) {
            throw new IOException("FFmpeg transcoding failed");
        }

        // Upload optimized video to storage
        byte[] videoBytes = Files.readAllBytes(outputPath);
        String optimizedUrl = storageService.uploadBytes(
            videoBytes,
            media.getAlbum().getId().toString(),
            "videos",
            outputFilename,
            "video/mp4"
        );

        // Clean up temp file
        Files.deleteIfExists(outputPath);

        log.info("Video transcoding completed: {}", media.getId());
        return optimizedUrl;
    }

    /**
     * Determine FFmpeg scale filter based on max resolution
     */
    private String determineScale(Integer width, Integer height) {
        if (width == null || height == null) {
            return "-1:-1"; // No scaling
        }

        int maxDimension = Integer.parseInt(maxResolution.replace("p", ""));

        if (width > height) {
            // Landscape: limit width
            return width > maxDimension ? maxDimension + ":-1" : "-1:-1";
        } else {
            // Portrait: limit height
            return height > maxDimension ? "-1:" + maxDimension : "-1:-1";
        }
    }

    private void updateMediaWithMetadata(Media media, VideoMetadata metadata) {
        media.setWidth(metadata.getWidth());
        media.setHeight(metadata.getHeight());
        media.setVideoDurationSeconds(metadata.getDuration());
        media.setVideoCodec(metadata.getCodec());

        if (metadata.getWidth() != null && metadata.getHeight() != null) {
            BigDecimal aspectRatio = BigDecimal.valueOf((double) metadata.getWidth() / metadata.getHeight())
                .setScale(2, RoundingMode.HALF_UP);
            media.setAspectRatio(aspectRatio);
            media.setOrientation(determineOrientation(metadata.getWidth(), metadata.getHeight()));
        }
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

    private Path createTempFile(byte[] content, String extension) throws IOException {
        Path tempDir = Paths.get(uploadPath, "temp");
        Files.createDirectories(tempDir);

        Path tempFile = tempDir.resolve(UUID.randomUUID() + extension);
        Files.write(tempFile, content);

        return tempFile;
    }

    /**
     * Video metadata container
     */
    @lombok.Data
    private static class VideoMetadata {
        private Integer width;
        private Integer height;
        private Integer duration;
        private String codec;
    }
}

package com.photovault.service;

import com.photovault.dto.MediaDTO;
import com.photovault.entity.Album;
import com.photovault.entity.Media;
import com.photovault.entity.Photographer;
import com.photovault.repository.AlbumRepository;
import com.photovault.repository.MediaRepository;
import com.photovault.repository.PhotographerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final AlbumRepository albumRepository;
    private final PhotographerRepository photographerRepository;
    private final StorageService storageService;
    private final ImageProcessingService imageProcessingService;

    @Transactional
    public Media createMedia(UUID albumId, MultipartFile file, String photographerEmail) {
        log.info("Creating media for album: {}", albumId);

        Photographer photographer = photographerRepository.findActiveByEmail(photographerEmail)
            .orElseThrow(() -> new RuntimeException("Photographer not found"));

        Album album = albumRepository.findById(albumId)
            .orElseThrow(() -> new RuntimeException("Album not found"));

        if (!album.getPhotographer().getId().equals(photographer.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        // Check storage limit
        Long currentStorage = mediaRepository.getTotalStorageByPhotographer(photographer.getId());
        if (currentStorage != null && currentStorage + file.getSize() > photographer.getStorageLimitBytes()) {
            throw new RuntimeException("Storage limit exceeded");
        }

        try {
            // Upload original file
            String originalUrl = storageService.uploadFile(file, albumId.toString(), "originals");

            Media media = Media.builder()
                .album(album)
                .photographer(photographer)
                .filename(file.getOriginalFilename())
                .originalFilename(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .originalUrl(originalUrl)
                .processingStatus(Media.ProcessingStatus.PENDING)
                .sortOrder(album.getMediaCount())
                .build();

            media = mediaRepository.save(media);

            // Update album and photographer stats
            album.setMediaCount(album.getMediaCount() + 1);
            album.setTotalSizeBytes(album.getTotalSizeBytes() + file.getSize());
            albumRepository.save(album);

            photographer.setStorageUsedBytes(photographer.getStorageUsedBytes() + file.getSize());
            photographerRepository.save(photographer);

            // Process asynchronously if image
            if (file.getContentType() != null && file.getContentType().startsWith("image/")) {
                log.info("Starting async image processing for media: {}", media.getId());
                imageProcessingService.processImage(media)
                    .thenAccept(processedMedia -> {
                        mediaRepository.save(processedMedia);
                        log.info("Image processing completed and saved: {}", processedMedia.getId());
                    });
            }

            log.info("Media created: {}", media.getId());
            return media;

        } catch (IOException e) {
            log.error("Failed to upload media", e);
            throw new RuntimeException("Failed to upload media", e);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "media", key = "#mediaId")
    public MediaDTO getMedia(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
            .orElseThrow(() -> new RuntimeException("Media not found"));
        return toDTO(media);
    }

    @Transactional(readOnly = true)
    public Page<MediaDTO> getAlbumMedia(UUID albumId, Pageable pageable) {
        return mediaRepository.findByAlbumId(albumId, pageable)
            .map(this::toDTO);
    }

    @Transactional
    public void deleteMedia(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
            .orElseThrow(() -> new RuntimeException("Media not found"));

        media.setDeletedAt(Instant.now());
        mediaRepository.save(media);

        // Update album stats
        Album album = media.getAlbum();
        album.setMediaCount(Math.max(0, album.getMediaCount() - 1));
        album.setTotalSizeBytes(Math.max(0, album.getTotalSizeBytes() - media.getFileSizeBytes()));
        albumRepository.save(album);

        // Update photographer storage
        Photographer photographer = media.getPhotographer();
        photographer.setStorageUsedBytes(Math.max(0, photographer.getStorageUsedBytes() - media.getFileSizeBytes()));
        photographerRepository.save(photographer);

        log.info("Media deleted: {}", mediaId);
    }

    @Transactional
    public void incrementViewCount(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
            .orElseThrow(() -> new RuntimeException("Media not found"));

        media.setViewCount(media.getViewCount() + 1);
        mediaRepository.save(media);
    }

    @Transactional
    public void incrementDownloadCount(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
            .orElseThrow(() -> new RuntimeException("Media not found"));

        media.setDownloadCount(media.getDownloadCount() + 1);
        mediaRepository.save(media);

        // Also increment album download count
        Album album = media.getAlbum();
        album.setTotalDownloads(album.getTotalDownloads() + 1);
        albumRepository.save(album);
    }

    private MediaDTO toDTO(Media media) {
        return MediaDTO.builder()
            .id(media.getId())
            .albumId(media.getAlbum().getId())
            .filename(media.getFilename())
            .originalFilename(media.getOriginalFilename())
            .mimeType(media.getMimeType())
            .fileSizeBytes(media.getFileSizeBytes())
            .originalUrl(media.getOriginalUrl())
            .optimizedUrl(media.getOptimizedUrl())
            .thumbnailUrl(media.getThumbnailUrl())
            .previewUrl(media.getPreviewUrl())
            .videoThumbnailUrl(media.getVideoThumbnailUrl())
            .videoDurationSeconds(media.getVideoDurationSeconds())
            .width(media.getWidth())
            .height(media.getHeight())
            .aspectRatio(media.getAspectRatio())
            .orientation(media.getOrientation())
            .cameraMake(media.getCameraMake())
            .cameraModel(media.getCameraModel())
            .focalLength(media.getFocalLength())
            .aperture(media.getAperture())
            .iso(media.getIso())
            .takenAt(media.getTakenAt())
            .tagsAuto(media.getTagsAuto())
            .colorPalette(media.getColorPalette())
            .sortOrder(media.getSortOrder())
            .isCover(media.getIsCover())
            .viewCount(media.getViewCount())
            .downloadCount(media.getDownloadCount())
            .processingStatus(media.getProcessingStatus().name())
            .blurHash(media.getBlurHash())
            .uploadedAt(media.getUploadedAt())
            .processedAt(media.getProcessedAt())
            .build();
    }
}

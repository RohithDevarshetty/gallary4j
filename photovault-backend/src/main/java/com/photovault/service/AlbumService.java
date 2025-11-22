package com.photovault.service;

import com.photovault.dto.AlbumDTO;
import com.photovault.dto.CreateAlbumRequest;
import com.photovault.entity.Album;
import com.photovault.entity.Photographer;
import com.photovault.repository.AlbumRepository;
import com.photovault.repository.PhotographerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final PhotographerRepository photographerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @CacheEvict(value = "albums", allEntries = true)
    public AlbumDTO createAlbum(String photographerEmail, CreateAlbumRequest request) {
        log.info("Creating album for photographer: {}", photographerEmail);

        Photographer photographer = photographerRepository.findActiveByEmail(photographerEmail)
            .orElseThrow(() -> new RuntimeException("Photographer not found"));

        // Check album limit
        long albumCount = albumRepository.countByPhotographerId(photographer.getId());
        if (albumCount >= photographer.getAlbumsLimit()) {
            throw new RuntimeException("Album limit reached for current plan");
        }

        Album album = Album.builder()
            .photographer(photographer)
            .title(request.getTitle())
            .description(request.getDescription())
            .eventDate(request.getEventDate())
            .clientName(request.getClientName())
            .clientEmail(request.getClientEmail())
            .clientPhone(request.getClientPhone())
            .allowDownloads(request.getAllowDownloads() != null ? request.getAllowDownloads() : true)
            .allowSharing(request.getAllowSharing() != null ? request.getAllowSharing() : true)
            .enableSelection(request.getEnableSelection() != null ? request.getEnableSelection() : false)
            .maxSelections(request.getMaxSelections())
            .watermarkPhotos(request.getWatermarkPhotos() != null ? request.getWatermarkPhotos() : false)
            .tags(request.getTags())
            .category(request.getCategory())
            .isActive(true)
            .isPublic(true)
            .build();

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            album.setRequiresPassword(true);
            album.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        album = albumRepository.save(album);

        // Update photographer album count
        photographer.setAlbumsCount(photographer.getAlbumsCount() + 1);
        photographerRepository.save(photographer);

        log.info("Album created: {}", album.getId());
        return toDTO(album);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "albums", key = "#albumId")
    public AlbumDTO getAlbum(UUID albumId) {
        Album album = albumRepository.findById(albumId)
            .orElseThrow(() -> new RuntimeException("Album not found"));
        return toDTO(album);
    }

    @Transactional(readOnly = true)
    public AlbumDTO getAlbumBySlug(String slug) {
        Album album = albumRepository.findActiveBySlug(slug)
            .orElseThrow(() -> new RuntimeException("Album not found"));
        return toDTO(album);
    }

    @Transactional(readOnly = true)
    public Page<AlbumDTO> getPhotographerAlbums(String photographerEmail, Pageable pageable) {
        Photographer photographer = photographerRepository.findActiveByEmail(photographerEmail)
            .orElseThrow(() -> new RuntimeException("Photographer not found"));

        return albumRepository.findByPhotographerId(photographer.getId(), pageable)
            .map(this::toDTO);
    }

    @Transactional
    @CacheEvict(value = "albums", key = "#albumId")
    public AlbumDTO updateAlbum(UUID albumId, CreateAlbumRequest request) {
        Album album = albumRepository.findById(albumId)
            .orElseThrow(() -> new RuntimeException("Album not found"));

        album.setTitle(request.getTitle());
        album.setDescription(request.getDescription());
        album.setEventDate(request.getEventDate());
        album.setClientName(request.getClientName());
        album.setClientEmail(request.getClientEmail());
        album.setClientPhone(request.getClientPhone());

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            album.setRequiresPassword(true);
            album.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        album = albumRepository.save(album);
        return toDTO(album);
    }

    @Transactional
    @CacheEvict(value = "albums", key = "#albumId")
    public void deleteAlbum(UUID albumId) {
        Album album = albumRepository.findById(albumId)
            .orElseThrow(() -> new RuntimeException("Album not found"));

        album.setDeletedAt(Instant.now());
        albumRepository.save(album);

        // Update photographer album count
        Photographer photographer = album.getPhotographer();
        photographer.setAlbumsCount(Math.max(0, photographer.getAlbumsCount() - 1));
        photographerRepository.save(photographer);

        log.info("Album deleted: {}", albumId);
    }

    @Transactional
    public void incrementViewCount(UUID albumId) {
        Album album = albumRepository.findById(albumId)
            .orElseThrow(() -> new RuntimeException("Album not found"));

        album.setViewCount(album.getViewCount() + 1);
        album.setLastViewedAt(Instant.now());
        albumRepository.save(album);
    }

    private AlbumDTO toDTO(Album album) {
        return AlbumDTO.builder()
            .id(album.getId())
            .title(album.getTitle())
            .description(album.getDescription())
            .slug(album.getSlug())
            .eventDate(album.getEventDate())
            .clientName(album.getClientName())
            .clientEmail(album.getClientEmail())
            .requiresPassword(album.getRequiresPassword())
            .expiresAt(album.getExpiresAt())
            .isActive(album.getIsActive())
            .isPublic(album.getIsPublic())
            .allowDownloads(album.getAllowDownloads())
            .allowSharing(album.getAllowSharing())
            .enableSelection(album.getEnableSelection())
            .maxSelections(album.getMaxSelections())
            .watermarkPhotos(album.getWatermarkPhotos())
            .viewCount(album.getViewCount())
            .uniqueVisitors(album.getUniqueVisitors())
            .totalDownloads(album.getTotalDownloads())
            .mediaCount(album.getMediaCount())
            .totalSizeBytes(album.getTotalSizeBytes())
            .coverPhotoId(album.getCoverPhotoId())
            .tags(album.getTags())
            .category(album.getCategory())
            .createdAt(album.getCreatedAt())
            .publishedAt(album.getPublishedAt())
            .build();
    }
}

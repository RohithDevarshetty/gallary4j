package com.photovault.controller;

import com.photovault.dto.MediaDTO;
import com.photovault.entity.Media;
import com.photovault.repository.AlbumRepository;
import com.photovault.repository.MediaRepository;
import com.photovault.service.MediaService;
import com.photovault.service.StorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class MediaController {

    private final MediaService mediaService;
    private final MediaRepository mediaRepository;
    private final AlbumRepository albumRepository;
    private final StorageService storageService;
    private final com.photovault.repository.ProcessingJobRepository processingJobRepository;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<Map<String, Object>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("albumId") UUID albumId,
            @RequestParam(value = "folderPath", required = false) String folderPath,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Upload request for album: {} folder: {} from: {}", albumId, folderPath, userDetails.getUsername());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            Media media = mediaService.createMedia(albumId, file, userDetails.getUsername(), folderPath);

            // Look up the job that was created for this media (may be null for unsupported types)
            var job = processingJobRepository.findByMediaId(media.getId()).orElse(null);

            var response = new java.util.HashMap<String, Object>();
            response.put("mediaId", media.getId());
            response.put("status", media.getProcessingStatus().name());
            response.put("message", "File uploaded successfully");
            if (job != null) response.put("jobId", job.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaDTO> getMedia(@PathVariable UUID mediaId) {
        MediaDTO media = mediaService.getMedia(mediaId);
        mediaService.incrementViewCount(mediaId);
        return ResponseEntity.ok(media);
    }

    @GetMapping("/album/{albumId}")
    public ResponseEntity<Page<MediaDTO>> getAlbumMedia(
            @PathVariable UUID albumId,
            @PageableDefault(size = 50) Pageable pageable) {

        Page<MediaDTO> media = mediaService.getAlbumMedia(albumId, pageable);
        return ResponseEntity.ok(media);
    }

    @DeleteMapping("/{mediaId}")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<Void> deleteMedia(
            @PathVariable UUID mediaId,
            @AuthenticationPrincipal UserDetails userDetails) {
        mediaService.deleteMedia(mediaId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/v1/media/batch
     * Body: list of media UUIDs to delete.
     * All items must belong to the authenticated photographer.
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<Map<String, Object>> deleteMediaBatch(
            @RequestBody List<UUID> ids,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No IDs provided"));
        }

        mediaService.deleteMediaBatch(ids, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("deleted", ids.size()));
    }

    @PostMapping("/batch-status")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<Map<String, String>> getBatchStatus(@RequestBody List<UUID> ids) {
        Map<String, String> result = new java.util.HashMap<>();
        for (UUID id : ids) {
            mediaRepository.findById(id).ifPresent(m ->
                result.put(id.toString(), m.getProcessingStatus().name()));
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{mediaId}/download")
    public ResponseEntity<Void> trackDownload(@PathVariable UUID mediaId) {
        mediaService.incrementDownloadCount(mediaId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/album/{albumId}/zip")
    public void downloadAlbumZip(@PathVariable UUID albumId, HttpServletResponse response) throws IOException {
        var album = albumRepository.findById(albumId)
            .orElseThrow(() -> new IllegalArgumentException("Album not found"));

        if (Boolean.FALSE.equals(album.getAllowDownloads())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Downloads are disabled for this album");
            return;
        }

        List<Media> mediaList = mediaRepository.findAllByAlbumId(albumId);

        String zipFilename = album.getTitle().replaceAll("[^a-zA-Z0-9 _-]", "").trim() + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFilename + "\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (Media media : mediaList) {
                if (media.getOriginalUrl() == null) continue;
                try {
                    byte[] bytes = storageService.downloadFile(media.getOriginalUrl());
                    String name = media.getOriginalFilename() != null
                        ? media.getOriginalFilename()
                        : media.getFilename();
                    String entryName = (media.getFolderPath() != null && !media.getFolderPath().isBlank())
                        ? media.getFolderPath() + "/" + name
                        : name;
                    zos.putNextEntry(new ZipEntry(entryName));
                    zos.write(bytes);
                    zos.closeEntry();
                    mediaService.incrementDownloadCount(media.getId());
                } catch (Exception e) {
                    log.warn("Skipping media {} in zip: {}", media.getId(), e.getMessage());
                }
            }
        }
    }
}

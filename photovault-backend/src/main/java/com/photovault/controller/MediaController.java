package com.photovault.controller;

import com.photovault.dto.MediaDTO;
import com.photovault.entity.Media;
import com.photovault.service.MediaService;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<Map<String, Object>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("albumId") UUID albumId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Upload request for album: {} from: {}", albumId, userDetails.getUsername());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            Media media = mediaService.createMedia(albumId, file, userDetails.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mediaId", media.getId(),
                "status", media.getProcessingStatus().name(),
                "message", "File uploaded successfully"
            ));

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
    public ResponseEntity<Void> deleteMedia(@PathVariable UUID mediaId) {
        mediaService.deleteMedia(mediaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{mediaId}/download")
    public ResponseEntity<Void> trackDownload(@PathVariable UUID mediaId) {
        mediaService.incrementDownloadCount(mediaId);
        return ResponseEntity.ok().build();
    }
}

package com.photovault.controller;

import com.photovault.dto.AlbumDTO;
import com.photovault.dto.CreateAlbumRequest;
import com.photovault.service.AlbumService;
import jakarta.validation.Valid;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/albums")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class AlbumController {

    private final AlbumService albumService;

    @PostMapping
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<AlbumDTO> createAlbum(
            @Valid @RequestBody CreateAlbumRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Create album request from: {}", userDetails.getUsername());
        AlbumDTO album = albumService.createAlbum(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(album);
    }

    @GetMapping("/{albumId}")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<AlbumDTO> getAlbum(@PathVariable UUID albumId) {
        AlbumDTO album = albumService.getAlbum(albumId);
        return ResponseEntity.ok(album);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<AlbumDTO> getAlbumBySlug(@PathVariable String slug) {
        AlbumDTO album = albumService.getAlbumBySlug(slug);
        albumService.incrementViewCount(album.getId());
        return ResponseEntity.ok(album);
    }

    @GetMapping
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<Page<AlbumDTO>> getMyAlbums(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AlbumDTO> albums = albumService.getPhotographerAlbums(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(albums);
    }

    @PutMapping("/{albumId}")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<AlbumDTO> updateAlbum(
            @PathVariable UUID albumId,
            @Valid @RequestBody CreateAlbumRequest request) {

        AlbumDTO album = albumService.updateAlbum(albumId, request);
        return ResponseEntity.ok(album);
    }

    @PostMapping("/slug/{slug}/verify")
    public ResponseEntity<Map<String, Object>> verifyAlbumPassword(
            @PathVariable String slug,
            @RequestBody Map<String, String> body) {

        String password = body.getOrDefault("password", "");
        boolean verified = albumService.verifyAlbumPassword(slug, password);
        if (verified) {
            return ResponseEntity.ok(Map.of("verified", true));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Incorrect password"));
        }
    }

    @DeleteMapping("/{albumId}")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<Void> deleteAlbum(
            @PathVariable UUID albumId,
            @AuthenticationPrincipal UserDetails userDetails) {
        albumService.deleteAlbum(albumId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}

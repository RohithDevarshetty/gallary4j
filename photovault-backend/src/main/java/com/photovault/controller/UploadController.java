package com.photovault.controller;

import com.photovault.dto.UploadSessionDTO;
import com.photovault.dto.UploadSessionCreateRequest;
import com.photovault.dto.UploadChunkRequest;
import com.photovault.dto.UploadCompleteRequest;
import com.photovault.entity.UploadSession;
import com.photovault.service.UploadSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Slf4j
public class UploadController {

    private final UploadSessionService uploadSessionService;

    /**
     * Initialize a new chunked upload session
     * POST /api/v1/uploads/sessions
     */
    @PostMapping("/sessions")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<UploadSessionDTO> createUploadSession(
            @Valid @RequestBody UploadSessionCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Creating upload session for album {} with {} files",
                request.getAlbumId(), request.getTotalFiles());

        UploadSession session = uploadSessionService.createSession(
                userDetails.getUsername(),
                request.getAlbumId(),
                request.getTotalFiles(),
                request.getTotalBytes(),
                request.getClientType(),
                request.getClientVersion()
        );

        UploadSessionDTO dto = uploadSessionService.toDTO(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Get upload session status
     * GET /api/v1/uploads/sessions/{sessionId}
     */
    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<UploadSessionDTO> getUploadSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Fetching upload session: {}", sessionId);

        UploadSession session = uploadSessionService.getSession(sessionId, userDetails.getUsername());
        UploadSessionDTO dto = uploadSessionService.toDTO(session);

        return ResponseEntity.ok(dto);
    }

    /**
     * Upload a file chunk
     * POST /api/v1/uploads/sessions/{sessionId}/chunks
     */
    @PostMapping("/sessions/{sessionId}/chunks")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<Void> uploadChunk(
            @PathVariable UUID sessionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("chunkNumber") int chunkNumber,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam("filename") String filename,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Uploading chunk {}/{} for session {}", chunkNumber, totalChunks, sessionId);

        uploadSessionService.processChunk(
                sessionId,
                userDetails.getUsername(),
                file,
                chunkNumber,
                totalChunks,
                filename
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Complete upload session
     * POST /api/v1/uploads/sessions/{sessionId}/complete
     */
    @PostMapping("/sessions/{sessionId}/complete")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<Void> completeUploadSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody UploadCompleteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Completing upload session: {}", sessionId);

        uploadSessionService.completeSession(sessionId, userDetails.getUsername());

        return ResponseEntity.ok().build();
    }

    /**
     * Cancel upload session
     * DELETE /api/v1/uploads/sessions/{sessionId}
     */
    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<Void> cancelUploadSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Cancelling upload session: {}", sessionId);

        uploadSessionService.cancelSession(sessionId, userDetails.getUsername());

        return ResponseEntity.noContent().build();
    }

    /**
     * Get presigned URLs for direct upload to R2/S3
     * POST /api/v1/uploads/presigned-urls
     */
    @PostMapping("/presigned-urls")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<UploadSessionDTO> generatePresignedUrls(
            @Valid @RequestBody UploadSessionCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Generating presigned URLs for album {} with {} files",
                request.getAlbumId(), request.getTotalFiles());

        UploadSession session = uploadSessionService.generatePresignedUrls(
                userDetails.getUsername(),
                request.getAlbumId(),
                request.getTotalFiles(),
                request.getFileNames()
        );

        UploadSessionDTO dto = uploadSessionService.toDTO(session);
        return ResponseEntity.ok(dto);
    }
}

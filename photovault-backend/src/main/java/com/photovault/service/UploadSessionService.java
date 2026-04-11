package com.photovault.service;

import com.photovault.dto.PresignedUrlDTO;
import com.photovault.dto.UploadSessionDTO;
import com.photovault.entity.Album;
import com.photovault.entity.Photographer;
import com.photovault.entity.UploadSession;
import com.photovault.exception.ResourceNotFoundException;
import com.photovault.repository.AlbumRepository;
import com.photovault.repository.PhotographerRepository;
import com.photovault.repository.UploadSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadSessionService {

    private final UploadSessionRepository uploadSessionRepository;
    private final PhotographerRepository photographerRepository;
    private final AlbumRepository albumRepository;
    private final StorageService storageService;
    private final R2StorageService r2StorageService;
    private final MediaService mediaService;

    @Value("${storage.type:local}")
    private String storageType;

    @Value("${upload.chunk-size:10485760}") // 10MB default
    private long chunkSize;

    @Transactional
    public UploadSession createSession(String email, UUID albumId, int totalFiles,
                                       long totalBytes, String clientType, String clientVersion) {
        Photographer photographer = photographerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Photographer", "email", email));

        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResourceNotFoundException("Album", "id", albumId));

        if (!album.getPhotographer().getId().equals(photographer.getId())) {
            throw new IllegalArgumentException("Album does not belong to this photographer");
        }

        UploadSession session = UploadSession.builder()
                .photographer(photographer)
                .album(album)
                .totalFiles(totalFiles)
                .uploadedFiles(0)
                .totalBytes(totalBytes)
                .uploadedBytes(0L)
                .status("active")
                .clientType(clientType)
                .clientVersion(clientVersion)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        return uploadSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public UploadSession getSession(UUID sessionId, String email) {
        UploadSession session = uploadSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("UploadSession", "id", sessionId));

        if (!session.getPhotographer().getEmail().equals(email)) {
            throw new IllegalArgumentException("Upload session does not belong to this photographer");
        }

        return session;
    }

    @Transactional
    public void processChunk(UUID sessionId, String email, MultipartFile file,
                            int chunkNumber, int totalChunks, String filename) {
        UploadSession session = getSession(sessionId, email);

        if (session.getStatus() != UploadSession.Status.ACTIVE) {
            throw new IllegalStateException("Upload session is not active");
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setStatus(UploadSession.Status.EXPIRED);
            uploadSessionRepository.save(session);
            throw new IllegalStateException("Upload session has expired");
        }

        try {
            // Store chunk temporarily
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "photovault", sessionId.toString());
            Files.createDirectories(tempDir);

            Path chunkPath = tempDir.resolve(filename + ".chunk." + chunkNumber);
            file.transferTo(chunkPath.toFile());

            // Update session progress
            session.setUploadedBytes(session.getUploadedBytes() + file.getSize());

            // If all chunks received, assemble file
            if (chunkNumber == totalChunks) {
                assembleAndUploadFile(session, tempDir, filename, totalChunks);
                session.setUploadedFiles(session.getUploadedFiles() + 1);
            }

            uploadSessionRepository.save(session);

            log.debug("Processed chunk {}/{} for file: {}", chunkNumber, totalChunks, filename);

        } catch (IOException e) {
            log.error("Failed to process chunk", e);
            throw new RuntimeException("Failed to process upload chunk", e);
        }
    }

    private void assembleAndUploadFile(UploadSession session, Path tempDir, String filename, int totalChunks) throws IOException {
        Path assembledFile = tempDir.resolve(filename);

        // Combine all chunks
        try (var output = Files.newOutputStream(assembledFile)) {
            for (int i = 1; i <= totalChunks; i++) {
                Path chunkPath = tempDir.resolve(filename + ".chunk." + i);
                Files.copy(chunkPath, output);
                Files.deleteIfExists(chunkPath);
            }
        }

        // Upload to storage
        byte[] fileBytes = Files.readAllBytes(assembledFile);
        String mimeType = Files.probeContentType(assembledFile);

        String url = storageService.uploadBytes(
                fileBytes,
                session.getAlbum().getId().toString(),
                "originals",
                filename,
                mimeType
        );

        // Create media record
        mediaService.createMediaFromUpload(
                session.getAlbum().getId(),
                session.getPhotographer().getId(),
                filename,
                mimeType,
                fileBytes.length,
                url
        );

        // Cleanup
        Files.deleteIfExists(assembledFile);
        log.info("Assembled and uploaded file: {}", filename);
    }

    @Transactional
    public void completeSession(UUID sessionId, String email) {
        UploadSession session = getSession(sessionId, email);

        session.setStatus(UploadSession.Status.COMPLETED);
        session.setCompletedAt(Instant.now());

        uploadSessionRepository.save(session);

        log.info("Upload session completed: {} ({} files, {} MB)",
                sessionId,
                session.getUploadedFiles(),
                session.getUploadedBytes() / 1024 / 1024);
    }

    @Transactional
    public void cancelSession(UUID sessionId, String email) {
        UploadSession session = getSession(sessionId, email);

        session.setStatus(UploadSession.Status.FAILED);
        uploadSessionRepository.save(session);

        // Cleanup temp files
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "photovault", sessionId.toString());
        try {
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                log.warn("Failed to delete temp file: {}", path);
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup temp directory: {}", tempDir);
        }

        log.info("Upload session cancelled: {}", sessionId);
    }

    @Transactional
    public UploadSession generatePresignedUrls(String email, UUID albumId, int totalFiles, List<String> fileNames) {
        if ("r2".equals(storageType)) {
            UploadSession session = createSession(email, albumId, totalFiles, 0L, "web", "1.0");

            List<PresignedUrlDTO> presignedUrls = new ArrayList<>();
            for (String fileName : fileNames) {
                String url = r2StorageService.generatePresignedUploadUrl(
                        albumId.toString(),
                        fileName
                );
                presignedUrls.add(PresignedUrlDTO.builder()
                        .filename(fileName)
                        .uploadUrl(url)
                        .expiresAt(session.getExpiresAt())
                        .build());
            }

            session.setPresignedUrls(presignedUrls);
            return uploadSessionRepository.save(session);

        } else {
            throw new UnsupportedOperationException("Presigned URLs only supported with R2 storage");
        }
    }

    public UploadSessionDTO toDTO(UploadSession session) {
        return UploadSessionDTO.builder()
                .id(session.getId())
                .albumId(session.getAlbum().getId())
                .totalFiles(session.getTotalFiles())
                .uploadedFiles(session.getUploadedFiles())
                .totalBytes(session.getTotalBytes())
                .uploadedBytes(session.getUploadedBytes())
                .status(session.getStatus())
                .expiresAt(session.getExpiresAt())
                .completedAt(session.getCompletedAt())
                .presignedUrls(session.getPresignedUrls())
                .progress(calculateProgress(session))
                .build();
    }

    private double calculateProgress(UploadSession session) {
        if (session.getTotalBytes() == 0) {
            return 0.0;
        }
        return (double) session.getUploadedBytes() / session.getTotalBytes() * 100.0;
    }
}

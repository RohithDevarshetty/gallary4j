package com.photovault.service;

import com.photovault.entity.Album;
import com.photovault.entity.Photographer;
import com.photovault.entity.UploadSession;
import com.photovault.exception.ResourceNotFoundException;
import com.photovault.repository.AlbumRepository;
import com.photovault.repository.PhotographerRepository;
import com.photovault.repository.UploadSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for UploadSessionService — covers the enum type bug fix (status field
 * was set with String "active" but the field is UploadSession.Status enum).
 */
@ExtendWith(MockitoExtension.class)
class UploadSessionServiceTest {

    @Mock private UploadSessionRepository uploadSessionRepository;
    @Mock private PhotographerRepository photographerRepository;
    @Mock private AlbumRepository albumRepository;
    @Mock private StorageService storageService;
    @Mock private MediaService mediaService;
    // r2StorageService is optional, left null intentionally

    @InjectMocks private UploadSessionService uploadSessionService;

    private Photographer photographer;
    private Album album;
    private UploadSession session;

    @BeforeEach
    void setUp() {
        photographer = Photographer.builder()
            .id(UUID.randomUUID())
            .email("photo@studio.com")
            .build();

        album = Album.builder()
            .id(UUID.randomUUID())
            .photographer(photographer)
            .title("Test Album")
            .build();

        session = UploadSession.builder()
            .id(UUID.randomUUID())
            .photographer(photographer)
            .album(album)
            .totalFiles(5)
            .uploadedFiles(0)
            .totalBytes(1024L * 1024L * 50L) // 50MB
            .uploadedBytes(0L)
            .status(UploadSession.Status.ACTIVE)
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }

    // ── createSession ────────────────────────────────────────────────────────

    @Test
    void createSession_validInputs_createsSessionWithActiveStatus() {
        when(photographerRepository.findByEmail(photographer.getEmail()))
            .thenReturn(Optional.of(photographer));
        when(albumRepository.findById(album.getId())).thenReturn(Optional.of(album));
        when(uploadSessionRepository.save(any(UploadSession.class))).thenAnswer(inv -> inv.getArgument(0));

        UploadSession created = uploadSessionService.createSession(
            photographer.getEmail(), album.getId(), 5, 1024L * 1024L, "ios", "1.0"
        );

        assertNotNull(created);
        // The bug: status was set as String "active" instead of enum ACTIVE
        assertEquals(UploadSession.Status.ACTIVE, created.getStatus());
        assertEquals(5, created.getTotalFiles());
        assertNotNull(created.getExpiresAt());
    }

    @Test
    void createSession_setsExpiryTo24Hours() {
        when(photographerRepository.findByEmail(photographer.getEmail()))
            .thenReturn(Optional.of(photographer));
        when(albumRepository.findById(album.getId())).thenReturn(Optional.of(album));
        when(uploadSessionRepository.save(any(UploadSession.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now().plusSeconds(23 * 3600);

        UploadSession created = uploadSessionService.createSession(
            photographer.getEmail(), album.getId(), 1, 100L, "web", "1.0"
        );

        assertTrue(created.getExpiresAt().isAfter(before));
    }

    @Test
    void createSession_photographerNotFound_throwsException() {
        when(photographerRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            uploadSessionService.createSession("notfound@example.com", album.getId(), 1, 100L, "web", "1.0")
        );
        verify(uploadSessionRepository, never()).save(any());
    }

    @Test
    void createSession_albumNotFound_throwsException() {
        when(photographerRepository.findByEmail(photographer.getEmail()))
            .thenReturn(Optional.of(photographer));
        when(albumRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            uploadSessionService.createSession(photographer.getEmail(), UUID.randomUUID(), 1, 100L, "web", "1.0")
        );
        verify(uploadSessionRepository, never()).save(any());
    }

    @Test
    void createSession_albumBelongsToDifferentPhotographer_throwsException() {
        Photographer other = Photographer.builder().id(UUID.randomUUID()).email("other@studio.com").build();
        Album otherAlbum = Album.builder()
            .id(UUID.randomUUID())
            .photographer(other)
            .build();

        when(photographerRepository.findByEmail(photographer.getEmail()))
            .thenReturn(Optional.of(photographer));
        when(albumRepository.findById(otherAlbum.getId())).thenReturn(Optional.of(otherAlbum));

        assertThrows(IllegalArgumentException.class, () ->
            uploadSessionService.createSession(photographer.getEmail(), otherAlbum.getId(), 1, 100L, "web", "1.0")
        );
    }

    // ── getSession ───────────────────────────────────────────────────────────

    @Test
    void getSession_validOwner_returnsSession() {
        when(uploadSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        UploadSession result = uploadSessionService.getSession(session.getId(), photographer.getEmail());

        assertNotNull(result);
        assertEquals(session.getId(), result.getId());
    }

    @Test
    void getSession_notFound_throwsException() {
        UUID unknown = UUID.randomUUID();
        when(uploadSessionRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            uploadSessionService.getSession(unknown, photographer.getEmail())
        );
    }

    @Test
    void getSession_wrongOwner_throwsException() {
        when(uploadSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () ->
            uploadSessionService.getSession(session.getId(), "intruder@example.com")
        );
    }

    // ── completeSession ──────────────────────────────────────────────────────

    @Test
    void completeSession_setsStatusAndTimestamp() {
        when(uploadSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(uploadSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        uploadSessionService.completeSession(session.getId(), photographer.getEmail());

        verify(uploadSessionRepository).save(argThat(s ->
            s.getStatus() == UploadSession.Status.COMPLETED &&
            s.getCompletedAt() != null
        ));
    }

    // ── cancelSession ────────────────────────────────────────────────────────

    @Test
    void cancelSession_setsStatusToFailed() {
        when(uploadSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(uploadSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        uploadSessionService.cancelSession(session.getId(), photographer.getEmail());

        verify(uploadSessionRepository).save(argThat(s ->
            s.getStatus() == UploadSession.Status.FAILED
        ));
    }

    // ── toDTO ────────────────────────────────────────────────────────────────

    @Test
    void toDTO_convertsStatusEnumToString() {
        // This tests the other bug fix: getStatus().name() instead of getStatus()
        session = UploadSession.builder()
            .id(UUID.randomUUID())
            .photographer(photographer)
            .album(album)
            .totalFiles(3)
            .uploadedFiles(1)
            .totalBytes(100L)
            .uploadedBytes(33L)
            .status(UploadSession.Status.ACTIVE)
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        var dto = uploadSessionService.toDTO(session);

        assertNotNull(dto);
        assertEquals("ACTIVE", dto.getStatus()); // String, not enum
        assertEquals(3, dto.getTotalFiles());
        assertEquals(1, dto.getUploadedFiles());
    }

    @Test
    void toDTO_calculatesProgressCorrectly() {
        session = UploadSession.builder()
            .id(UUID.randomUUID())
            .photographer(photographer)
            .album(album)
            .totalFiles(4)
            .uploadedFiles(2)
            .totalBytes(1000L)
            .uploadedBytes(500L)
            .status(UploadSession.Status.ACTIVE)
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        var dto = uploadSessionService.toDTO(session);

        assertEquals(50.0, dto.getProgress(), 0.001);
    }

    @Test
    void toDTO_zeroTotalBytes_progressIsZero() {
        session = UploadSession.builder()
            .id(UUID.randomUUID())
            .photographer(photographer)
            .album(album)
            .totalFiles(1)
            .uploadedFiles(0)
            .totalBytes(0L)
            .uploadedBytes(0L)
            .status(UploadSession.Status.ACTIVE)
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        var dto = uploadSessionService.toDTO(session);

        assertEquals(0.0, dto.getProgress(), 0.001);
    }
}

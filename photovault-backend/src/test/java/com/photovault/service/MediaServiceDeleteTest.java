package com.photovault.service;

import com.photovault.entity.Album;
import com.photovault.entity.Media;
import com.photovault.entity.Photographer;
import com.photovault.messaging.MediaEventProducer;
import com.photovault.repository.AlbumRepository;
import com.photovault.repository.MediaRepository;
import com.photovault.repository.PhotographerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceDeleteTest {

    @Mock private MediaRepository mediaRepository;
    @Mock private AlbumRepository albumRepository;
    @Mock private PhotographerRepository photographerRepository;
    @Mock private StorageService storageService;
    @Mock private MediaEventProducer mediaEventProducer;
    @Mock private ProcessingJobService processingJobService;

    @InjectMocks private MediaService mediaService;

    private Photographer owner;
    private Photographer stranger;
    private Album album;
    private Media media;

    @BeforeEach
    void setUp() {
        owner = Photographer.builder()
            .id(UUID.randomUUID())
            .email("owner@studio.com")
            .storageUsedBytes(50_000L)
            .build();

        stranger = Photographer.builder()
            .id(UUID.randomUUID())
            .email("stranger@other.com")
            .storageUsedBytes(0L)
            .build();

        album = Album.builder()
            .id(UUID.randomUUID())
            .photographer(owner)
            .title("Test Album")
            .mediaCount(5)
            .totalSizeBytes(50_000L)
            .build();

        media = Media.builder()
            .id(UUID.randomUUID())
            .album(album)
            .photographer(owner)
            .filename("photo.jpg")
            .originalFilename("photo.jpg")
            .mimeType("image/jpeg")
            .fileSizeBytes(10_000L)
            .processingStatus(Media.ProcessingStatus.COMPLETED)
            .build();
    }

    // ── Single delete — ownership ─────────────────────────────────────────────

    @Test
    void deleteMedia_ownerEmail_softDeletes() {
        when(mediaRepository.findById(media.getId())).thenReturn(Optional.of(media));

        mediaService.deleteMedia(media.getId(), owner.getEmail());

        verify(mediaRepository).save(argThat(m -> m.getDeletedAt() != null));
    }

    @Test
    void deleteMedia_nonOwnerEmail_throwsAccessDeniedException() {
        when(mediaRepository.findById(media.getId())).thenReturn(Optional.of(media));

        assertThrows(AccessDeniedException.class,
            () -> mediaService.deleteMedia(media.getId(), stranger.getEmail()));

        verify(mediaRepository, never()).save(any());
    }

    @Test
    void deleteMedia_notFound_throwsRuntimeException() {
        UUID unknown = UUID.randomUUID();
        when(mediaRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
            () -> mediaService.deleteMedia(unknown, owner.getEmail()));
    }

    // ── Single delete — side effects ──────────────────────────────────────────

    @Test
    void deleteMedia_decrementsAlbumMediaCount() {
        when(mediaRepository.findById(media.getId())).thenReturn(Optional.of(media));

        mediaService.deleteMedia(media.getId(), owner.getEmail());

        verify(albumRepository).save(argThat(a -> a.getMediaCount() == 4));
    }

    @Test
    void deleteMedia_decrementsAlbumTotalSize() {
        when(mediaRepository.findById(media.getId())).thenReturn(Optional.of(media));

        mediaService.deleteMedia(media.getId(), owner.getEmail());

        // 50000 - 10000 = 40000
        verify(albumRepository).save(argThat(a -> a.getTotalSizeBytes() == 40_000L));
    }

    @Test
    void deleteMedia_decrementsPhotographerStorageUsed() {
        when(mediaRepository.findById(media.getId())).thenReturn(Optional.of(media));

        mediaService.deleteMedia(media.getId(), owner.getEmail());

        // 50000 - 10000 = 40000
        verify(photographerRepository).save(argThat(p -> p.getStorageUsedBytes() == 40_000L));
    }

    @Test
    void deleteMedia_countAtZero_clampsAlbumCountToZero() {
        album.setMediaCount(0);
        when(mediaRepository.findById(media.getId())).thenReturn(Optional.of(media));

        mediaService.deleteMedia(media.getId(), owner.getEmail());

        verify(albumRepository).save(argThat(a -> a.getMediaCount() == 0));
    }

    // ── Bulk delete ───────────────────────────────────────────────────────────

    @Test
    void deleteMediaBatch_allOwned_softDeletesAll() {
        Media m2 = Media.builder().id(UUID.randomUUID()).album(album).photographer(owner)
            .fileSizeBytes(5_000L).processingStatus(Media.ProcessingStatus.COMPLETED).build();
        List<UUID> ids = List.of(media.getId(), m2.getId());
        when(mediaRepository.findAllById(ids)).thenReturn(List.of(media, m2));

        mediaService.deleteMediaBatch(ids, owner.getEmail());

        verify(mediaRepository).saveAll(argThat(saved -> {
            List<Media> list = (List<Media>) saved;
            return list.size() == 2 && list.stream().allMatch(m -> m.getDeletedAt() != null);
        }));
    }

    @Test
    void deleteMediaBatch_anyNonOwned_throwsAccessDeniedException() {
        Media foreign = Media.builder().id(UUID.randomUUID()).album(album).photographer(stranger)
            .fileSizeBytes(1_000L).processingStatus(Media.ProcessingStatus.COMPLETED).build();
        List<UUID> ids = List.of(media.getId(), foreign.getId());
        when(mediaRepository.findAllById(ids)).thenReturn(List.of(media, foreign));

        assertThrows(AccessDeniedException.class,
            () -> mediaService.deleteMediaBatch(ids, owner.getEmail()));

        verify(mediaRepository, never()).saveAll(any());
    }

    @Test
    void deleteMediaBatch_emptyList_doesNothing() {
        mediaService.deleteMediaBatch(List.of(), owner.getEmail());

        verify(mediaRepository, never()).findAllById(any());
        verify(mediaRepository, never()).saveAll(any());
    }

    @Test
    void deleteMediaBatch_updatesAlbumStatsForEachAffectedAlbum() {
        album.setMediaCount(3);
        album.setTotalSizeBytes(30_000L);
        Media m2 = Media.builder().id(UUID.randomUUID()).album(album).photographer(owner)
            .fileSizeBytes(5_000L).processingStatus(Media.ProcessingStatus.COMPLETED).build();
        List<UUID> ids = List.of(media.getId(), m2.getId());
        when(mediaRepository.findAllById(ids)).thenReturn(List.of(media, m2));

        mediaService.deleteMediaBatch(ids, owner.getEmail());

        // album had 3 items; we delete 2 → 1 remaining; size 30000 - 15000 = 15000
        verify(albumRepository).save(argThat(a ->
            a.getMediaCount() == 1 && a.getTotalSizeBytes() == 15_000L));
    }

    @Test
    void deleteMediaBatch_updatesPhotographerStorageForAllDeletedBytes() {
        owner.setStorageUsedBytes(100_000L);
        Media m2 = Media.builder().id(UUID.randomUUID()).album(album).photographer(owner)
            .fileSizeBytes(5_000L).processingStatus(Media.ProcessingStatus.COMPLETED).build();
        List<UUID> ids = List.of(media.getId(), m2.getId());
        when(mediaRepository.findAllById(ids)).thenReturn(List.of(media, m2));

        mediaService.deleteMediaBatch(ids, owner.getEmail());

        // 100000 - (10000 + 5000) = 85000
        verify(photographerRepository).save(argThat(p -> p.getStorageUsedBytes() == 85_000L));
    }
}

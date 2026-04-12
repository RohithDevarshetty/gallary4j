package com.photovault.service;

import com.photovault.dto.AlbumDTO;
import com.photovault.dto.CreateAlbumRequest;
import com.photovault.entity.Album;
import com.photovault.entity.Media;
import com.photovault.entity.Photographer;
import com.photovault.repository.AlbumRepository;
import com.photovault.repository.MediaRepository;
import com.photovault.repository.PhotographerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock private AlbumRepository albumRepository;
    @Mock private PhotographerRepository photographerRepository;
    @Mock private MediaRepository mediaRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AlbumService albumService;

    private Photographer photographer;
    private Album album;
    private CreateAlbumRequest createRequest;

    @BeforeEach
    void setUp() {
        photographer = Photographer.builder()
            .id(UUID.randomUUID())
            .email("photo@studio.com")
            .studioName("Test Studio")
            .albumsCount(0)
            .albumsLimit(10)
            .plan("pro")
            .build();

        album = Album.builder()
            .id(UUID.randomUUID())
            .photographer(photographer)
            .title("Wedding Album")
            .slug("wedding-album")
            .viewCount(0)
            .mediaCount(0)
            .isActive(true)
            .isPublic(true)
            .allowDownloads(true)
            .allowSharing(true)
            .enableSelection(false)
            .watermarkPhotos(false)
            .requiresPassword(false)
            .totalSizeBytes(0L)
            .uniqueVisitors(0)
            .totalDownloads(0)
            .createdAt(Instant.now())
            .build();

        createRequest = CreateAlbumRequest.builder()
            .title("Wedding Album")
            .description("Beautiful ceremony")
            .clientName("Jane & John")
            .clientEmail("client@example.com")
            .build();
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Test
    void createAlbum_underLimit_succeeds() {
        when(photographerRepository.findActiveByEmail(photographer.getEmail()))
            .thenReturn(Optional.of(photographer));
        when(albumRepository.countByPhotographerId(photographer.getId())).thenReturn(3L);
        when(albumRepository.save(any(Album.class))).thenReturn(album);

        AlbumDTO result = albumService.createAlbum(photographer.getEmail(), createRequest);

        assertNotNull(result);
        assertEquals(album.getTitle(), result.getTitle());
        verify(albumRepository).save(any(Album.class));
    }

    @Test
    void createAlbum_atLimit_throwsException() {
        when(photographerRepository.findActiveByEmail(photographer.getEmail()))
            .thenReturn(Optional.of(photographer));
        when(albumRepository.countByPhotographerId(photographer.getId()))
            .thenReturn(10L);

        assertThrows(RuntimeException.class, () ->
            albumService.createAlbum(photographer.getEmail(), createRequest)
        );
        verify(albumRepository, never()).save(any(Album.class));
    }

    @Test
    void createAlbum_incrementsPhotographerAlbumCount() {
        photographer.setAlbumsCount(2);
        when(photographerRepository.findActiveByEmail(any())).thenReturn(Optional.of(photographer));
        when(albumRepository.countByPhotographerId(any())).thenReturn(2L);
        when(albumRepository.save(any(Album.class))).thenReturn(album);

        albumService.createAlbum(photographer.getEmail(), createRequest);

        verify(photographerRepository).save(argThat(p -> p.getAlbumsCount() == 3));
    }

    @Test
    void createAlbum_withPassword_setsPasswordHash() {
        createRequest.setPassword("secret123");
        when(photographerRepository.findActiveByEmail(any())).thenReturn(Optional.of(photographer));
        when(albumRepository.countByPhotographerId(any())).thenReturn(0L);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
        when(albumRepository.save(any(Album.class))).thenReturn(album);

        albumService.createAlbum(photographer.getEmail(), createRequest);

        verify(albumRepository).save(argThat(a ->
            Boolean.TRUE.equals(a.getRequiresPassword()) &&
            "hashed-secret".equals(a.getPasswordHash())
        ));
    }

    @Test
    void createAlbum_noPassword_doesNotEncodeAnything() {
        when(photographerRepository.findActiveByEmail(any())).thenReturn(Optional.of(photographer));
        when(albumRepository.countByPhotographerId(any())).thenReturn(0L);
        when(albumRepository.save(any(Album.class))).thenReturn(album);

        albumService.createAlbum(photographer.getEmail(), createRequest);

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void createAlbum_photographerNotFound_throwsException() {
        when(photographerRepository.findActiveByEmail(any())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
            albumService.createAlbum("unknown@example.com", createRequest)
        );
        verify(albumRepository, never()).save(any());
    }

    // ── Get ──────────────────────────────────────────────────────────────────

    @Test
    void getAlbum_existingId_returnsDTO() {
        when(albumRepository.findById(album.getId())).thenReturn(Optional.of(album));

        AlbumDTO result = albumService.getAlbum(album.getId());

        assertNotNull(result);
        assertEquals(album.getId(), result.getId());
        assertEquals(album.getTitle(), result.getTitle());
        assertEquals(album.getSlug(), result.getSlug());
    }

    @Test
    void getAlbum_notFound_throwsException() {
        UUID unknown = UUID.randomUUID();
        when(albumRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> albumService.getAlbum(unknown));
    }

    @Test
    void getAlbumBySlug_existingSlug_returnsDTO() {
        when(albumRepository.findActiveBySlug("wedding-album")).thenReturn(Optional.of(album));

        AlbumDTO result = albumService.getAlbumBySlug("wedding-album");

        assertNotNull(result);
        assertEquals("wedding-album", result.getSlug());
    }

    @Test
    void getPhotographerAlbums_returnsPageOfDTOs() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Album> page = new PageImpl<>(List.of(album));
        when(photographerRepository.findActiveByEmail(photographer.getEmail()))
            .thenReturn(Optional.of(photographer));
        when(albumRepository.findByPhotographerId(photographer.getId(), pageable)).thenReturn(page);

        Page<AlbumDTO> result = albumService.getPhotographerAlbums(photographer.getEmail(), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(album.getTitle(), result.getContent().get(0).getTitle());
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Test
    void updateAlbum_existingAlbum_updatesFields() {
        CreateAlbumRequest updateReq = CreateAlbumRequest.builder()
            .title("Updated Title")
            .clientName("New Client")
            .build();
        when(albumRepository.findById(album.getId())).thenReturn(Optional.of(album));
        when(albumRepository.save(any(Album.class))).thenReturn(album);

        AlbumDTO result = albumService.updateAlbum(album.getId(), updateReq);

        assertNotNull(result);
        verify(albumRepository).save(argThat(a -> "Updated Title".equals(a.getTitle())));
    }

    @Test
    void updateAlbum_notFound_throwsException() {
        UUID unknown = UUID.randomUUID();
        when(albumRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
            albumService.updateAlbum(unknown, createRequest)
        );
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Test
    void deleteAlbum_ownerEmail_softDeletes() {
        photographer.setAlbumsCount(3);
        when(albumRepository.findById(album.getId())).thenReturn(Optional.of(album));
        when(mediaRepository.findAllByAlbumId(album.getId())).thenReturn(List.of());

        albumService.deleteAlbum(album.getId(), photographer.getEmail());

        verify(albumRepository).save(argThat(a -> a.getDeletedAt() != null));
    }

    @Test
    void deleteAlbum_nonOwnerEmail_throwsAccessDeniedException() {
        when(albumRepository.findById(album.getId())).thenReturn(Optional.of(album));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
            () -> albumService.deleteAlbum(album.getId(), "other@studio.com"));

        verify(albumRepository, never()).save(any());
    }

    @Test
    void deleteAlbum_decrementsPhotographerAlbumCount() {
        photographer.setAlbumsCount(3);
        when(albumRepository.findById(album.getId())).thenReturn(Optional.of(album));
        when(mediaRepository.findAllByAlbumId(album.getId())).thenReturn(List.of());

        albumService.deleteAlbum(album.getId(), photographer.getEmail());

        verify(photographerRepository).save(argThat(p -> p.getAlbumsCount() == 2));
    }

    @Test
    void deleteAlbum_countAtZero_clampsToZeroNotNegative() {
        photographer.setAlbumsCount(0);
        when(albumRepository.findById(album.getId())).thenReturn(Optional.of(album));
        when(mediaRepository.findAllByAlbumId(album.getId())).thenReturn(List.of());

        albumService.deleteAlbum(album.getId(), photographer.getEmail());

        verify(photographerRepository).save(argThat(p -> p.getAlbumsCount() == 0));
    }

    @Test
    void deleteAlbum_notFound_throwsException() {
        UUID unknown = UUID.randomUUID();
        when(albumRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
            () -> albumService.deleteAlbum(unknown, photographer.getEmail()));
        verify(albumRepository, never()).save(any());
    }

    @Test
    void deleteAlbum_cascadesMediaSoftDelete() {
        photographer.setAlbumsCount(1);
        Media m1 = Media.builder().id(UUID.randomUUID()).album(album)
            .photographer(photographer).fileSizeBytes(1000L).build();
        Media m2 = Media.builder().id(UUID.randomUUID()).album(album)
            .photographer(photographer).fileSizeBytes(2000L).build();
        when(albumRepository.findById(album.getId())).thenReturn(Optional.of(album));
        when(mediaRepository.findAllByAlbumId(album.getId())).thenReturn(List.of(m1, m2));

        albumService.deleteAlbum(album.getId(), photographer.getEmail());

        verify(mediaRepository).saveAll(argThat(media -> {
            List<Media> list = (List<Media>) media;
            return list.size() == 2 && list.stream().allMatch(m -> m.getDeletedAt() != null);
        }));
    }

    @Test
    void deleteAlbum_reclaimsAllMediaStorageBytesFromPhotographer() {
        photographer.setAlbumsCount(2);
        photographer.setStorageUsedBytes(10_000L);
        album.setTotalSizeBytes(3000L);
        Media m1 = Media.builder().id(UUID.randomUUID()).album(album)
            .photographer(photographer).fileSizeBytes(1000L).build();
        Media m2 = Media.builder().id(UUID.randomUUID()).album(album)
            .photographer(photographer).fileSizeBytes(2000L).build();
        when(albumRepository.findById(album.getId())).thenReturn(Optional.of(album));
        when(mediaRepository.findAllByAlbumId(album.getId())).thenReturn(List.of(m1, m2));

        albumService.deleteAlbum(album.getId(), photographer.getEmail());

        // 10000 - (1000 + 2000) = 7000
        verify(photographerRepository).save(argThat(p -> p.getStorageUsedBytes() == 7_000L));
    }

    @Test
    void deleteAlbum_noMediaInAlbum_storageUnchanged() {
        photographer.setAlbumsCount(1);
        photographer.setStorageUsedBytes(5_000L);
        when(albumRepository.findById(album.getId())).thenReturn(Optional.of(album));
        when(mediaRepository.findAllByAlbumId(album.getId())).thenReturn(List.of());

        albumService.deleteAlbum(album.getId(), photographer.getEmail());

        verify(photographerRepository).save(argThat(p -> p.getStorageUsedBytes() == 5_000L));
    }

    // ── View count ───────────────────────────────────────────────────────────

    @Test
    void incrementViewCount_incrementsCountAndSetsTimestamp() {
        album.setViewCount(5);
        when(albumRepository.findById(album.getId())).thenReturn(Optional.of(album));

        albumService.incrementViewCount(album.getId());

        verify(albumRepository).save(argThat(a ->
            a.getViewCount() == 6 && a.getLastViewedAt() != null
        ));
    }
}

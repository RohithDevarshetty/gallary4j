package com.photovault.service;

import com.photovault.dto.AlbumDTO;
import com.photovault.dto.CreateAlbumRequest;
import com.photovault.entity.Album;
import com.photovault.entity.Photographer;
import com.photovault.repository.AlbumRepository;
import com.photovault.repository.PhotographerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private PhotographerRepository photographerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AlbumService albumService;

    private Photographer photographer;
    private Album album;
    private CreateAlbumRequest createRequest;

    @BeforeEach
    void setUp() {
        photographer = Photographer.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .studioName("Test Studio")
            .passwordHash("hashedpassword")
            .albumsLimit(10)
            .albumsCount(0)
            .build();

        album = Album.builder()
            .id(UUID.randomUUID())
            .photographer(photographer)
            .title("Test Album")
            .description("Test Description")
            .slug("test-album-abc123")
            .isActive(true)
            .isPublic(true)
            .allowDownloads(true)
            .mediaCount(0)
            .build();

        createRequest = CreateAlbumRequest.builder()
            .title("Test Album")
            .description("Test Description")
            .clientName("Client Name")
            .clientEmail("client@example.com")
            .build();
    }

    @Test
    void createAlbum_Success() {
        // Arrange
        when(photographerRepository.findActiveByEmail(photographer.getEmail()))
            .thenReturn(Optional.of(photographer));
        when(albumRepository.countByPhotographerId(photographer.getId()))
            .thenReturn(0L);
        when(albumRepository.save(any(Album.class)))
            .thenReturn(album);

        // Act
        AlbumDTO result = albumService.createAlbum(photographer.getEmail(), createRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Test Album", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        verify(albumRepository).save(any(Album.class));
        verify(photographerRepository).save(any(Photographer.class));
    }

    @Test
    void createAlbum_ExceedsLimit_ThrowsException() {
        // Arrange
        when(photographerRepository.findActiveByEmail(photographer.getEmail()))
            .thenReturn(Optional.of(photographer));
        when(albumRepository.countByPhotographerId(photographer.getId()))
            .thenReturn(10L);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            albumService.createAlbum(photographer.getEmail(), createRequest)
        );
        verify(albumRepository, never()).save(any(Album.class));
    }

    @Test
    void getAlbum_Success() {
        // Arrange
        when(albumRepository.findById(album.getId()))
            .thenReturn(Optional.of(album));

        // Act
        AlbumDTO result = albumService.getAlbum(album.getId());

        // Assert
        assertNotNull(result);
        assertEquals(album.getTitle(), result.getTitle());
        assertEquals(album.getId(), result.getId());
    }

    @Test
    void getAlbumBySlug_Success() {
        // Arrange
        when(albumRepository.findActiveBySlug(album.getSlug()))
            .thenReturn(Optional.of(album));

        // Act
        AlbumDTO result = albumService.getAlbumBySlug(album.getSlug());

        // Assert
        assertNotNull(result);
        assertEquals(album.getSlug(), result.getSlug());
    }

    @Test
    void deleteAlbum_Success() {
        // Arrange
        when(albumRepository.findById(album.getId()))
            .thenReturn(Optional.of(album));

        // Act
        albumService.deleteAlbum(album.getId());

        // Assert
        verify(albumRepository).save(any(Album.class));
        verify(photographerRepository).save(any(Photographer.class));
    }

    @Test
    void incrementViewCount_Success() {
        // Arrange
        when(albumRepository.findById(album.getId()))
            .thenReturn(Optional.of(album));

        // Act
        albumService.incrementViewCount(album.getId());

        // Assert
        verify(albumRepository).save(argThat(savedAlbum ->
            savedAlbum.getViewCount() == 1
        ));
    }
}

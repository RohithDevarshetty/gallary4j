package com.photovault.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for StorageService local-mode behaviour.
 * R2StorageService is optional (@ConditionalOnProperty storage.type=r2)
 * so it may be null in local mode — StorageService must handle that gracefully.
 */
@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private R2StorageService r2StorageService;

    @InjectMocks
    private StorageService storageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(storageService, "storageType", "local");
        ReflectionTestUtils.setField(storageService, "localStoragePath", tempDir.toString());
        ReflectionTestUtils.setField(storageService, "cdnUrl", "http://localhost:8080/media");
    }

    // ── uploadFile ───────────────────────────────────────────────────────────

    @Test
    void uploadFile_localMode_writesToDiskAndReturnsCdnUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "photo", "test.jpg", "image/jpeg", "fake-image-data".getBytes()
        );

        String url = storageService.uploadFile(file, "album-1", "originals");

        assertNotNull(url);
        assertTrue(url.startsWith("http://localhost:8080/media/"));
        assertTrue(url.contains("album-1"));
        assertTrue(url.contains("originals"));
        verifyNoInteractions(r2StorageService);
    }

    @Test
    void uploadFile_localMode_fileExistsOnDisk() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "photo", "test.jpg", "image/jpeg", "fake-image-data".getBytes()
        );

        storageService.uploadFile(file, "album-1", "originals");

        // At least one file must have been written to tempDir
        long filesWritten = java.nio.file.Files.walk(tempDir)
            .filter(java.nio.file.Files::isRegularFile)
            .count();
        assertTrue(filesWritten > 0);
    }

    @Test
    void uploadFile_unknownStorageType_throwsException() {
        ReflectionTestUtils.setField(storageService, "storageType", "gcs");
        MockMultipartFile file = new MockMultipartFile(
            "photo", "test.jpg", "image/jpeg", "data".getBytes()
        );

        assertThrows(UnsupportedOperationException.class, () ->
            storageService.uploadFile(file, "album-1", "originals")
        );
    }

    // ── uploadBytes ──────────────────────────────────────────────────────────

    @Test
    void uploadBytes_localMode_writesToDiskAndReturnsCdnUrl() throws IOException {
        byte[] data = "processed-thumbnail".getBytes();

        String url = storageService.uploadBytes(data, "album-1", "thumbnails", "thumb.webp", "image/webp");

        assertNotNull(url);
        assertTrue(url.startsWith("http://localhost:8080/media/"));
        verifyNoInteractions(r2StorageService);
    }

    // ── downloadFile ─────────────────────────────────────────────────────────

    @Test
    void downloadFile_localMode_returnsFileContents() throws IOException {
        byte[] original = "hello-photo".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "photo", "hello.jpg", "image/jpeg", original
        );

        String url = storageService.uploadFile(file, "album-x", "originals");
        byte[] downloaded = storageService.downloadFile(url);

        assertArrayEquals(original, downloaded);
    }

    @Test
    void downloadFile_missingFile_throwsException() {
        String fakeUrl = "http://localhost:8080/media/nonexistent/path/photo.jpg";

        assertThrows(IOException.class, () ->
            storageService.downloadFile(fakeUrl)
        );
    }

    // ── deleteFile ───────────────────────────────────────────────────────────

    @Test
    void deleteFile_localMode_removesFileFromDisk() throws IOException {
        byte[] data = "to-be-deleted".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "photo", "delete-me.jpg", "image/jpeg", data
        );

        String url = storageService.uploadFile(file, "album-del", "originals");

        // File exists
        long before = java.nio.file.Files.walk(tempDir)
            .filter(java.nio.file.Files::isRegularFile).count();
        assertTrue(before > 0);

        storageService.deleteFile(url);

        // File is gone
        long after = java.nio.file.Files.walk(tempDir)
            .filter(java.nio.file.Files::isRegularFile).count();
        assertEquals(before - 1, after);
    }

    @Test
    void deleteFile_nonExistentFile_doesNotThrow() {
        String fakeUrl = "http://localhost:8080/media/ghost/path/photo.jpg";

        assertDoesNotThrow(() -> storageService.deleteFile(fakeUrl));
    }

    // ── r2 routing ───────────────────────────────────────────────────────────

    @Test
    void uploadFile_r2Mode_delegatesToR2Service() throws IOException {
        ReflectionTestUtils.setField(storageService, "storageType", "r2");
        MockMultipartFile file = new MockMultipartFile(
            "photo", "test.jpg", "image/jpeg", "data".getBytes()
        );
        when(r2StorageService.uploadFile(any(), any(), any())).thenReturn("https://r2.example.com/key");

        String url = storageService.uploadFile(file, "album-1", "originals");

        assertEquals("https://r2.example.com/key", url);
        verify(r2StorageService).uploadFile(file, "album-1", "originals");
    }

    @Test
    void uploadBytes_r2Mode_delegatesToR2Service() throws IOException {
        ReflectionTestUtils.setField(storageService, "storageType", "r2");
        byte[] data = "thumbnail".getBytes();
        when(r2StorageService.uploadBytes(any(), any(), any(), any(), any()))
            .thenReturn("https://r2.example.com/thumb");

        String url = storageService.uploadBytes(data, "album-1", "thumbs", "t.webp", "image/webp");

        assertEquals("https://r2.example.com/thumb", url);
    }
}

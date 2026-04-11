package com.photovault.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageService {

    private final R2StorageService r2StorageService;

    @Value("${storage.type:local}")
    private String storageType;

    @Value("${storage.local.path:./uploads}")
    private String localStoragePath;

    @Value("${storage.cdn.url:http://localhost:8080/media}")
    private String cdnUrl;

    public String uploadFile(MultipartFile file, String albumId, String prefix) throws IOException {
        String filename = generateUniqueFilename(file.getOriginalFilename());
        String key = String.format("%s/%s/%s", albumId, prefix, filename);

        if ("local".equals(storageType)) {
            return uploadToLocal(file, key);
        } else if ("r2".equals(storageType)) {
            return r2StorageService.uploadFile(file, albumId, prefix);
        } else {
            throw new UnsupportedOperationException("Unknown storage type: " + storageType);
        }
    }

    public String uploadBytes(byte[] bytes, String albumId, String prefix, String filename, String contentType) throws IOException {
        String uniqueFilename = generateUniqueFilename(filename);
        String key = String.format("%s/%s/%s", albumId, prefix, uniqueFilename);

        if ("local".equals(storageType)) {
            return uploadBytesToLocal(bytes, key);
        } else if ("r2".equals(storageType)) {
            return r2StorageService.uploadBytes(bytes, albumId, prefix, filename, contentType);
        } else {
            throw new UnsupportedOperationException("Unknown storage type: " + storageType);
        }
    }

    private String uploadToLocal(MultipartFile file, String key) throws IOException {
        Path uploadPath = Paths.get(localStoragePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(key);
        Files.createDirectories(filePath.getParent());

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("File uploaded to local storage: {}", filePath);
        return cdnUrl + "/" + key;
    }

    private String uploadBytesToLocal(byte[] bytes, String key) throws IOException {
        Path uploadPath = Paths.get(localStoragePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(key);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, bytes);

        log.info("Bytes uploaded to local storage: {}", filePath);
        return cdnUrl + "/" + key;
    }

    public byte[] downloadFile(String url) throws IOException {
        if ("local".equals(storageType)) {
            String key = url.replace(cdnUrl + "/", "");
            Path filePath = Paths.get(localStoragePath).resolve(key);
            return Files.readAllBytes(filePath);
        } else if ("r2".equals(storageType)) {
            return r2StorageService.downloadFile(url);
        } else {
            throw new UnsupportedOperationException("Unknown storage type: " + storageType);
        }
    }

    public void deleteFile(String url) throws IOException {
        if ("local".equals(storageType)) {
            String key = url.replace(cdnUrl + "/", "");
            Path filePath = Paths.get(localStoragePath).resolve(key);
            Files.deleteIfExists(filePath);
            log.info("File deleted from local storage: {}", filePath);
        } else if ("r2".equals(storageType)) {
            r2StorageService.deleteFile(url);
        } else {
            throw new UnsupportedOperationException("Unknown storage type: " + storageType);
        }
    }

    private String generateUniqueFilename(String originalFilename) {
        String timestamp = Instant.now().toString().replaceAll("[^0-9]", "");
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        return timestamp + "_" + uuid + extension;
    }

    public String generateS3Key(String albumId, String filename) {
        String sanitizedFilename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");
        return String.format("%s/originals/%s_%s", albumId, Instant.now().toString(), sanitizedFilename);
    }
}

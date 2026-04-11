package com.photovault.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class StorageConfig {

    @Value("${storage.r2.endpoint:}")
    private String r2Endpoint;

    @Value("${storage.r2.access-key:}")
    private String r2AccessKey;

    @Value("${storage.r2.secret-key:}")
    private String r2SecretKey;

    @Value("${storage.s3.region:us-east-1}")
    private String s3Region;

    @Value("${storage.s3.access-key:}")
    private String s3AccessKey;

    @Value("${storage.s3.secret-key:}")
    private String s3SecretKey;

    /**
     * R2 S3 Client (Cloudflare R2)
     */
    @Bean(name = "r2Client")
    public S3Client r2Client() {
        if (r2Endpoint == null || r2Endpoint.isEmpty()) {
            return null; // R2 not configured
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(r2AccessKey, r2SecretKey);

        return S3Client.builder()
            .endpointOverride(URI.create(r2Endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of("auto")) // R2 uses 'auto' region
            .build();
    }

    /**
     * S3 Client (AWS S3 for backups)
     */
    @Bean(name = "s3Client")
    public S3Client s3Client() {
        if (s3AccessKey == null || s3AccessKey.isEmpty()) {
            return null; // S3 not configured
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(s3AccessKey, s3SecretKey);

        return S3Client.builder()
            .region(Region.of(s3Region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();
    }
}

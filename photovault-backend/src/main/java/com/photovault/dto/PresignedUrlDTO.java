package com.photovault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlDTO {
    private String filename;
    private String uploadUrl;
    private Instant expiresAt;
    private String key;  // S3/R2 object key
}

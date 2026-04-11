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
public class PresignedUploadUrl {
    private String uploadUrl;
    private String key;
    private Instant expiresAt;
}

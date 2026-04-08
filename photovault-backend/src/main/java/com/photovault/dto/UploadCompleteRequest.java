package com.photovault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadCompleteRequest {
    private List<String> uploadedFiles;
    private String checksum;  // Optional integrity check
}

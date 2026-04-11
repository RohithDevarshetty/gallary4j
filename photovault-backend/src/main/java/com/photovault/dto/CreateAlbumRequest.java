package com.photovault.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlbumRequest {

    @NotBlank
    private String title;

    private String description;
    private LocalDate eventDate;
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private String password;
    private Boolean allowDownloads;
    private Boolean allowSharing;
    private Boolean enableSelection;
    private Integer maxSelections;
    private Boolean watermarkPhotos;
    private List<String> tags;
    private String category;
}

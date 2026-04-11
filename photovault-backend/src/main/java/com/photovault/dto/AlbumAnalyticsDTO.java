package com.photovault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlbumAnalyticsDTO {
    private UUID albumId;
    private Long totalViews;
    private Long totalDownloads;
    private Long totalShares;
    private Long totalFavorites;
    private Long uniqueVisitors;
    private Long recentViews;  // Last 7 days
    private List<Map<String, Object>> popularMedia;
    private List<Map<String, Object>> dailyStats;
}

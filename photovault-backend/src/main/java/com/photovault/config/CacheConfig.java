package com.photovault.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager();
        mgr.setCacheNames(List.of("albums", "media", "processedMedia"));
        mgr.setCaffeine(Caffeine.newBuilder()
            .maximumSize(5_000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .expireAfterAccess(30, TimeUnit.MINUTES));
        return mgr;
    }
}

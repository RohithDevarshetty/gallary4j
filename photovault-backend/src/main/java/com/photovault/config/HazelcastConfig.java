package com.photovault.config;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class HazelcastConfig {

    @Value("${hazelcast.cluster-name:photovault-cluster}")
    private String clusterName;

    @Bean
    public Config hazelcastConfig() {
        Config config = new Config();
        config.setClusterName(clusterName);

        // Network configuration
        NetworkConfig network = config.getNetworkConfig();
        network.setPort(5701);
        network.setPortAutoIncrement(true);

        JoinConfig join = network.getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig()
            .setEnabled(true)
            .addMember("localhost");

        // Configure distributed maps for caching
        MapConfig albumCacheConfig = new MapConfig("albums");
        albumCacheConfig.setTimeToLiveSeconds(3600) // 1 hour
            .setMaxIdleSeconds(1800) // 30 minutes
            .setEvictionConfig(new EvictionConfig()
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setMaxSizePolicy(MaxSizeConfig.MaxSizePolicy.USED_HEAP_PERCENTAGE)
                .setSize(20));
        config.addMapConfig(albumCacheConfig);

        MapConfig mediaCacheConfig = new MapConfig("media");
        mediaCacheConfig.setTimeToLiveSeconds(7200) // 2 hours
            .setMaxIdleSeconds(3600) // 1 hour
            .setEvictionConfig(new EvictionConfig()
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setMaxSizePolicy(MaxSizeConfig.MaxSizePolicy.USED_HEAP_PERCENTAGE)
                .setSize(30));
        config.addMapConfig(mediaCacheConfig);

        MapConfig processedMediaConfig = new MapConfig("processedMedia");
        processedMediaConfig.setTimeToLiveSeconds(3600)
            .setEvictionConfig(new EvictionConfig()
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setMaxSizePolicy(MaxSizeConfig.MaxSizePolicy.ENTRY_COUNT)
                .setSize(1000));
        config.addMapConfig(processedMediaConfig);

        return config;
    }

    @Bean
    public HazelcastInstance hazelcastInstance(Config config) {
        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean
    public CacheManager cacheManager(HazelcastInstance hazelcastInstance) {
        return new com.hazelcast.spring.cache.HazelcastCacheManager(hazelcastInstance);
    }
}

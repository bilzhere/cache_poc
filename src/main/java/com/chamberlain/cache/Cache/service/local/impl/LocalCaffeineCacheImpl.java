package com.chamberlain.cache.Cache.service.local.impl;

import com.chamberlain.cache.Cache.service.local.LocalCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
@Service
@Slf4j
public class LocalCaffeineCacheImpl implements LocalCache<JsonNode> {
    private static Cache<String, JsonNode> cache = null;
    @Override
    public void put(String key, JsonNode value) {
        log.info("Putting key: {}", key);
        getCaffeineCache().put(key, value);
    }

    @Override
    public JsonNode get(String key) {
        log.info("Getting key: {}", key);
        return getCaffeineCache().getIfPresent(key);
    }

    @Override
    public void invalidate(String key) {
        log.info("Invalidating key: {} and found {}", key, getCaffeineCache().getIfPresent(key) != null);
        getCaffeineCache().invalidate(key);
    }

    private Cache<String, JsonNode> getCaffeineCache() {
        if (cache == null) {
             cache = Caffeine.newBuilder()
                     .maximumSize(100)
                     .expireAfterWrite(10, TimeUnit.MINUTES)
                     .build();
        }
        return cache;
    }
}

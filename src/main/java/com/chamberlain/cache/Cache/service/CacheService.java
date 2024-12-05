package com.chamberlain.cache.Cache.service;

import com.chamberlain.cache.Cache.model.CacheResponse;

public interface CacheService {
    CacheResponse putCache(Integer count, Boolean fullFlush, Long expire);
    CacheResponse getCache(String key, Boolean invalidate);
}

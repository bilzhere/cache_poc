package com.chamberlain.cache.Cache.service.local;

public interface LocalCache<T> {
    void put(String key, T value);
    T get(String key);
    void invalidate(String key);
}

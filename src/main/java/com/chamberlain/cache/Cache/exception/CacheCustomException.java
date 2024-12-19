package com.chamberlain.cache.Cache.exception;

import lombok.Getter;

@Getter
public class CacheCustomException extends RuntimeException{
    private final String key;
    public CacheCustomException(String key, String message) {
        super(message);
        this.key = key;
    }
}

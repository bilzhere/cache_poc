package com.chamberlain.cache.Cache.controller;

import com.chamberlain.cache.Cache.model.CacheResponse;
import com.chamberlain.cache.Cache.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class CacheController {

    private final CacheService cacheService;

    @Autowired
    public CacheController(@Qualifier("CacheRedissonServiceImpl")CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @PostMapping("/v1/cache/{entries}/flush/{flush}/expire/{expire}/create")
    public CacheResponse createCacheEntries(@PathVariable("entries") Integer count,@PathVariable("flush")  Boolean fullFlush, Long expire) {
        return  cacheService.putCache(count, fullFlush, expire);
    }

    @GetMapping("/v1/cache/{key}/{invalidate}")
    public CacheResponse getCacheKey( @PathVariable("key") String key, @PathVariable("invalidate") Boolean invalidate) {
        return cacheService.getCache(key, invalidate);
    }
}

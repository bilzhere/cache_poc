package com.chamberlain.cache.Cache.controller;

import com.chamberlain.cache.Cache.model.CacheResponse;
import com.chamberlain.cache.Cache.model.CacheType;
import com.chamberlain.cache.Cache.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class CacheController {
    private final BeanFactory beanFactory;

    @PostMapping("/v1/cache/{entries}/flush/{flush}/expire/{expire}/create/with/{method}")
    public CacheResponse createCacheEntries(@PathVariable("entries") Integer count,
                                            @PathVariable("flush")  Boolean fullFlush,
                                            @PathVariable("expire") Long expire,
                                            @PathVariable("method") CacheType method){
        return beanFactory.getBean(
                method.equals(CacheType.LETTUCE)?"CacheLettuceServiceImpl":"CacheRedissonServiceImpl",
                CacheService.class).putCache(count, fullFlush, expire);
    }

    @GetMapping("/v1/cache/{key}/{invalidate}/with/{method}")
    public CacheResponse getCacheKey(
            @PathVariable("key") String key,
            @PathVariable("invalidate") Boolean invalidate,
            @PathVariable("method") CacheType method) {
        return beanFactory.getBean(method.equals(CacheType.LETTUCE)?"CacheLettuceServiceImpl":"CacheRedissonServiceImpl",
                CacheService.class).getCache(key, invalidate);
    }
}

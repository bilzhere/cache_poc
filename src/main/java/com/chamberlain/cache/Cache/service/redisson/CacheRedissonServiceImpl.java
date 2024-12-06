package com.chamberlain.cache.Cache.service.redisson;

import com.chamberlain.cache.Cache.config.RedisRedissonConnectionProvider;
import com.chamberlain.cache.Cache.model.CacheResponse;
import com.chamberlain.cache.Cache.service.CacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RClusteredLocalCachedMap;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.UUID;

@Service("CacheRedissonServiceImpl")
@Slf4j
@RequiredArgsConstructor
public class CacheRedissonServiceImpl implements CacheService {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public CacheResponse putCache(Integer count, Boolean fullFlush, Long expire) {
        File jsonFile = new File("src/main/resources/static/sample-client.json").getAbsoluteFile();
        JsonNode sessionObject = null;
        try {
            if (fullFlush)
                RedisRedissonConnectionProvider.getRedissonClient().getKeys().flushdb();
            sessionObject = objectMapper.readTree(jsonFile);
            String session = objectMapper.writeValueAsString(sessionObject);
            RClusteredLocalCachedMap<String, String> localCache = RedisRedissonConnectionProvider.getLocalCache();
            long begin = System.nanoTime();
            for(int i = 0; i < count; i++) {
                String key = UUID.randomUUID().toString();
                localCache.putAsync(key, session);
            }
            long end = System.nanoTime();
            return CacheResponse.builder()
                    .message("Cache entries created successfully")
                    .noOfEntries(count.toString())
                    .timeTake(end - begin + " ns")
                    .status("Success")
                    .build();
        } catch (Exception e) {
            log.error("Error while processing json", e);
            return CacheResponse.builder()
                    .message("Error while processing json, "+e.getMessage())
                    .noOfEntries("0")
                    .timeTake("0 ns")
                    .status("Failure")
                    .build();
        }

    }

    @Override
    public CacheResponse getCache(String key, Boolean invalidate) {
        RClusteredLocalCachedMap<String, String> localCache = RedisRedissonConnectionProvider.getLocalCache();
        boolean isExists = localCache.getCachedMap().containsKey(key);
        long begin = System.nanoTime();
        String node = localCache.get(key);
        long end = System.nanoTime();

        localCache.cachedKeySet().forEach(System.out::println);
        log.info("Cache entry retrieved successfully from "+(isExists?"local":"remote")+" cache");
        if (invalidate)
            localCache.remove(key);
        try {
            return CacheResponse.builder()
                    .message("Cache entry retrieved successfully from "+(isExists?"local":"remote")+" cache")
                    .noOfEntries("1")
                    .timeTake(end - begin + " ns")
                    .status("Success")
                    .data(objectMapper.readTree(node))
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Error while processing json", e);
            return CacheResponse.builder()
                    .message("Error while processing json")
                    .noOfEntries("0")
                    .timeTake(end - begin + " ns")
                    .status("Failure")
                    .build();
        }
    }

}

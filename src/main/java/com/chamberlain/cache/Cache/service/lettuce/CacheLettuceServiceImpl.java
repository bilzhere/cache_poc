package com.chamberlain.cache.Cache.service.lettuce;

import com.chamberlain.cache.Cache.config.RedisConnectionProvider;
import com.chamberlain.cache.Cache.model.CacheResponse;
import com.chamberlain.cache.Cache.service.CacheService;
import com.chamberlain.cache.Cache.service.local.LocalCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CacheLettuceServiceImpl implements CacheService {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final RedisConnectionProvider redisConnectionProvider;
    private final LocalCache<JsonNode> localCache;
    @Override
    public CacheResponse putCache(Integer count, Boolean fullFlush, Long expire) {
        File jsonFile = new File("src/main/resources/static/sample-client.json").getAbsoluteFile();
        JsonNode sessionObject = null;
        try {
            if (fullFlush)
                redisConnectionProvider.getRedisAdvancedClusterCommands().flushdb();
            sessionObject = objectMapper.readTree(jsonFile);
            String session = objectMapper.writeValueAsString(sessionObject);
            long begin = System.nanoTime();
            for(int i = 0; i < count; i++) {
                String key = UUID.randomUUID().toString();
                if (expire.compareTo(1L)>0){
                    redisConnectionProvider.getRedisAdvancedClusterCommands().setex(key, expire, session);
                }else {
                    redisConnectionProvider.getRedisAdvancedClusterCommands().set(key, session);
                }
                localCache.put(key, sessionObject);
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
        String notifySetting = redisConnectionProvider.getRedisAdvancedClusterCommands().configGet("notify-keyspace-events").get("notify-keyspace-events");
        log.info("notifySetting: {}", notifySetting);
        long begin = System.nanoTime();
        JsonNode node = localCache.get(key);
        if (node != null) {
            long end = System.nanoTime();
            if (invalidate)
                localCache.invalidate(key);
            return CacheResponse.builder()
                    .message("Cache entry retrieved successfully from local cache")
                    .noOfEntries("1")
                    .timeTake(end - begin + " ns")
                    .status("Success")
                    .data(node)
                    .build();
        }
        String value = redisConnectionProvider.getRedisAdvancedClusterCommands().get(key);
        long end = System.nanoTime();
        // put the value in local cache
        localCache.put(key, objectMapper.valueToTree(value));
        if (invalidate)
            redisConnectionProvider.getRedisAdvancedClusterCommands().del(key);
        try {
            return CacheResponse.builder()
                    .message("Cache entry retrieved successfully from remote cache")
                    .noOfEntries("1")
                    .timeTake(end - begin + " ns")
                    .status("Success")
                    .data(objectMapper.readTree(value))
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

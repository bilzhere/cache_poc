package com.chamberlain.cache.Cache.service.lettuce;

import com.chamberlain.cache.Cache.config.RedisLettuceConnectionProvider;
import com.chamberlain.cache.Cache.config.RedisRedissonConnectionProvider;
import com.chamberlain.cache.Cache.model.CacheResponse;
import com.chamberlain.cache.Cache.service.CacheService;
import com.chamberlain.cache.Cache.service.local.LocalCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service("CacheLettuceServiceImpl")
@Slf4j
@RequiredArgsConstructor
public class CacheLettuceServiceImpl implements CacheService {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final RedisLettuceConnectionProvider redisLettuceConnectionProvider;
    private final LocalCache<JsonNode> localCache;
    @Override
    public CacheResponse putCache(Integer count, Boolean fullFlush, Long expire) {
        File jsonFile = new File("src/main/resources/static/sample-client.json").getAbsoluteFile();
        JsonNode sessionObject = null;

        try {
            if (fullFlush)
                redisLettuceConnectionProvider.getRedisAdvancedClusterCommands().flushdb();
            putCustomCommand();
            sessionObject = objectMapper.readTree(jsonFile);
            String session = objectMapper.writeValueAsString(sessionObject);
            long begin = System.nanoTime();
            for(int i = 0; i < count; i++) {
                String key = UUID.randomUUID().toString();
                if (expire.compareTo(1L)>0){
                    redisLettuceConnectionProvider.getRedisAdvancedClusterCommands().setex(key, expire, session);
                }else {
                    redisLettuceConnectionProvider.getRedisAdvancedClusterCommands().set(key, session);
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
        String notifySetting = redisLettuceConnectionProvider.getRedisAdvancedClusterCommands().configGet("notify-keyspace-events").get("notify-keyspace-events");
        log.info("notifySetting: {}", notifySetting);
        updateCustomCommand();
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
        String value = redisLettuceConnectionProvider.getRedisAdvancedClusterCommands().get(key);
        long end = System.nanoTime();
        // put the value in local cache
        localCache.put(key, objectMapper.valueToTree(value));
        if (invalidate)
            redisLettuceConnectionProvider.getRedisAdvancedClusterCommands().del(key);
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
    private void clearCache() {
        redisLettuceConnectionProvider.getRedisAdvancedClusterCommands().flushdb();
    }
    private void putCustomCommand() throws IOException {
        File jsonFile = new File("src/main/resources/static/sample-client.json").getAbsoluteFile();
        JsonNode sessionObject = objectMapper.readTree(jsonFile);
        String session = objectMapper.writeValueAsString(sessionObject);
        redisLettuceConnectionProvider.getRedisAdvancedClusterCommands()
                .dispatch(CommandType.JSON_SET, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey("session").add(".").add(session));
    }
    private void updateCustomCommand(){
        redisLettuceConnectionProvider.getRedisAdvancedClusterCommands()
                .dispatch(CommandType.JSON_SET, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey("session").add("$.alias").add("binayak_das"));
    }
}

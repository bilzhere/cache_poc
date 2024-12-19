package com.chamberlain.cache.Cache.service.lettuce;

import com.chamberlain.cache.Cache.config.RedisLettuceConnectionProvider;
import com.chamberlain.cache.Cache.exception.CacheCustomException;
import com.chamberlain.cache.Cache.model.CacheResponse;
import com.chamberlain.cache.Cache.service.CacheService;
import com.chamberlain.cache.Cache.service.local.LocalCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final String ENTIRE_JSON = ".";

    @Override
    public CacheResponse putCache(Integer count, Boolean fullFlush, Long expire) {
        try {
            if (fullFlush)
                clearCache();
            long begin = System.nanoTime();
            for (int i = 0; i < count; i++) {
                String key = UUID.randomUUID().toString();
                putCustomCommand(key, expire);
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
            throw new CacheCustomException("NEW-ENTRY", "Error while processing json, " + e.getMessage());
        }

    }

    @Override
    public CacheResponse getCache(String key, Boolean invalidate) {
        String notifySetting = redisLettuceConnectionProvider.getRedisAdvancedClusterCommands().configGet("notify-keyspace-events").get("notify-keyspace-events");
        log.info("notifySetting: {}", notifySetting);
        long begin = System.nanoTime();
        JsonNode node = localCache.get(key);

        log.info("BEFORE UPDATE: Particular filed fetched from json: {}", fetchParticularField(key, "$.alias"));
        updateCustomCommand(key, "$.alias", UUID.randomUUID().toString());
        log.info("AFTER UPDATE: Particular filed fetched from json: {}", fetchParticularField(key, "$.alias"));

        if (node != null) {
            long end = System.nanoTime();
            if (invalidate)
                localCache.invalidate(key);
            return CacheResponse.builder()
                    .message("Cache entry retrieved successfully from local cache")
                    .noOfEntries("1")
                    .cacheKey(key)
                    .timeTake(end - begin + " ns")
                    .status("Success")
                    .data(node)
                    .build();
        }
        String value = fetchParticularField(key, ENTIRE_JSON);
        long end = System.nanoTime();
        try {
            if (invalidate)
                redisLettuceConnectionProvider.getRedisAdvancedClusterCommands().del(key);
            localCache.put(key, objectMapper.readTree(value));

            return CacheResponse.builder()
                    .message("Cache entry retrieved successfully from remote cache")
                    .noOfEntries("1")
                    .cacheKey(key)
                    .timeTake(end - begin + " ns")
                    .status("Success")
                    .data(objectMapper.readTree(value))
                    .build();
        } catch (Exception e) {
            log.error("Error while processing json", e);
            throw new CacheCustomException(key, "Error while processing json, " + e.getMessage());
        }
    }

    private void clearCache() {
        redisLettuceConnectionProvider.getRedisAdvancedClusterCommands().flushdb();
    }

    private void putCustomCommand(String key, Long expire) throws IOException {
        File jsonFile = new File("src/main/resources/static/sample-client.json").getAbsoluteFile();
        JsonNode sessionObject = objectMapper.readTree(jsonFile);
        String session = objectMapper.writeValueAsString(sessionObject);
        redisLettuceConnectionProvider.getRedisAdvancedClusterCommands()
                .dispatch(CommandType.JSON_SET, new StatusOutput<>(StringCodec.UTF8),
                        new CommandArgs<>(StringCodec.UTF8).addKey(key).add(ENTIRE_JSON).add(session));

        if (expire.compareTo(1L) > 0) {
            redisLettuceConnectionProvider.getRedisAdvancedClusterCommands().expire(key, expire);
        }
        localCache.put(key, sessionObject);
    }

    private void updateCustomCommand(String key, String path, String value) {
        redisLettuceConnectionProvider.getRedisAdvancedClusterCommands().dispatch(CommandType.JSON_SET, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8)
                        .addKey(key)
                        .add(path)
                        .add("\"" + value + "\""));
    }

    private String fetchParticularField(String key, String path) {
        return redisLettuceConnectionProvider.getRedisAdvancedClusterCommands().dispatch(CommandType.JSON_GET, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8)
                        .addKey(key)
                        .add(path));
    }
}

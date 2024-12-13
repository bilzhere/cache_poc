package com.chamberlain.cache.Cache.service.redisson;

import com.chamberlain.cache.Cache.config.RedisRedissonConnectionProvider;
import com.chamberlain.cache.Cache.model.CacheResponse;
import com.chamberlain.cache.Cache.service.CacheService;
import org.redisson.api.RScript;
import org.redisson.codec.JsonJacksonCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RClusteredLocalCachedMap;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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
            setCustomCommand();
            sessionObject = objectMapper.readTree(jsonFile);
            String session = objectMapper.writeValueAsString(sessionObject);
            RClusteredLocalCachedMap<String, JsonNode> localCache = RedisRedissonConnectionProvider.getLocalCache();
            long begin = System.nanoTime();
            for(int i = 0; i < count; i++) {
                String key = UUID.randomUUID().toString();
                localCache.putAsync(key, sessionObject);
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
        RClusteredLocalCachedMap<String, JsonNode> localCache = RedisRedissonConnectionProvider.getLocalCache();
        boolean isExists = localCache.getCachedMap().containsKey(key);
        updateCustomCommand();
        long begin = System.nanoTime();
        JsonNode node = localCache.get(key);
        long end = System.nanoTime();
        localCache.cachedKeySet().forEach(System.out::println);
        log.info("Cache entry retrieved successfully from "+(isExists?"local":"remote")+" cache");
        if (invalidate)
            localCache.remove(key);
        return CacheResponse.builder()
                .message("Cache entry retrieved successfully from "+(isExists?"local":"remote")+" cache")
                .noOfEntries("1")
                .timeTake(end - begin + " ns")
                .status("Success")
                .data(node)
                .build();
    }

    private void clearCache() {
        RedisRedissonConnectionProvider.getLocalCache().clear();
    }
    private void clearCache(String key) {
        RedisRedissonConnectionProvider.getLocalCache().remove(key);
    }
    private void setCustomCommand() throws IOException {
        File jsonFile = new File("src/main/resources/static/sample-client.json").getAbsoluteFile();
        JsonNode sessionObject = objectMapper.readTree(jsonFile);
        String session = objectMapper.writeValueAsString(sessionObject);

        String setScript = "return redis.call('JSON.SET', KEYS[1], ARGV[1], ARGV[2])";
        RedisRedissonConnectionProvider.getRedissonClient().getScript()
                .eval(RScript.Mode.READ_WRITE, setScript, RScript.ReturnType.VALUE,
                        Collections.singletonList("session"), "$", session);
    }

    private void updateCustomCommand() {
        String updateScript = "return redis.call('JSON.SET', KEYS[1], ARGV[1], ARGV[2])";
        RedisRedissonConnectionProvider.getRedissonClient().getScript().eval(RScript.Mode.READ_WRITE, updateScript, RScript.ReturnType.VALUE,
                Collections.singletonList("session"), "$.value", "binayak camera");
    }

}

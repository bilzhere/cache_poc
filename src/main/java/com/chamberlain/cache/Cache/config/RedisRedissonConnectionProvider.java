package com.chamberlain.cache.Cache.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.redisson.Redisson;
import org.redisson.api.ClusteredLocalCachedMapOptions;
import org.redisson.api.RClusteredLocalCachedMap;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.chamberlain.cache.Cache.config.CacheConstants.REDIS_HOST;
import static com.chamberlain.cache.Cache.config.CacheConstants.REDIS_PORTS;
import static org.redisson.api.ClusteredLocalCachedMapOptions.CacheProvider.REDISSON;
import static org.redisson.api.ClusteredLocalCachedMapOptions.StoreMode.LOCALCACHE_REDIS;

@Component
public class RedisRedissonConnectionProvider {
    private static RedissonClient redissonClient;
    public static RedissonClient getRedissonClient() {
        if (redissonClient == null) {
            List<String> nodeAddresses = REDIS_PORTS.stream().map(s -> "redis://" + REDIS_HOST + ":" + s).toList();
            Config config = new Config();
            config.useClusterServers()
                    .addNodeAddress("redis://172.22.204.10:20001","redis://172.22.204.10:20002")
                    .setNodeAddresses(nodeAddresses);
            config.setRegistrationKey("Tcf1rglNmx1OOj/Kq9cS8JngUM8bBOu0GqyI9F35X1wBqhVjbRwbUVtbqVrOYAOcc+k05dGjIP2YezW8ipVrUJJcQEFBDpPcr9y/iML8TF809YJX5+otyqffis803RZEFXIW0QO7gHs3QnTtGk9C2SP6IQ4U5eNAWa4s788zCVI=");
            redissonClient = Redisson.create(config);
        }
        return redissonClient;
    }

    public static ClusteredLocalCachedMapOptions<String, JsonNode> getLocalCacheConfiguration() {
        return ClusteredLocalCachedMapOptions.<String, JsonNode>defaults()
                .syncStrategy(ClusteredLocalCachedMapOptions.SyncStrategy.INVALIDATE)
                .reconnectionStrategy(ClusteredLocalCachedMapOptions.ReconnectionStrategy.LOAD)
                .evictionPolicy(ClusteredLocalCachedMapOptions.EvictionPolicy.LFU)
                .maxIdle(10, TimeUnit.MINUTES)
                .timeToLive(100, TimeUnit.MINUTES)
                .cacheSize(1000)
                .storeMode(LOCALCACHE_REDIS)
                .storeCacheMiss(true)
                .cacheProvider(REDISSON);
    }

    public static RClusteredLocalCachedMap<String, JsonNode> getLocalCache(){
        return getRedissonClient().getClusteredLocalCachedMap("localCache", new JsonJacksonCodec(), getLocalCacheConfiguration());
    }

}

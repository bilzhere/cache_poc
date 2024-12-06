package com.chamberlain.cache.Cache.config;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.chamberlain.cache.Cache.config.CacheConstants.REDIS_HOST;
import static com.chamberlain.cache.Cache.config.CacheConstants.REDIS_PORTS;

@Component
public class RedisLettuceConnectionProvider {
    private RedisAdvancedClusterCommands<String, String> redisAdvancedClusterCommands;
    private StatefulRedisClusterConnection<String, String> connection;
    public StatefulRedisClusterPubSubConnection<String, String> getRedisClusterPubSubConnection() {
        return getRedisClusterClient(getRedisURI()).connectPubSub();
    }
    public StatefulRedisClusterPubSubConnection<String, String> getRedisClusterPubSubConnection(RedisURI redisURI) {
        return getRedisClusterClient(Collections.singletonList(redisURI)).connectPubSub();
    }
    public RedisAdvancedClusterCommands<String, String> getRedisAdvancedClusterCommands() {
        if (redisAdvancedClusterCommands == null) {
            redisAdvancedClusterCommands = getRedisAdvancedClusterCommands(getConnection());
            //redisAdvancedClusterCommands.configGet("notify-keyspace-events", "KA");
        }
        return redisAdvancedClusterCommands;
    }
    public StatefulRedisClusterConnection<String, String> getConnection() {
        if (connection == null) {
            connection = getRedisClusterConnection(getRedisClusterClient(getRedisURI()));
        }
        return connection;
    }

    public List<RedisURI> getRedisURI() {
        return REDIS_PORTS.stream().map(port -> RedisURI.create(REDIS_HOST, Integer.parseInt(port))).toList();
    }
    private RedisClusterClient getRedisClusterClient(List<RedisURI> redisURIs) {
        return RedisClusterClient.create(redisURIs);
    }
    private RedisAdvancedClusterCommands<String, String> getRedisAdvancedClusterCommands(StatefulRedisClusterConnection<String, String> connection) {
        return connection.sync();
    }

    private static StatefulRedisClusterConnection<String, String> getRedisClusterConnection(RedisClusterClient redisClusterClient) {
        return redisClusterClient.connect();
    }

}

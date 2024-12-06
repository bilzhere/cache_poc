package com.chamberlain.cache.Cache.config;

import com.chamberlain.cache.Cache.service.local.LocalCache;
import com.fasterxml.jackson.databind.JsonNode;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubListener;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.cluster.pubsub.api.sync.RedisClusterPubSubCommands;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PubSubListenerForLocalCache {
    private static final String DEL_CHANNEL_NAME = "__keyevent@0__:del";
    private static final String EXPIRED_CHANNEL_NAME = "__keyevent@0__:expired";
    private static final String SET_CHANNEL_NAME = "__keyevent@0__:set";
    private final LocalCache<JsonNode> localCache;
    private final RedisLettuceConnectionProvider redisLettuceConnectionProvider;
    @PostConstruct
    public void init() {
        log.info("Initializing PubSubListenerForLocalCache listener");
        redisLettuceConnectionProvider.getRedisURI().forEach(redisURI -> {
            log.info("Redis URI: {}", redisURI);
            StatefulRedisClusterPubSubConnection<String, String> connection = redisLettuceConnectionProvider.getRedisClusterPubSubConnection(redisURI);
            RedisClusterPubSubCommands<String, String> pubSubCommands= connection.sync();
            connection.addListener(new RedisClusterPubSubListener<>() {
                @Override
                public void message(RedisClusterNode node, String channel, String message) {
                    log.info("Received message: {} on channel: {} from node {}", message, channel, node.getUri().toString());
                    if (DEL_CHANNEL_NAME.equals(channel) || EXPIRED_CHANNEL_NAME.equals(channel)) {
                        localCache.invalidate(message);
                    }
                }

                @Override
                public void message(RedisClusterNode node, String pattern, String channel, String message) {
                    log.info("Received message: {} on channel: {} with pattern: {}", message, channel, pattern);
                }

                @Override
                public void subscribed(RedisClusterNode node, String channel, long count) {
                    log.info("Subscribed to channel: {} and count {} on node {}", channel, count, node.getUri().toString());
                }

                @Override
                public void psubscribed(RedisClusterNode node, String pattern, long count) {
                    log.info("Subscribed to pattern: {}", pattern);
                }

                @Override
                public void unsubscribed(RedisClusterNode node, String channel, long count) {
                    log.info("Unsubscribed from channel: {}", channel);
                }

                @Override
                public void punsubscribed(RedisClusterNode node, String pattern, long count) {
                    log.info("Unsubscribed from pattern: {}", pattern);
                }
            });
            pubSubCommands.subscribe(DEL_CHANNEL_NAME, SET_CHANNEL_NAME, EXPIRED_CHANNEL_NAME);
            connection.setNodeMessagePropagation(true);
        });
        log.info("PubSubListenerForLocalCache listener initialized");
    }
}

package com.ktb.chatapp.websocket.socketio;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.TypedJsonJacksonCodec;

/**
 * Redis-backed implementation of ChatDataStore for multi-node deployments.
 * Uses Redisson buckets with JSON codec to share user/room state across nodes.
 */
@RequiredArgsConstructor
public class RedisChatDataStore implements ChatDataStore {

    private final RedissonClient redissonClient;

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        RBucket<T> bucket = redissonClient.getBucket(key, codec(type));
        return Optional.ofNullable(bucket.get());
    }

    @Override
    public void set(String key, Object value) {
        if (value == null) {
            delete(key);
            return;
        }
        @SuppressWarnings("unchecked")
        RBucket<Object> bucket = (RBucket<Object>) redissonClient.getBucket(key, codec(value.getClass()));
        bucket.set(value);
    }

    @Override
    public void delete(String key) {
        redissonClient.getBucket(key).delete();
    }

    @Override
    public int size() {
        // Counts only keys owned by this Redisson instance; used for logging.
        return (int) redissonClient.getKeys().count();
    }

    private Codec codec(Class<?> type) {
        return new TypedJsonJacksonCodec(type);
    }
}

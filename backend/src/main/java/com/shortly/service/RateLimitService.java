package com.shortly.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RateLimitService {
    private final StringRedisTemplate redis;
    private final RedisScript<Long> script;
    private final int capacity;
    private final double refillPerSecond;
    public RateLimitService(StringRedisTemplate redis, RedisScript<Long> tokenBucketScript,
                            @Value("${app.rate-limit.capacity:30}") int capacity,
                            @Value("${app.rate-limit.refill-per-second:1}") double refillPerSecond) {
        this.redis = redis; this.script = tokenBucketScript; this.capacity = capacity; this.refillPerSecond = refillPerSecond;
    }
    public boolean allow(String identity) {
        Long result = redis.execute(script, List.of("rate:" + identity),
            String.valueOf(System.currentTimeMillis() / 1000.0), String.valueOf(capacity), String.valueOf(refillPerSecond));
        return Long.valueOf(1).equals(result);
    }
}

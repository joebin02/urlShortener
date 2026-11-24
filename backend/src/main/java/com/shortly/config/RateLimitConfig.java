package com.shortly.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RateLimitConfig {
    @Bean
    DefaultRedisScript<Long> tokenBucketScript() {
        var script = new DefaultRedisScript<Long>();
        script.setScriptText("""
            local tokens = tonumber(redis.call('HGET', KEYS[1], 'tokens'))
            local last = tonumber(redis.call('HGET', KEYS[1], 'last'))
            local now = tonumber(ARGV[1])
            local capacity = tonumber(ARGV[2])
            local refill = tonumber(ARGV[3])
            if tokens == nil then tokens = capacity end
            if last == nil then last = now end
            tokens = math.min(capacity, tokens + math.max(0, now - last) * refill)
            local allowed = 0
            if tokens >= 1 then tokens = tokens - 1; allowed = 1 end
            redis.call('HSET', KEYS[1], 'tokens', tokens, 'last', now)
            redis.call('EXPIRE', KEYS[1], math.ceil(capacity / refill) + 1)
            return allowed
            """);
        script.setResultType(Long.class);
        return script;
    }
}

package com.example.s2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis configuration for the S2 multi-store scenario.
 * Configures StringRedisTemplate for JSON-based cache operations.
 */
@Configuration
public class RedisConfig {

    /**
     * Creates a StringRedisTemplate for cache operations.
     * Uses String serialization for both keys and values.
     *
     * @param connectionFactory the Redis connection factory
     * @return configured StringRedisTemplate
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.setEnableTransactionSupport(false);
        return template;
    }
}

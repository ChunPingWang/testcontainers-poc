package com.example.s2.service;

import com.example.s2.domain.Customer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Redis cache operations.
 * Implements cache read-through and write-through patterns for Customer entities.
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private static final String CUSTOMER_KEY_PREFIX = "customer:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Gets a customer from cache.
     *
     * @param id the customer ID
     * @return the cached customer if present
     */
    public Optional<Customer> get(UUID id) {
        String key = buildKey(id);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                log.debug("Cache HIT for customer: {}", id);
                return Optional.of(objectMapper.readValue(json, Customer.class));
            }
            log.debug("Cache MISS for customer: {}", id);
            return Optional.empty();
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize customer from cache: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * Puts a customer into cache with default TTL.
     *
     * @param customer the customer to cache
     */
    public void put(Customer customer) {
        put(customer, DEFAULT_TTL);
    }

    /**
     * Puts a customer into cache with specified TTL.
     *
     * @param customer the customer to cache
     * @param ttl      time-to-live duration
     */
    public void put(Customer customer, Duration ttl) {
        String key = buildKey(customer.getId());
        try {
            String json = objectMapper.writeValueAsString(customer);
            redisTemplate.opsForValue().set(key, json, ttl);
            log.debug("Cached customer: {} with TTL: {}", customer.getId(), ttl);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize customer to cache: {}", customer.getId(), e);
        }
    }

    /**
     * Evicts a customer from cache.
     *
     * @param id the customer ID to evict
     */
    public void evict(UUID id) {
        String key = buildKey(id);
        Boolean deleted = redisTemplate.delete(key);
        log.debug("Evicted customer from cache: {}, deleted: {}", id, deleted);
    }

    /**
     * Checks if a customer exists in cache.
     *
     * @param id the customer ID
     * @return true if cached
     */
    public boolean exists(UUID id) {
        String key = buildKey(id);
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Gets the TTL for a cached customer.
     *
     * @param id the customer ID
     * @return the remaining TTL, or empty if not cached
     */
    public Optional<Duration> getTtl(UUID id) {
        String key = buildKey(id);
        Long ttlSeconds = redisTemplate.getExpire(key);
        if (ttlSeconds != null && ttlSeconds > 0) {
            return Optional.of(Duration.ofSeconds(ttlSeconds));
        }
        return Optional.empty();
    }

    /**
     * Clears all customer cache entries.
     * Use with caution - mainly for testing purposes.
     */
    public void clearAll() {
        var keys = redisTemplate.keys(CUSTOMER_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cleared {} customer cache entries", keys.size());
        }
    }

    private String buildKey(UUID id) {
        return CUSTOMER_KEY_PREFIX + id.toString();
    }
}

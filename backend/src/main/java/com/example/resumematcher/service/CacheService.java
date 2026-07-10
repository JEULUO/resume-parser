package com.example.resumematcher.service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheService {
    private static final Duration REDIS_TTL = Duration.ofDays(7);

    private final ConcurrentMap<String, String> memoryCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public CacheService(ObjectMapper objectMapper, ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.objectMapper = objectMapper;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        String value = getRedisValue(key).orElseGet(() -> memoryCache.get(key));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, type));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public void set(String key, Object value) {
        try {
            String payload = objectMapper.writeValueAsString(value);
            memoryCache.put(key, payload);
            redisTemplateProvider.ifAvailable(template -> {
                try {
                    template.opsForValue().set(key, payload, REDIS_TTL);
                } catch (Exception ignored) {
                    // Redis is optional; the in-memory cache keeps local demos reliable.
                }
            });
        } catch (Exception exception) {
            throw new IllegalStateException("缓存写入失败", exception);
        }
    }

    private Optional<String> getRedisValue(String key) {
        try {
            StringRedisTemplate template = redisTemplateProvider.getIfAvailable();
            if (template == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(template.opsForValue().get(key));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }
}

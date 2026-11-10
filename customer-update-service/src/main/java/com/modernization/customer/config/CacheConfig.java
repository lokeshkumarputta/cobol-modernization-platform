package com.modernization.customer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modernization.customer.dto.CustomerDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CUSTOMER_CACHE = "customers";

    @Bean
    RedisCacheManagerBuilderCustomizer customerCacheCustomizer(
            ObjectMapper objectMapper, @Value("${customer.cache.ttl:PT10M}") Duration ttl) {
        Jackson2JsonRedisSerializer<CustomerDto> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, CustomerDto.class);
        return builder -> builder.withCacheConfiguration(CUSTOMER_CACHE,
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(ttl)
                        .disableCachingNullValues()
                        .serializeValuesWith(SerializationPair.fromSerializer(serializer)));
    }
}

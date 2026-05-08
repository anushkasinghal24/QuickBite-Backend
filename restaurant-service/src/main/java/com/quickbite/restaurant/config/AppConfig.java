package com.quickbite.restaurant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * AppConfig — bean definitions used throughout restaurant-service.
 *
 * Redis caching:
 *  - Restaurant listings cached for 10 minutes (high-frequency read)
 *  - Individual restaurant detail cached for 5 minutes
 *  - Cache is invalidated on update/delete via @CacheEvict in service layer
 *
 */
@Configuration
@EnableCaching
public class AppConfig {

    private static final String RESTAURANT_DETAIL_CACHE = "restaurant_detail_v2";
    private static final String RESTAURANT_LIST_CACHE = "restaurant_list_v2";

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
              .setMatchingStrategy(MatchingStrategies.STRICT)
              .setSkipNullEnabled(true);
        return mapper;
    }

    @Bean
    @ConditionalOnProperty(name = "quickbite.redis.enabled", havingValue = "true")
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper redisObjectMapper = new ObjectMapper();
        redisObjectMapper.registerModule(new JavaTimeModule());
        redisObjectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(redisObjectMapper)));

        RedisCacheConfiguration detailConfig = defaultConfig.entryTtl(Duration.ofMinutes(5));
        RedisCacheConfiguration listConfig = defaultConfig.entryTtl(Duration.ofMinutes(10));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(RESTAURANT_DETAIL_CACHE, detailConfig)
                .withCacheConfiguration(RESTAURANT_LIST_CACHE, listConfig)
                .transactionAware()
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "quickbite.redis.enabled", havingValue = "false", matchIfMissing = true)
    public CacheManager inMemoryCacheManager() {
        return new ConcurrentMapCacheManager(RESTAURANT_DETAIL_CACHE, RESTAURANT_LIST_CACHE);
    }
}

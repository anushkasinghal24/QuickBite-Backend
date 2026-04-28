package com.quickbite.restaurant.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.web.client.RestTemplate;

/**
 * AppConfig — bean definitions used throughout restaurant-service.
 *
 * Redis caching:
 *  - Restaurant listings cached for 10 minutes (high-frequency read)
 *  - Individual restaurant detail cached for 5 minutes
 *  - Cache is invalidated on update/delete via @CacheEvict in service layer
 *
 * RestTemplate:
 *  - Used for internal calls to notification-service
 */
@Configuration
@EnableCaching
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
              .setMatchingStrategy(MatchingStrategies.STRICT)
              .setSkipNullEnabled(true);
        return mapper;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("restaurant_detail", "restaurant_list");
    }
}

package com.marketmind.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Alpha Vantage integration and caching.
 */
@Configuration
@EnableCaching
public class AlphaVantageConfiguration {

    /**
     * Create RestTemplate bean for making HTTP requests to Alpha Vantage API.
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

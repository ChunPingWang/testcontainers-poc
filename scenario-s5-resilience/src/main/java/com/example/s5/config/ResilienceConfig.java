package com.example.s5.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for resilience patterns and HTTP client.
 *
 * Resilience4j configuration is primarily in application.yml.
 * This class provides additional beans and programmatic configuration.
 */
@Configuration
public class ResilienceConfig {

    /**
     * Creates a RestTemplate with configurable timeouts.
     * The timeouts are set to allow testing of timeout scenarios.
     *
     * @param builder the RestTemplateBuilder
     * @return the configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }
}

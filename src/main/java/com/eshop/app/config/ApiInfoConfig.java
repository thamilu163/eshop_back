package com.eshop.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(ApiInfoProperties.class)
public class ApiInfoConfig {

    private final ApiInfoProperties props;

    public ApiInfoConfig(ApiInfoProperties props) {
        this.props = props;
    }

    @Bean
    public ApiInfoProperties apiInfoProperties() {
        return props;
    }
}

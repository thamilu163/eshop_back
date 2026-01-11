package com.eshop.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import com.eshop.app.constants.ApiConstants;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping(ApiConstants.API_PREFIX + "/**")
                    .allowedOrigins("http://localhost:3000", "http://localhost:3001")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With", "If-Match", "Cache-Control")
                    .exposedHeaders("Authorization", "Content-Type", "X-Total-Count", "X-Total-Pages", "ETag", "Cache-Control")
                    .allowCredentials(true);
            }
        };
    }
}

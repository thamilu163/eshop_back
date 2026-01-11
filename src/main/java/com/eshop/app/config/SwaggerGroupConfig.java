package com.eshop.app.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.eshop.app.constants.ApiConstants;

@Configuration
public class SwaggerGroupConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .displayName("Public APIs")
            .pathsToMatch(ApiConstants.BASE_PATH + "/public/**", ApiConstants.BASE_PATH + "/products/**", ApiConstants.BASE_PATH + "/categories/**")
            .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
            .group("auth")
            .displayName("Authentication / Keycloak APIs")
            .pathsToMatch(ApiConstants.BASE_PATH + "/auth/**")
            .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("admin")
            .displayName("Admin APIs")
            .pathsToMatch(ApiConstants.BASE_PATH + "/admin/**")
            .build();
    }

    @Bean
    public GroupedOpenApi sellerApi() {
        return GroupedOpenApi.builder()
            .group("seller")
            .displayName("Seller APIs")
            .pathsToMatch(ApiConstants.BASE_PATH + "/seller/**")
            .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
            .group("user")
            .displayName("User APIs")
            .pathsToMatch(ApiConstants.BASE_PATH + "/users/**", ApiConstants.BASE_PATH + "/orders/**", ApiConstants.BASE_PATH + "/cart/**")
            .build();
    }

    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder()
            .group("all")
            .displayName("All APIs")
            .pathsToMatch(ApiConstants.API_PREFIX + "/**")
            .build();
    }

    @Bean
    public GroupedOpenApi allPackages() {
        return GroupedOpenApi.builder()
            .group("all-packages")
            .displayName("All Packages")
            .packagesToScan("com.eshop.app")
            .build();
    }
}

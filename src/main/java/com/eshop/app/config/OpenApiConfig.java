package com.eshop.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class OpenApiConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8080/realms/eshop}")
    private String issuerUri;

    @Value("${server.port:8082}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        log.info("OpenAPI documentation enabled");

        final String securitySchemeName = "oauth2";
        final String authorizationUrl = issuerUri + "/protocol/openid-connect/auth";
        final String tokenUrl = issuerUri + "/protocol/openid-connect/token";

        return new OpenAPI()
            .info(apiInfo())
            .servers(List.of(new Server().url("http://localhost:" + serverPort).description("Development Server")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.OAUTH2)
                        .description("OAuth2 Authentication with Keycloak")
                        .flows(new OAuthFlows()
                            .authorizationCode(new OAuthFlow()
                                .authorizationUrl(authorizationUrl)
                                .tokenUrl(tokenUrl)
                                .scopes(new Scopes()
                                    .addString("openid", "OpenID Connect")
                                    .addString("profile", "User profile")
                                    .addString("email", "User email")
                                )
                            )
                        )
                )
                .addSecuritySchemes("bearer-jwt",
                    new SecurityScheme()
                        .name("bearer-jwt")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter JWT token directly")
                )
            );
    }

    private Info apiInfo() {
        return new Info()
            .title("E-Shop REST API")
            .description("Enterprise E-Commerce Platform API Documentation")
            .version("1.0.0")
            .contact(new Contact().name("API Support").email("support@eshop.com").url("https://eshop.com/support"))
            .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"));
    }
}
package com.eshop.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
@ConditionalOnProperty(name = "app.openapi.enabled", havingValue = "true", matchIfMissing = false)
public class SwaggerSecurityConfig {
   
    @Bean
    public OpenAPI secureOpenAPI() {
        return new OpenAPI()
            .components(new Components()
                .addSecuritySchemes("keycloak",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(new OAuthFlows()
                            .authorizationCode(new OAuthFlow()
                                .authorizationUrl("http://localhost:8080/realms/eshop-dev/protocol/openid-connect/auth")
                                .tokenUrl("http://localhost:8080/realms/eshop-dev/protocol/openid-connect/token")
                                .scopes(new Scopes()
                                    .addString("openid", "OpenID")
                                )
                            )
                        )
                )
            )
            .addSecurityItem(new SecurityRequirement().addList("keycloak"));
    }
}

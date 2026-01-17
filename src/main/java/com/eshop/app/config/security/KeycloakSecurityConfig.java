package com.eshop.app.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * Keycloak OAuth2 Security Configuration - Enterprise Grade
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>OAuth2 Resource Server with JWT validation</li>
 *   <li>Role-based access control (RBAC)</li>
 *   <li>Keycloak role mapping</li>
 *   <li>CORS configuration</li>
 *   <li>Stateless session management</li>
 *   <li>Public endpoints (Swagger, Actuator)</li>
 * </ul>
 * 
 * <h2>Security Requirements:</h2>
 * <ul>
 *   <li>/api/admin/** - ROLE_ADMIN</li>
 *   <li>/api/seller/** - ROLE_SELLER</li>
 *   <li>/api/customer/** - ROLE_CUSTOMER</li>
 *   <li>/swagger-ui/**, /v3/api-docs/** - Public (dev only)</li>
 *   <li>/actuator/health - Public</li>
 * </ul>
 * 
 * @author EShop Team
 * @version 2.0
 * @since 2025-12-15
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    securedEnabled = true,
    jsr250Enabled = true
)
@org.springframework.context.annotation.Profile("keycloak")
@RequiredArgsConstructor
@Slf4j
public class KeycloakSecurityConfig {
    
    private final Environment environment;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8080/realms/eshop}")
    private String issuerUri;

    @Value("${security.keycloak.realm}")
    private String keycloakRealm;
    
    @PostConstruct
    public void init() {
        log.info("╔═══════════════════════════════════════════════════════════╗");
        log.info("║  KEYCLOAK OAUTH2 SECURITY ENABLED                        ║");
        log.info("╠═══════════════════════════════════════════════════════════╣");
        log.info("║  Realm: {}", keycloakRealm);
        log.info("║  Issuer: {}", issuerUri);
        log.info("║  RBAC: Method-level security enabled                     ║");
        log.info("║  Session: Stateless                                      ║");
        log.info("╚═══════════════════════════════════════════════════════════╝");

        boolean isDevProfile = environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev", "test"));
        if ((issuerUri.contains("localhost") || issuerUri.contains(":8081")) && !isDevProfile) {
            log.error("❌ FATAL: Dev Keycloak issuer URI '{}' detected in non-dev profile! Refusing to start.", issuerUri);
            throw new IllegalStateException("Invalid Keycloak configuration: Dev issuer URI in non-dev profile");
        }
    }
    
    /**
     * Security Filter Chain with OAuth2 Resource Server
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Security Filter Chain with OAuth2 Resource Server");
        
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/api/public/**",
                    "/api/auth/**",
                    "/api/v1/auth/**"
                ).permitAll()
                
                // Public read-only product/category endpoints
                .requestMatchers(org.springframework.http.HttpMethod.GET, 
                    "/api/v1/products/**",
                    "/api/v1/categories/**",
                    "/api/v1/brands/**",
                    "/api/v1/shops/**",
                    "/api/v1/stores/**"
                ).permitAll()
                
                // Admin endpoints
                .requestMatchers("/api/admin/**", "/api/v1/admin/**", "/api/v1/dashboard/admin/**")
                    .hasRole("ADMIN")
                
                // Categories write access - Admin only
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/categories/**")
                    .hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/v1/categories/**")
                    .hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/categories/**")
                    .hasRole("ADMIN")
                
                // Seller endpoints
                .requestMatchers("/api/seller/**", "/api/v1/seller/**", "/api/v1/dashboard/seller/**")
                    .hasRole("SELLER")
                
                // Customer endpoints
                .requestMatchers("/api/customer/**", "/api/v1/dashboard/customer/**")
                    .hasRole("CUSTOMER")
                
                // Delivery endpoints
                .requestMatchers("/api/delivery/**", "/api/v1/dashboard/delivery-agent/**")
                    .hasRole("DELIVERY_AGENT")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        
        // Add legacy JWT filter for local auth support (seed scripts, local testing)
        http.addFilterBefore(jwtAuthenticationFilter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    /**
     * Legacy JWT Authentication Filter for local JWT tokens
     * Supports tokens generated by the local auth controller
     */
    @Bean
    public com.eshop.app.security.JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new com.eshop.app.security.JwtAuthenticationFilter();
    }
    
    /**
     * JWT Authentication Converter with Keycloak Role Mapping
     * 
     * <p>Maps Keycloak roles from realm_access.roles to Spring Security authorities</p>
     * <p>Example: "CUSTOMER" → "ROLE_CUSTOMER"</p>
     * 
     * <p>FIXED: Correctly extracts roles from Keycloak's realm_access.roles structure</p>
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            java.util.List<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();
            
            // Extract from realm_access.roles (Keycloak structure)
            java.util.Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                @SuppressWarnings("unchecked")
                java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
                if (roles != null) {
                    roles.forEach(role -> {
                        String authority = "ROLE_" + role;
                        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(authority));
                        log.debug("Mapped Keycloak role '{}' → Spring authority '{}'", role, authority);
                    });
                }
            }
            
            // TEMP: Log extracted roles for debugging
            log.warn("JWT roles resolved = {}", authorities);
            
            return authorities;
        });
        
        log.info("JWT Authentication Converter configured: realm_access.roles → ROLE_* mapping");
        return converter;
    }
    
    /**
     * CORS Configuration for Frontend Integration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:4200"
        ));
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        log.info("CORS configured: localhost:3000, localhost:4200");
        return source;
    }
}

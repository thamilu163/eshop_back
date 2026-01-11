package com.eshop.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.io.IOException;
import java.util.*;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import com.eshop.app.constants.ApiConstants;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@org.springframework.context.annotation.Profile("oauth2")
public class OAuth2SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SecurityConfig.class);
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_KEY = "roles";
    private static final String DEFAULT_ROLE_PREFIX = "default-";


    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                .anyRequest().hasRole("ADMIN")
            )
            .csrf(csrf -> csrf.disable());
        return http.build();
    }


    // Highest priority: static resources, favicon, error, etc.
    @Bean
    @Order(0)
    public SecurityFilterChain staticResourcesChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
                "/favicon.ico",
                "/static/**",
                "/public/**",
                "/error",
                "/css/**",
                "/js/**",
                "/images/**"
            )
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .securityContext(context -> context.disable())
            .sessionManagement(session -> session.disable())
            .requestCache(cache -> cache.disable());
        return http.build();
    }




    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .anonymous(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .frameOptions(frame -> frame.deny())
            )
            .authorizeHttpRequests(this::configureAuthorization)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(this::handleAuthenticationError)
                .accessDeniedHandler((req, res, ex2) -> handleAccessDenied(req, res, ex2))
            );

        return http.build();
    }

    private void configureAuthorization(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
        // Add actuator endpoints if needed
        // auth.requestMatchers("/actuator/**").hasRole("ADMIN");
        auth.requestMatchers("/error", "/", ApiConstants.API_PREFIX).permitAll();
        // Allow Swagger endpoints
        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
        // Allow unauthenticated access to auth endpoints used by frontend
        auth.requestMatchers(ApiConstants.BASE_PATH + "/auth/login", ApiConstants.BASE_PATH + "/auth/register", ApiConstants.BASE_PATH + "/auth/config").permitAll();
        auth.requestMatchers(ApiConstants.BASE_PATH + "/auth/config", ApiConstants.BASE_PATH + "/auth/health", ApiConstants.BASE_PATH + "/auth/logout-url").permitAll();
        // Allow Keycloak authentication endpoints (public)
        auth.requestMatchers(
            ApiConstants.BASE_PATH + "/auth/login",
            ApiConstants.BASE_PATH + "/auth/refresh",
            ApiConstants.BASE_PATH + "/auth/token",
            ApiConstants.BASE_PATH + "/auth/callback",
            ApiConstants.BASE_PATH + "/auth/login-url",
            ApiConstants.BASE_PATH + "/auth/config",
            ApiConstants.BASE_PATH + "/auth/error"
        ).permitAll();
        // Keycloak protected endpoints
        auth.requestMatchers(
            ApiConstants.BASE_PATH + "/auth/userinfo",
            ApiConstants.BASE_PATH + "/auth/introspect",
            ApiConstants.BASE_PATH + "/auth/logout",
            ApiConstants.BASE_PATH + "/auth/me"
        ).authenticated();
        // Backend identity endpoint (JWT validation)
        auth.requestMatchers(ApiConstants.BASE_PATH + "/me").authenticated();
        // Allow public product/category/brand/shop listings
        auth.requestMatchers(HttpMethod.GET, ApiConstants.BASE_PATH + "/products/**").permitAll();
        auth.requestMatchers(HttpMethod.GET, ApiConstants.BASE_PATH + "/categories/**").permitAll();
        auth.requestMatchers(HttpMethod.GET, ApiConstants.BASE_PATH + "/brands/**").permitAll();
        auth.requestMatchers(HttpMethod.GET, ApiConstants.BASE_PATH + "/shops/**").permitAll();
        auth.requestMatchers(ApiConstants.BASE_PATH + "/cart/**").permitAll();
        // CRITICAL: Seller Dashboard - MUST be called to prove seller authentication
        auth.requestMatchers(ApiConstants.BASE_PATH + "/dashboard/seller/**").hasRole("SELLER");
        // Secure admin and seller endpoints
        auth.requestMatchers(ApiConstants.BASE_PATH + "/admin/**").hasRole("ADMIN");
        auth.requestMatchers(HttpMethod.POST, ApiConstants.BASE_PATH + "/users/**").hasRole("ADMIN");
        auth.requestMatchers(HttpMethod.PUT, ApiConstants.BASE_PATH + "/users/*/role").hasRole("ADMIN");
        auth.requestMatchers(HttpMethod.DELETE, ApiConstants.BASE_PATH + "/users/**").hasRole("ADMIN");
        auth.requestMatchers(ApiConstants.BASE_PATH + "/shops/my-shop/**").hasRole("SELLER");
        auth.requestMatchers(HttpMethod.POST, ApiConstants.BASE_PATH + "/products/**").hasAnyRole("SELLER", "ADMIN");
        auth.requestMatchers(HttpMethod.PUT, ApiConstants.BASE_PATH + "/products/**").hasAnyRole("SELLER", "ADMIN");
        auth.requestMatchers(HttpMethod.DELETE, ApiConstants.BASE_PATH + "/products/**").hasAnyRole("SELLER", "ADMIN");
        auth.requestMatchers(ApiConstants.BASE_PATH + "/cart/**", ApiConstants.API_PREFIX + "/auth/cart/**").hasAnyRole("CUSTOMER", "ADMIN");
        auth.requestMatchers(HttpMethod.POST, ApiConstants.BASE_PATH + "/orders/**").hasAnyRole("CUSTOMER", "ADMIN");
        auth.requestMatchers(HttpMethod.GET, ApiConstants.BASE_PATH + "/orders/my-orders/**").hasAnyRole("CUSTOMER", "ADMIN");
        auth.requestMatchers(ApiConstants.BASE_PATH + "/orders/delivery/**").hasAnyRole("DELIVERY_AGENT", "ADMIN");
        auth.requestMatchers(ApiConstants.BASE_PATH + "/home").authenticated();
        auth.anyRequest().authenticated();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Read allowed origins from properties
        String originsProp = System.getProperty("app.cors.allowed-origins", System.getenv("APP_CORS_ALLOWED_ORIGINS"));
        if (originsProp == null || originsProp.isBlank()) {
            originsProp = "http://localhost:3000,http://localhost:4200";
        }
        List<String> allowedOrigins = Arrays.asList(originsProp.split(","));
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "X-CSRF-Token", "X-Correlation-ID"));
        configuration.setExposedHeaders(List.of("Authorization", "X-Correlation-ID", "X-RateLimit-Remaining"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("CORS configured for origins: {}", configuration.getAllowedOrigins());
        return source;
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    /**
     * JWT Granted Authorities Converter - Extracts roles from Keycloak's realm_access claim
     * 
     * <p><strong>FIX:</strong> Keycloak stores roles in nested structure:</p>
     * <pre>
     * {
     *   "realm_access": {
     *     "roles": ["CUSTOMER", "SELLER", "offline_access", "uma_authorization"]
     *   },
     *   "resource_access": {
     *     "eshop-client": {
     *       "roles": ["client-role"]
     *     }
     *   }
     * }
     * </pre>
     * 
     * <p>This converter extracts roles from <strong>realm_access.roles</strong> and optionally 
     * from resource_access for client-specific roles.</p>
     */
    @Bean
    public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            try {
                Collection<GrantedAuthority> authorities = new ArrayList<>();
                
                // Extract realm_access roles (primary source)
                Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
                if (realmAccess != null && realmAccess.containsKey(ROLES_KEY)) {
                    @SuppressWarnings("unchecked")
                    Collection<String> realmRoles = (Collection<String>) realmAccess.get(ROLES_KEY);
                    
                    if (realmRoles != null) {
                        log.debug("🔐 JWT Authentication | user={} | realm_roles={} | subject={}", 
                                 jwt.getClaimAsString("preferred_username"),
                                 realmRoles,
                                 jwt.getSubject());
                        
                        authorities.addAll(
                            realmRoles.stream()
                                .filter(role -> role != null && !role.isBlank())
                                .filter(role -> !role.startsWith(DEFAULT_ROLE_PREFIX))
                                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                                .toList()
                        );
                    }
                }
                
                // Optional: Extract resource_access roles (client-specific roles)
                Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
                if (resourceAccess != null) {
                    for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> resource = (Map<String, Object>) entry.getValue();
                        if (resource != null && resource.containsKey(ROLES_KEY)) {
                            @SuppressWarnings("unchecked")
                            Collection<String> resourceRoles = (Collection<String>) resource.get(ROLES_KEY);
                            if (resourceRoles != null) {
                                authorities.addAll(
                                    resourceRoles.stream()
                                        .filter(role -> role != null && !role.isBlank())
                                        .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                                        .toList()
                                );
                            }
                        }
                    }
                }
                
                if (authorities.isEmpty()) {
                    log.warn("⚠️  No roles found in JWT token for user: {}. Check realm_access.roles in token.", 
                             jwt.getClaimAsString("preferred_username"));
                }
                
                log.debug("✅ Granted authorities: {}", authorities);
                
                return authorities;
                    
            } catch (Exception e) {
                log.error("❌ Failed to extract authorities from JWT: {}", e.getMessage(), e);
                return Collections.emptyList();
            }
        };
    }

    private void handleAuthenticationError(HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException exception) throws IOException {
        log.warn("Authentication failed for request '{}': {}", request.getRequestURI(), exception.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\",\"timestamp\":\"%s\"}", java.time.Instant.now()));
    }

    private void handleAccessDenied(HttpServletRequest request, HttpServletResponse response, org.springframework.security.access.AccessDeniedException exception) throws IOException {
        log.warn("Access denied for request '{}': {}", request.getRequestURI(), exception.getMessage());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\":\"Forbidden\",\"message\":\"Insufficient permissions\",\"timestamp\":\"%s\"}", java.time.Instant.now()));
    }

    // Legacy JwtAuthenticationFilter is intentionally not registered here.
    // Spring Security OAuth2 Resource Server handles incoming Keycloak/OIDC tokens.

}

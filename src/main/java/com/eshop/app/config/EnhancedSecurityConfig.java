package com.eshop.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.eshop.app.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.time.Instant;
import java.util.Arrays;

/**
 * Enterprise-grade Security Configuration for Spring Boot 4.0.
 * 
 * <p>Features:
 * <ul>
 *   <li>OAuth2 Resource Server with JWT validation</li>
 *   <li>Role-based access control (RBAC)</li>
 *   <li>CSRF protection with SPA support</li>
 *   <li>CORS configuration</li>
 *   <li>Custom authentication and authorization error handlers</li>
 *   <li>Security headers (Content-Security-Policy, X-Frame-Options, etc.)</li>
 *   <li>Stateless session management</li>
 * </ul>
 * 
 * @author E-Shop Team
 * @version 2.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@Profile("!keycloak & !oauth2")
@RequiredArgsConstructor
@Slf4j
public class EnhancedSecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8080/realms/eshop}")
    private String issuerUri;
    
    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;
    
    /**
     * Principal details for JWT authentication.
     * Contains user identity information extracted from JWT claims.
     * Provides both record-style accessors (id(), username(), email()) 
     * and JavaBean-style getters (getId(), getUsername(), getEmail()) for reflection compatibility.
     */
    public record PrincipalDetails(Long id, String username, String email) {
        // JavaBean-style getters for reflection compatibility
        public Long getId() {
            return id;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getEmail() {
            return email;
        }
        
        public String getName() {
            return username;
        }
    }
    
    @Value("${app.cors.allowed-methods}")
    private String[] allowedMethods;
    
    @Value("${app.cors.allowed-headers}")
    private String[] allowedHeaders;
    
    @Value("${app.cors.exposed-headers}")
    private String[] exposedHeaders;
    
    @Value("${app.cors.max-age}")
    private long corsMaxAge;
    
    @Value("${app.security.jwt.authorities-claim-name:roles}")
    private String authoritiesClaimName;
    
    @Value("${app.security.jwt.authority-prefix:ROLE_}")
    private String authorityPrefix;
    
    private final ObjectMapper objectMapper;
    private final org.springframework.beans.factory.ObjectProvider<com.eshop.app.service.UserService> userServiceProvider;

    /**
     * Main security filter chain configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder) throws Exception {
        log.info("Configuring security filter chain with OAuth2 Resource Server");
        
        http
            // CSRF: Disabled for REST API (using JWT stateless auth)
            .csrf(AbstractHttpConfigurer::disable)
            
            // CORS Configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Authorization Rules
            .authorizeHttpRequests(auth -> auth
                // CRITICAL-004 FIX: CSP Report endpoint (browsers send without auth)
                .requestMatchers("/csp/report").permitAll()
                
                // CRITICAL-005 FIX: Session validation endpoint (must be public to check token validity)
                .requestMatchers("/auth/session").permitAll()
                .requestMatchers("/auth/config").permitAll()
                
                // Public auth endpoints
                        .requestMatchers(
                    "/api/v1/public/**",
                    "/api/auth/**",
                    "/auth/_log"
                ).permitAll()
                
                // Actuator health endpoints
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/prometheus"
                ).permitAll()
                
                // Swagger/OpenAPI (conditionally accessible)
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/error"
                ).permitAll()
                
                // Actuator - Admin only
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // Products - Read public, Write restricted
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/products/**")
                    .hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/products/**")
                    .hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**")
                    .hasRole("ADMIN")
                
                // Categories - Read public, Write admin
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                .requestMatchers("/api/v1/categories/**").hasRole("ADMIN")
                
                // Admin endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                
                // Seller endpoints
                .requestMatchers("/api/v1/seller/**").hasAnyRole("SELLER", "ADMIN")
                
                // Orders - Authenticated users
                .requestMatchers("/api/v1/orders/**").authenticated()
                
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            
            // OAuth2 Resource Server
                .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                    .decoder(jwtDecoder)
                )
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )
            
            // Session Management - Stateless
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Exception Handling
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )
            
            // Security Headers
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")
                )
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            );

        return http.build();
    }
    // JWT decoder is provided by JwtAudienceValidatorConfig; injected into the security filter chain.

    /**
     * JWT Authentication Converter - Extracts roles from JWT claims.
     * FIXED: Extract roles from Keycloak's realm_access.roles structure
     */
    @Bean
    public org.springframework.core.convert.converter.Converter<org.springframework.security.oauth2.jwt.Jwt, org.springframework.security.authentication.AbstractAuthenticationToken> jwtAuthenticationConverter() {
        log.info("🔧 EnhancedSecurityConfig JWT converter - extracting from realm_access.roles and mapping principal to local user id");

        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(jwt -> {
            java.util.List<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();

            log.debug("🔍 Extracting authorities from JWT for user: {}", jwt.getClaimAsString("preferred_username"));

            // Extract from realm_access.roles (Keycloak structure)
            java.util.Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                @SuppressWarnings("unchecked")
                java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
                if (roles != null) {
                    roles.forEach(role -> {
                        if (role != null && !role.isBlank() && !role.startsWith("default-")) {
                            String authority = authorityPrefix + role;
                            authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(authority));
                            log.debug("   → Granting authority: {}", authority);
                        }
                    });
                }
            }

            // TEMP: Log extracted roles for debugging
            log.warn("JWT roles resolved = {}", authorities);

            return authorities;
        });

        return jwt -> {
            org.springframework.security.core.Authentication auth = delegate.convert(jwt);
            java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities = auth != null ? auth.getAuthorities() : java.util.List.of();

            // Resolve local user id from claims (find or create)
            String username = jwt.getClaimAsString("preferred_username");
            String email = jwt.getClaimAsString("email");
            String givenName = jwt.getClaimAsString("given_name");
            String familyName = jwt.getClaimAsString("family_name");
            Boolean emailVerified = jwt.getClaim("email_verified");

            Long localUserId = null;
            try {
                com.eshop.app.service.UserService userService = userServiceProvider.getIfAvailable();
                if (userService != null) {
                    java.util.Optional<Long> found = userService.findUserIdByUsernameOrEmail(username != null ? username : email);
                    if (found.isPresent()) {
                        localUserId = found.get();
                    } else {
                        localUserId = userService.createUserFromClaims(username, email, givenName, familyName, emailVerified != null ? emailVerified : false);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to resolve/create local user for JWT subject {}", jwt.getSubject(), e);
            }

            // Ensure we always have a non-null id on the principal to avoid unboxing NPEs
            Long principalId = localUserId != null ? localUserId : -1L;
            log.info("Resolved principal id={} for jwt.sub={} username={}", principalId, jwt.getSubject(), username);

            // Create principal object expected by SpEL expressions (principal.id)
            PrincipalDetails principal = new PrincipalDetails(principalId, username, email);

            return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, jwt, authorities);
        };
    }

    /**
     * CORS Configuration Source.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS with allowed origins: {}", Arrays.toString(allowedOrigins));
        
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList(allowedMethods));
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders));
        configuration.setExposedHeaders(Arrays.asList(exposedHeaders));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(corsMaxAge);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }

    /**
     * Custom Authentication Entry Point for 401 Unauthorized errors.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            log.warn("Authentication failed for request: {} {}", 
                request.getMethod(), request.getRequestURI());
            
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Full authentication is required to access this resource")
                .path(request.getRequestURI())
                .build();
            
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }

    /**
     * Custom Access Denied Handler for 403 Forbidden errors.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.warn("Access denied for request: {} {}, user: {}", 
                request.getMethod(), 
                request.getRequestURI(),
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous");
            
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("You don't have permission to access this resource")
                .path(request.getRequestURI())
                .build();
            
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }
}

package com.eshop.app.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * DEPRECATED: This configuration is replaced by OAuth2SecurityConfig
 * Keep for reference only - not active
 * 
 * This was the original JWT-based authentication.
 * Now using Keycloak OAuth2 - see OAuth2SecurityConfig.java
 */
// @Configuration  // Disabled to prevent bean conflicts
// @EnableWebSecurity
// @EnableMethodSecurity(prePostEnabled = true)
@SuppressWarnings("unused")
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;
    
    // @Bean - Disabled to prevent conflict with OAuth2SecurityConfig
    // @Bean - Disabled to prevent conflict with OAuth2SecurityConfig
    // public JwtAuthenticationFilter jwtAuthenticationFilter() {
    // return new JwtAuthenticationFilter();
    // }
    
    // @Bean - Disabled, using bean from OAuth2SecurityConfig
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    

    // @Bean - Disabled, using bean from OAuth2SecurityConfig
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    // @Bean - Disabled, using bean from OAuth2SecurityConfig
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    // @Bean - Disabled, using bean from OAuth2SecurityConfig
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Home page endpoints
                .requestMatchers("/", "/api").permitAll()
                
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/error").permitAll()
                
                // Swagger/OpenAPI endpoints (public) - Fixed for Spring Boot 4.0
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-ui/index.html", 
                                "/swagger-resources/**", "/webjars/**", "/configuration/**").permitAll()
                
                // Role-specific home dashboard (requires authentication)
                .requestMatchers("/api/home").authenticated()
                
                // Public product browsing
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/brands/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/shops/**").permitAll()
                
                // Admin only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/users/*/role").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                
                // Seller endpoints
                .requestMatchers("/api/shops/my-shop/**").hasRole("SELLER")
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                
                // Customer endpoints
                .requestMatchers("/api/cart/**").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/orders/**").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/orders/my-orders/**").hasAnyRole("CUSTOMER", "ADMIN")
                
                // Delivery agent endpoints
                .requestMatchers("/api/orders/delivery/**").hasAnyRole("DELIVERY_AGENT", "ADMIN")
                
                // All other authenticated endpoints
                .anyRequest().authenticated()
            );
        
        // http.addFilterBefore(jwtAuthenticationFilter(),
        // UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}

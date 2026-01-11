package com.eshop.app.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility class for security-related operations
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<Authentication> getCurrentAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    public static Optional<String> getCurrentUserId() {
        return getCurrentAuthentication()
                .filter(auth -> auth.getPrincipal() instanceof Jwt)
                .map(auth -> (Jwt) auth.getPrincipal())
                .map(jwt -> jwt.getClaimAsString("sub"));
    }

    public static Optional<String> getCurrentUsername() {
        return getCurrentAuthentication()
                .filter(auth -> auth.getPrincipal() instanceof Jwt)
                .map(auth -> (Jwt) auth.getPrincipal())
                .map(jwt -> jwt.getClaimAsString("preferred_username"));
    }

    public static boolean hasRole(String role) {
        return getCurrentAuthentication()
                .map(Authentication::getAuthorities)
                .map(authorities -> authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(auth -> auth.equals("ROLE_" + role) || auth.equals(role)))
                .orElse(false);
    }

    public static boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) return false;
        return getCurrentAuthentication()
                .map(Authentication::getAuthorities)
                .map(authorities -> {
                    Collection<String> userAuthorities = authorities.stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());
                    for (String role : roles) {
                        if (userAuthorities.contains("ROLE_" + role) || userAuthorities.contains(role)) return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    public static Optional<Jwt> getCurrentJwt() {
        return getCurrentAuthentication()
                .filter(auth -> auth.getPrincipal() instanceof Jwt)
                .map(auth -> (Jwt) auth.getPrincipal());
    }

}
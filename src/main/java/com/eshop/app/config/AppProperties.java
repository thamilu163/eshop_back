package com.eshop.app.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// This lightweight AppProperties class is intentionally NOT registered as a bean
// to avoid conflicting with the comprehensive AppProperties in
// com.eshop.app.config.properties.AppProperties
@Getter
@Setter
public class AppProperties {

    private final Cors cors = new Cors();

    // Expose the CORS config
    public Cors getCors() { return cors; }

    @Getter
    @Setter
    public static class Cors {

        @NotEmpty(message = "Allowed origins must not be empty")
        private List<@Pattern(regexp = "^(\\*|https?://[\\w.-]+(:\\d+)?)$", message = "Invalid origin format") String> allowedOrigins = Collections.singletonList("http://localhost:3000");

        @NotEmpty(message = "Allowed methods must not be empty")
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

        @NotEmpty(message = "Allowed headers must not be empty")
        private List<String> allowedHeaders = Collections.singletonList("*");

        private boolean allowCredentials = true;

        @Min(value = 0, message = "Max age must be non-negative")
        @Max(value = 86400, message = "Max age cannot exceed 24 hours")
        private int maxAge = 3600;

        // Cached set for fast lookup
        private transient Set<String> allowedOriginsSet = null;

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins == null ? Collections.emptyList() : List.copyOf(allowedOrigins);
            this.allowedOriginsSet = null; // reset cached set
        }

        public boolean isOriginAllowed(String origin) {
            if (allowedOrigins == null || allowedOrigins.isEmpty()) return false;
            if (allowedOrigins.contains("*")) return true;
            if (allowedOriginsSet == null) {
                allowedOriginsSet = new HashSet<>(allowedOrigins.stream().map(String::toLowerCase).collect(Collectors.toSet()));
            }
            return allowedOriginsSet.contains(origin == null ? null : origin.toLowerCase());
        }
    }
}

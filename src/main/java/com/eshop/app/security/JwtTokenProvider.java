package com.eshop.app.security;

import com.eshop.app.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private SecretKey key;
    private MacAlgorithm macAlgorithm;

    @PostConstruct
    public void init() {
        // Use Base64 decoding if the secret is Base64-encoded; fallback to UTF-8 bytes
        // when not
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        // Choose algorithm based on key size per RFC 7518
        if (keyBytes.length * 8 >= 512) {
            this.key = Keys.hmacShaKeyFor(keyBytes);
            this.macAlgorithm = Jwts.SIG.HS512;
        } else if (keyBytes.length * 8 >= 256) {
            this.key = Keys.hmacShaKeyFor(keyBytes);
            this.macAlgorithm = Jwts.SIG.HS256;
        } else {
            // If extremely short secret, pad to 256-bit minimum
            byte[] padded = java.util.Arrays.copyOf(keyBytes, 32);
            this.key = Keys.hmacShaKeyFor(padded);
            this.macAlgorithm = Jwts.SIG.HS256;
        }
    }

    public String generateToken(Authentication authentication) {
        Long id;
        String username;
        String email;
        String role;

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetailsImpl ud) {
            id = ud.getId();
            username = ud.getUsername();
            email = ud.getEmail();
            role = ud.getRole();
        } else if (principal instanceof com.eshop.app.config.EnhancedSecurityConfig.PrincipalDetails pd) {
            id = pd.getId();
            username = pd.getUsername();
            email = pd.getEmail();
            // Extract role from authorities (remove ROLE_ prefix)
            role = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("CUSTOMER");
        } else {
            throw new IllegalArgumentException("Unsupported principal type: " + principal.getClass().getName());
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(Long.toString(id))
                .claim("username", username)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, macAlgorithm)
                .compact();
    }

    public String generateTokenFromUser(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(Long.toString(user.getId()))
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, macAlgorithm)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("username", String.class);
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (SecurityException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty");
        }
        return false;
    }

    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    public Date getIssuedAtFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getIssuedAt();
    }
}
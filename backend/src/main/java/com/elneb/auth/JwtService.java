package com.elneb.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${elneb.jwt.secret}") String secret,
                      @Value("${elneb.jwt.expiration-minutes}") long expirationMinutes) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 256 bits");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String create(User user) {
        return createScoped(user, null);
    }

    public String createScoped(User user, String scope) {
        Instant now = Instant.now();
        var builder = Jwts.builder().subject(user.getId().toString()).claim("role", user.getRole().name())
                .claim("email", user.getEmail()).issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(scope == null ? expirationMinutes * 60 : 300)));
        if (scope != null) builder.claim("scope", scope);
        return builder.signWith(key).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}

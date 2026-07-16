package com.sms.nexus.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    public JwtTokenProvider(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String adminId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("adminId", adminId);
        claims.put("username", username);
        claims.put("role", role);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .claims(claims)
                .subject(adminId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getAdminIdFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public String getUsernameFromToken(String token) {
        return parseToken(token).get("username", String.class);
    }

    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            if (isTokenBlacklisted(token)) {
                return false;
            }
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public void blacklistToken(String token) {
        Claims claims = parseToken(token);
        long remainingTime = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (remainingTime > 0) {
            redisTemplate.opsForValue().set(
                    TOKEN_BLACKLIST_PREFIX + token,
                    "1",
                    remainingTime,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token));
    }
}

package com.designstudio.infrastructure.security;

import com.designstudio.domain.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String JWT_SECRET = "MySuperSecretKeyForDesignStudioApplication2024VeryLongAndSecureKeyForHS512Algorithm";
    private static final int JWT_EXPIRATION = 86400000; // 24 часа

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = JWT_SECRET.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            byte[] padded = new byte[64];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            this.key = Keys.hmacShaKeyFor(padded);
        } else {
            this.key = Keys.hmacShaKeyFor(keyBytes);
        }
        logger.info("JWT ключ инициализирован, длина: {} байт", this.key.getEncoded().length);
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION);
        String email = user.getEmail().trim().toLowerCase();

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            logger.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }
}
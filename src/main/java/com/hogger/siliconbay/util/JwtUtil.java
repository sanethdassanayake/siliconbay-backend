package com.hogger.siliconbay.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET_KEY =
            "MySuperSecretKeyForJWT12345678901234567890";

    private static final long EXPIRATION_TIME =
            7 * 24 * 60 * 60 * 1000L;

    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    public static String generateToken(String email, int userId) {
        return generateToken(email, userId, "USER");
    }

    public static String generateToken(String email, int userId, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role == null ? "USER" : role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(KEY)
                .compact();
    }

    private static Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public static Long getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    public static String getRoleFromToken(String token) {
        String role = parseClaims(token).get("role", String.class);
        return role == null ? "USER" : role;
    }

    public static boolean isTokenExpired(String token) {
        return parseClaims(token)
                .getExpiration()
                .before(new Date());
    }
}

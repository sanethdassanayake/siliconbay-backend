package com.hogger.siliconbay.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

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
                .setSubject(email)
                .claim("userId", userId)
                .claim("role", role == null ? "USER" : role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(KEY)
                .compact();
    }

    private static Claims parseClaims(String token) {
        return Jwts.parser()
            .setSigningKey(KEY)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public static String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public static Long getUserIdFromToken(String token) {
        Object v = parseClaims(token).get("userId");
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRoleFromToken(String token) {
        String role = parseClaims(token).get("role", String.class);
        return role == null ? "USER" : role;
    }

    public static boolean isTokenExpired(String token) {
        Date exp = parseClaims(token).getExpiration();
        return exp == null || exp.before(new Date());
    }
}

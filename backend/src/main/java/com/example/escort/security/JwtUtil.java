package com.example.escort.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * HS256算法签发与校验，密钥走环境变量 JWT_SECRET。
 * 安全要求：未注入密钥、仍为仓库默认值或长度不足32字节时拒绝启动。
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret:}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        if (secret == null || secret.isBlank()
                || secret.contains("please-change")
                || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET 未配置或仍为默认值（长度需≥32字节），拒绝启动");
        }
        // HS256要求密钥≥32字节（256位）
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 签发JWT
     * @param userId       用户ID
     * @param username     用户名
     * @param role         角色
     * @param tokenVersion 用户当前令牌版本号（登出/改密/禁用后递增使旧令牌失效）
     * @return JWT字符串
     */
    public String generateToken(Long userId, String username, String role, int tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("tv", tokenVersion);

        Date now = new Date();
        Date expireTime = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expireTime)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析JWT
     * @param token JWT字符串
     * @return Claims，解析失败返回null
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("JWT解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 校验Token是否有效
     */
    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中获取角色
     */
    public String getUserRole(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        return claims.get("role", String.class);
    }
}

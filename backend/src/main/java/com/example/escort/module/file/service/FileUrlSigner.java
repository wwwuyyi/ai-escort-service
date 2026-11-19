package com.example.escort.module.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * 上传文件短期签名 URL。
 *
 * 目的：不再把长期 JWT 放进图片 URL。前端先携带 Authorization 请求
 * /api/file/signed-url 做对象级授权，成功后拿到 5 分钟内有效的下载 URL；
 * <img> 直接使用该 URL，不需要在 URL 中携带登录 Token。
 *
 * 签名内容：path + uid + expires，使用 JWT_SECRET 派生的 HMAC-SHA256。
 */
@Component
public class FileUrlSigner {

    private final byte[] secret;
    private final long ttlSeconds;

    public FileUrlSigner(@Value("${jwt.secret}") String jwtSecret,
                         @Value("${file.sign-ttl-seconds:300}") long ttlSeconds) {
        this.secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = Math.max(60, Math.min(ttlSeconds, 900));
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public String buildSignedUrl(String path, Long userId) {
        long expires = Instant.now().getEpochSecond() + ttlSeconds;
        String sig = sign(path, userId, expires);
        return "/api/file/download?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8)
                + "&uid=" + userId
                + "&expires=" + expires
                + "&sig=" + sig;
    }

    public boolean verify(String path, Long userId, long expires, String sig) {
        if (path == null || userId == null || sig == null || sig.isBlank()) return false;
        long now = Instant.now().getEpochSecond();
        // 过期或超出允许的最长有效期（防止签名被改成长期 URL）
        if (expires <= now || expires > now + ttlSeconds + 60) return false;
        String expected = sign(path, userId, expires);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                sig.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String path, Long userId, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            String payload = path + "\n" + userId + "\n" + expires;
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("文件签名失败", e);
        }
    }
}

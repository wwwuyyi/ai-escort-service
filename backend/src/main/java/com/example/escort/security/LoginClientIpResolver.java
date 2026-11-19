package com.example.escort.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 登录限流使用的客户端 IP 解析。
 *
 * 安全策略：默认只取 request.getRemoteAddr()，不信任任何 X-Forwarded-For。
 * 仅当远端地址属于 security.login.trusted-proxies 配置的受信代理时，
 * 才解析 X-Forwarded-For，并从右向左跳过受信代理，取第一个非受信地址，
 * 避免客户端伪造 XFF 绕过登录限流。
 */
@Component
public class LoginClientIpResolver {

    private final Set<String> trustedProxies;

    public LoginClientIpResolver(@Value("${security.login.trusted-proxies:}") String trustedProxies) {
        Set<String> set = new HashSet<>();
        if (trustedProxies != null) {
            Arrays.stream(trustedProxies.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(this::normalize)
                    .forEach(set::add);
        }
        this.trustedProxies = set;
    }

    public String resolve(HttpServletRequest request) {
        String remote = normalize(request.getRemoteAddr());
        if (!trustedProxies.contains(remote)) {
            return remote;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank()) {
            return remote;
        }
        String[] parts = xff.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String ip = normalize(parts[i]);
            if (ip.isEmpty()) continue;
            if (!trustedProxies.contains(ip)) return ip;
        }
        return normalize(parts[0]);
    }

    private String normalize(String ip) {
        if (ip == null) return "";
        String value = ip.trim();
        if ("0:0:0:0:0:0:0:1".equals(value)) return "::1";
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }
}

package com.example.escort.security;

import com.example.escort.module.auth.entity.SysUser;
import com.example.escort.module.auth.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Token 认证服务
 * 统一校验 JWT 签名、用户状态与令牌版本号，供 API 拦截器和上传文件拦截器复用。
 *
 * 安全语义：
 * - 账号不存在或状态非 ENABLED → 认证失败（禁用后旧 Token 立即失效）
 * - Token 中 tv（tokenVersion）与数据库不一致 → 认证失败（登出/改密/禁用后旧 Token 失效）
 * - Token 角色与数据库角色不一致 → 认证失败（防止角色被篡改或变更后沿用旧 Token）
 */
@Component
@RequiredArgsConstructor
public class TokenAuthService {

    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;

    /**
     * @return 认证通过返回用户实体；任意校验失败返回 null
     */
    public SysUser authenticate(String token) {
        if (token == null || token.isBlank()) return null;
        Claims claims = jwtUtil.parseToken(token);
        if (claims == null) return null;

        Long userId = claims.get("userId", Long.class);
        if (userId == null) return null;

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !"ENABLED".equals(user.getStatus())) return null;

        Integer tokenVersion = claims.get("tv", Integer.class);
        int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        if (tokenVersion == null || tokenVersion.intValue() != currentVersion) return null;

        String tokenRole = claims.get("role", String.class);
        if (tokenRole == null || !tokenRole.equals(user.getRole())) return null;

        return user;
    }
}

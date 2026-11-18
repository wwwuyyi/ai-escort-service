package com.example.escort.security;

import com.example.escort.module.auth.entity.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 * 对所有非公开接口校验JWT Token，解析后写入UserContext。
 * 公开接口（登录/注册/验证码/Knife4j文档）在 WebMvcConfig 中排除。
 *
 * 除签名/过期外，还会校验用户当前状态与令牌版本号：
 * 账号被禁用、用户登出或修改密码后，旧 Token 立即失效。
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final TokenAuthService tokenAuthService;

    public LoginInterceptor(TokenAuthService tokenAuthService) {
        this.tokenAuthService = tokenAuthService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = extractToken(request);
        SysUser user = tokenAuthService.authenticate(token);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":401,\"message\":\"未登录或Token已失效\",\"data\":null}");
            } catch (Exception ignored) {
            }
            return false;
        }

        // 写入上下文
        UserContext.setUserId(user.getId());
        UserContext.setUserRole(user.getRole());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 防止线程池复用ThreadLocal泄漏
        UserContext.clear();
    }

    /**
     * 从请求头提取Token，格式：Authorization: Bearer {token}
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}

package com.example.escort.security;

import com.example.escort.module.auth.entity.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 上传文件访问拦截器
 * 病历图片/资质材料属于敏感医疗数据，/upload/** 不再匿名公开。
 *
 * Token 来源：Authorization: Bearer {token}；图片标签无法携带请求头时允许 ?token={token}。
 * 注意：该拦截器只负责认证，不做逐文件归属校验（文件与业务实体的绑定关系在
 * medical_document / escort_profile.materials 中维护，属于后续细粒度 ACL 范围）。
 */
@Component
@RequiredArgsConstructor
public class UploadAuthInterceptor implements HandlerInterceptor {

    private final TokenAuthService tokenAuthService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        } else {
            token = request.getParameter("token");
        }
        SysUser user = tokenAuthService.authenticate(token);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或Token已失效\",\"data\":null}");
            return false;
        }
        return true;
    }
}

package com.example.escort.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 接口文档访问开关（BUG-043）。
 * 默认关闭，避免 /doc.html 与 /v3/api-docs 在生产/演示环境公开暴露。
 * 本地需要查看文档时设置环境变量 API_DOCS_ENABLED=true 后重启后端。
 */
@Component
public class ApiDocsGuardInterceptor implements HandlerInterceptor {

    @Value("${app.api-docs.enabled:false}")
    private boolean enabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (enabled) return true;
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":404,\"message\":\"接口文档已关闭\",\"data\":null}");
        return false;
    }
}

package com.example.escort.security;

import com.example.escort.common.BizException;
import com.example.escort.module.auth.entity.SysUser;
import com.example.escort.module.file.service.FileAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 上传文件访问拦截器（兼容旧 /upload/** 直链）。
 *
 * 安全语义（REG-001）：
 * - 匿名访问 → 401
 * - 已登录但无业务归属 → 403/404
 * - 仅接受 Authorization: Bearer 请求头，不再接受 URL 中的长期 JWT（REG-002）
 *
 * 前端展示图片统一改用 /api/file/signed-url 获取短期签名 URL；
 * /upload/** 保留给直接接口调用与兼容，但仍执行对象级授权。
 */
@Component
@RequiredArgsConstructor
public class UploadAuthInterceptor implements HandlerInterceptor {

    private final TokenAuthService tokenAuthService;
    private final FileAccessService fileAccessService;

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
            token = null; // 明确拒绝 ?token= 传参，避免长期 JWT 进入 URL
        }
        SysUser user = tokenAuthService.authenticate(token);
        if (user == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "未登录或Token已失效");
            return false;
        }
        try {
            fileAccessService.assertCanAccess(user.getId(), user.getRole(), request.getRequestURI());
        } catch (BizException e) {
            writeError(response, e.getCode() == 404 ? 404 : 403, e.getCode(), e.getMessage());
            return false;
        }
        return true;
    }

    private void writeError(HttpServletResponse response, int httpStatus, int code, String message)
            throws Exception {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"code\":%d,\"message\":\"%s\",\"data\":null}", code,
                message == null ? "无权限访问该文件" : message.replace("\"", "'")));
    }
}

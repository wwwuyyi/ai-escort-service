package com.example.escort.config;

import com.example.escort.security.LoginInterceptor;
import com.example.escort.security.UploadAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc配置
 * - 跨域（CORS）
 * - 登录拦截器注册 + 公开路径排除
 * - 文件上传目录静态资源映射
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final UploadAuthInterceptor uploadAuthInterceptor;
    private final ApiDocsGuardInterceptor apiDocsGuardInterceptor;

    /**
     * 跨域域名白名单，来自 application.yml 的 cors.allowed-origins。
     * 安全说明：allowCredentials(true) 时禁止使用通配符 "*"，否则任意站点均可携带凭证访问后端。
     */
    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;

    public WebMvcConfig(LoginInterceptor loginInterceptor,
                        UploadAuthInterceptor uploadAuthInterceptor,
                        ApiDocsGuardInterceptor apiDocsGuardInterceptor) {
        this.loginInterceptor = loginInterceptor;
        this.uploadAuthInterceptor = uploadAuthInterceptor;
        this.apiDocsGuardInterceptor = apiDocsGuardInterceptor;
    }

    /**
     * 跨域配置
     * 仅允许配置文件中指定的前端域名（默认 Vue dev server localhost:5173）跨域请求。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 拦截器注册
     * 排除公开接口：认证/验证码/Knife4j文档/文件上传访问/静态资源
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/captcha",
                        "/api/auth/register",
                        "/api/auth/login",
                        // 受控下载由短期签名保护，图片标签无法携带 Authorization
                        "/api/file/download",
                        "/upload/**",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**"
                );
        // 上传文件为敏感医疗数据：/upload/** 必须登录且按业务归属授权
        registry.addInterceptor(uploadAuthInterceptor)
                .addPathPatterns("/upload/**");
        // 接口文档默认关闭（设置 API_DOCS_ENABLED=true 可按需开启）
        registry.addInterceptor(apiDocsGuardInterceptor)
                .addPathPatterns(
                        "/doc.html",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**"
                );
    }

    /**
     * 静态资源映射：将 ./upload/ 目录映射为 /upload/ 访问路径
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:./upload/");
    }
}
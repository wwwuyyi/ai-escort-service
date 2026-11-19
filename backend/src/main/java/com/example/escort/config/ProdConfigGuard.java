package com.example.escort.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 生产环境敏感配置 fail-fast（BUG-030）。
 * 仅在 prod profile 生效；demo/dev 保留本地默认值并继续输出安全警告。
 */
@Slf4j
@Component
@Profile("prod")
public class ProdConfigGuard {

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${encrypt.aes-key:}")
    private String aesKey;

    @PostConstruct
    public void validate() {
        if (dbPassword == null || dbPassword.isBlank() || "root".equals(dbPassword)) {
            throw new IllegalStateException(
                    "生产环境必须通过 DB_PASSWORD 注入非默认数据库口令，拒绝启动");
        }
        if (aesKey == null || aesKey.isBlank()
                || "escort-aes-key-2026-demo".equals(aesKey)
                || aesKey.getBytes(StandardCharsets.UTF_8).length < 16) {
            throw new IllegalStateException(
                    "生产环境必须通过 AES_KEY 注入非默认 AES 密钥（≥16字节），拒绝启动");
        }
        log.info("生产环境敏感配置校验通过（DB_PASSWORD/AES_KEY 已注入）");
    }
}

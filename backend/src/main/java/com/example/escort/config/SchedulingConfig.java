package com.example.escort.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * 启用定时任务（登录限流计数清理等）。
 * Clock 以 Bean 形式提供，便于测试注入可控时钟验证锁定到期。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}

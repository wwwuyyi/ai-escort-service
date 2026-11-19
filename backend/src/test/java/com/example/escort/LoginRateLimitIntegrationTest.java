package com.example.escort;

import com.example.escort.module.auth.service.LoginRateLimiter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BUG-029 登录限流集成测试：
 * 连续 5 次错误密码后锁定，第 6 次错误密码/锁定期内正确密码均返回 429；
 * 成功登录清零计数。使用独立 properties 覆盖锁定时长，不引入任何生产后门。
 */
@SpringBootTest(properties = {
        "security.login.max-failures=5",
        "security.login.window-minutes=5",
        "security.login.lock-minutes=1"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(LoginRateLimitIntegrationTest.ClockConfig.class)
class LoginRateLimitIntegrationTest {

    private static final String IP = "127.0.0.1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoginRateLimiter loginRateLimiter;

    @Autowired
    private MutableClock clock;

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    static class MutableClock extends Clock {
        private long offsetMillis;

        void advanceMinutes(long minutes) {
            offsetMillis += minutes * 60_000L;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.now().plusMillis(offsetMillis);
        }

        @Override
        public long millis() {
            return System.currentTimeMillis() + offsetMillis;
        }
    }

    private String[] getCaptcha(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/captcha").session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String captchaKey = node.get("data").get("captchaKey").asText();
        String captchaCode = (String) session.getAttribute("CAPTCHA_" + captchaKey);
        assertNotNull(captchaCode, "验证码答案应在Session中");
        return new String[]{captchaKey, captchaCode};
    }

    private MvcResult attempt(String username, String password, String role, MockHttpSession session)
            throws Exception {
        String[] cap = getCaptcha(session);
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username, "password", password,
                "captcha", cap[1], "captchaKey", cap[0],
                "role", role
        ));
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .session(session)
                        .with(request -> {
                            request.setRemoteAddr(IP);
                            return request;
                        }))
                .andReturn();
    }

    @Test
    @DisplayName("连续失败锁定、锁定期正确密码 429、成功登录清零")
    void loginRateLimitFlow() throws Exception {
        String family = "13800001111";
        // 连续 5 次错误密码
        for (int i = 0; i < 5; i++) {
            MvcResult r = attempt(family, "WrongPass1", "FAMILY", new MockHttpSession());
            assertEquals(400, r.getResponse().getStatus(), "错误密码应返回 400");
            JsonNode node = objectMapper.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
            assertEquals(400, node.get("code").asInt());
        }
        assertTrue(loginRateLimiter.checkLocked(IP, family) > 0, "达到阈值后应处于锁定状态");

        // 第 6 次错误密码应返回 429
        MvcResult sixth = attempt(family, "WrongPass1", "FAMILY", new MockHttpSession());
        assertEquals(429, sixth.getResponse().getStatus(), "第 6 次错误密码应返回 429");

        // 锁定期内正确密码仍 429
        MvcResult locked = attempt(family, "Test1234", "FAMILY", new MockHttpSession());
        assertEquals(429, locked.getResponse().getStatus(), "锁定期内正确密码也应 429");
        JsonNode lockedNode = objectMapper.readTree(locked.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals(429, lockedNode.get("code").asInt());
        assertTrue(lockedNode.get("message").asText().contains("分钟"),
                "429 消息应包含剩余时间，实际为: " + lockedNode.get("message").asText()
                        + "，完整响应: " + locked.getResponse().getContentAsString(StandardCharsets.UTF_8));

        // 另一账号：一次失败后成功登录，计数应清零
        String escort = "13800002222";
        MvcResult oneFail = attempt(escort, "WrongPass1", "ESCORT", new MockHttpSession());
        assertEquals(400, oneFail.getResponse().getStatus());
        assertTrue(loginRateLimiter.checkLocked(IP, escort) == 0, "未达阈值不应锁定");

        MvcResult success = attempt(escort, "Test1234", "ESCORT", new MockHttpSession());
        assertEquals(200, success.getResponse().getStatus(), "正确密码应登录成功");
        assertEquals(0, loginRateLimiter.checkLocked(IP, escort), "成功登录后失败计数应清零");

        // 推进时钟越过 1 分钟锁定期，原账号应可正常登录
        clock.advanceMinutes(2);
        MvcResult afterExpiry = attempt(family, "Test1234", "FAMILY", new MockHttpSession());
        assertEquals(200, afterExpiry.getResponse().getStatus(), "锁定期满后应可正常登录");
    }
}

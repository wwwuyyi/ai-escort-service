package com.example.escort.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginClientIpResolverTest {

    @Test
    void ignoresForwardedForFromUntrustedRemote() {
        LoginClientIpResolver resolver = new LoginClientIpResolver("");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("1.2.3.4");
        request.addHeader("X-Forwarded-For", "9.9.9.9");
        assertEquals("1.2.3.4", resolver.resolve(request));
    }

    @Test
    void trustsForwardedForOnlyFromTrustedProxy() {
        LoginClientIpResolver resolver = new LoginClientIpResolver("10.0.0.1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.1.1.1, 9.9.9.9");
        // 从右向左跳过受信代理，取第一个非受信地址
        assertEquals("9.9.9.9", resolver.resolve(request));
    }

    @Test
    void proxiesWithRightmostTrustedStillResolveLeftUntrusted() {
        LoginClientIpResolver resolver = new LoginClientIpResolver("10.0.0.1, 10.0.0.2");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        request.addHeader("X-Forwarded-For", "8.8.8.8, 10.0.0.1");
        assertEquals("8.8.8.8", resolver.resolve(request));
    }
}

package com.power_puff.bms.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class LocalSessionSecurityConfigurationTest {

    @Autowired
    private Environment environment;

    @Test
    void apiBindsToLoopbackAndUsesExplicitSessionCookiePolicy() {
        assertEquals("127.0.0.1", environment.getProperty("server.address"));
        assertEquals("true",
                environment.getProperty("server.servlet.session.cookie.http-only"));
        assertEquals("lax",
                environment.getProperty("server.servlet.session.cookie.same-site"));
        assertEquals("false",
                environment.getProperty("server.servlet.session.cookie.secure"));
    }

    @Test
    void sessionIdleTimeoutIsExplicitAndConfigurable() {
        assertEquals("30m", environment.getProperty("server.servlet.session.timeout"));
    }
}

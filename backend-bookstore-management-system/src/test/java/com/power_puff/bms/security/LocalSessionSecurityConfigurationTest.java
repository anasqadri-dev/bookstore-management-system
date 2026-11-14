package com.power_puff.bms.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSessionSecurityConfigurationTest {

    private Properties productionProperties() {
        Path configuration = Path.of("src/main/resources/application.yaml")
                .toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(configuration),
                "Production application.yaml must be present for this configuration test");

        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource(configuration));
        yaml.afterPropertiesSet();
        Properties properties = yaml.getObject();
        assertNotNull(properties);
        return properties;
    }

    @Test
    void apiBindsToLoopbackAndUsesExplicitSessionCookiePolicy() {
        Properties properties = productionProperties();
        assertEquals("127.0.0.1", properties.getProperty("server.address"));
        assertEquals("true",
                properties.getProperty("server.servlet.session.cookie.http-only"));
        assertEquals("lax",
                properties.getProperty("server.servlet.session.cookie.same-site"));
        assertEquals("false",
                properties.getProperty("server.servlet.session.cookie.secure"));
    }

    @Test
    void sessionIdleTimeoutIsExplicitAndConfigurable() {
        assertEquals("${BMS_SESSION_TIMEOUT:30m}",
                productionProperties().getProperty("server.servlet.session.timeout"));
    }
}

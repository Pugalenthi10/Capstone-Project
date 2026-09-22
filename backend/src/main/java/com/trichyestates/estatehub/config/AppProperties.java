package com.trichyestates.estatehub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Typed view of every custom "app.*" setting in application.properties. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cors cors,
        String frontendUrl,
        Contact contact,
        PasswordReset passwordReset,
        Mail mail,
        Seed seed) {

    public record Jwt(String secret, long expirationMs) { }

    public record Cors(List<String> allowedOrigins) { }

    public record Contact(String phone, String whatsapp) { }

    public record PasswordReset(long expirationMinutes) { }

    public record Mail(String from, boolean logResetLinks) { }

    public record Seed(boolean enabled,
                       String adminEmail, String adminMobile, String adminPassword,
                       String agentEmail, String agentMobile, String agentPassword) { }
}

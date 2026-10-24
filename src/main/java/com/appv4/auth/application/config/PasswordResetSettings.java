package com.appv4.auth.application.config;

import java.time.Duration;

/**
 * TTL jeton + URL FE reset (issue #31) — sans {@code @ConfigurationProperties}.
 */
public record PasswordResetSettings(Duration tokenTtl, String frontendResetUrl) {
}

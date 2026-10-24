package com.appv4.configuration;

import org.springframework.context.annotation.Configuration;

/**
 * Point d'extension transversal (hors auth).
 *
 * <p>CORS : une seule source dans {@code AuthConfiguration#corsConfigurationSource}.
 * {@link CorsProperties} est activé côté auth (issue #61 — plus de double mapping MVC).
 */
@Configuration
public class ApplicationConfiguration {
}

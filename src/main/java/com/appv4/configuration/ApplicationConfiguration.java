package com.appv4.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration transversale (CORS pour les fronts Angular locaux, etc.).
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class ApplicationConfiguration implements WebMvcConfigurer {

	private final CorsProperties corsProperties;

	public ApplicationConfiguration(CorsProperties corsProperties) {
		this.corsProperties = corsProperties;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		var origins = corsProperties.getAllowedOrigins();
		if (origins == null || origins.isEmpty()) {
			return;
		}
		registry.addMapping("/api/**")
				.allowedOrigins(origins.toArray(String[]::new))
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.allowCredentials(true);
	}
}

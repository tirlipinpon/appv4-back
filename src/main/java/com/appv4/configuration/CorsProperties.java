package com.appv4.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Origines CORS autorisées (ex. apps Angular locales).
 */
@ConfigurationProperties(prefix = "appv4.cors")
public class CorsProperties {

	private List<String> allowedOrigins = new ArrayList<>();

	public List<String> getAllowedOrigins() {
		return allowedOrigins;
	}

	public void setAllowedOrigins(List<String> allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}
}

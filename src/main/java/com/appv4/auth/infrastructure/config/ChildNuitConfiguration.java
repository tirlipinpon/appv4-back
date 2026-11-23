package com.appv4.auth.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.appv4.auth.application.port.in.SejourUseCase;
import com.appv4.auth.infrastructure.adapter.in.web.ChildNuitInterceptor;

/**
 * Branche la Nuit sur toutes les routes enfant.
 *
 * <p><b>Business :</b> de 22 h à 6 h, une app modifiée ne continue pas à jouer.
 * Le parent et le professeur ne sont pas fermés.
 * <p><b>Technique :</b> intercepteur MVC sur {@code /api/ext/**}.
 * <p><b>Métier / flow :</b> avant le controller enfant → {@link SejourUseCase#refuseDuringNuit}.
 */
@Configuration
public class ChildNuitConfiguration implements WebMvcConfigurer {

	private final SejourUseCase sejour;

	public ChildNuitConfiguration(SejourUseCase sejour) {
		this.sejour = sejour;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new ChildNuitInterceptor(sejour)).addPathPatterns("/api/ext/**");
	}
}

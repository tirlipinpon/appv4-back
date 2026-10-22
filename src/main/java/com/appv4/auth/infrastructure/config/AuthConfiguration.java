package com.appv4.auth.infrastructure.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.service.IssueJwtService;
import com.appv4.auth.infrastructure.adapter.in.web.JwtAuthenticationFilter;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtTokenAdapter;
import com.appv4.configuration.CorsProperties;

/**
 * Câblage Spring Security + JWT (socle SCRUM-93).
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class AuthConfiguration {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	JjwtTokenAdapter jjwtTokenAdapter(JwtProperties jwtProperties) {
		return new JjwtTokenAdapter(jwtProperties);
	}

	@Bean
	IssueJwtUseCase issueJwtUseCase(JjwtTokenAdapter jjwtTokenAdapter) {
		return new IssueJwtService(jjwtTokenAdapter);
	}

	@Bean
	JwtAuthenticationFilter jwtAuthenticationFilter(JjwtTokenAdapter jjwtTokenAdapter) {
		return new JwtAuthenticationFilter(jjwtTokenAdapter);
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtAuthenticationFilter jwtAuthenticationFilter,
			CorsConfigurationSource corsConfigurationSource) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/api/ext/**").permitAll()
						// Futures routes publiques auth (signup/login/oauth/forgot) :
						// .requestMatchers("/api/int/auth/signup", "/api/int/auth/login", ...).permitAll()
						.requestMatchers("/api/int/**").authenticated()
						.anyRequest().permitAll())
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
					response.sendError(
							HttpStatus.UNAUTHORIZED.value(),
							HttpStatus.UNAUTHORIZED.getReasonPhrase());
				}))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
		CorsConfiguration config = new CorsConfiguration();
		List<String> origins = corsProperties.getAllowedOrigins();
		if (origins != null && !origins.isEmpty()) {
			config.setAllowedOrigins(origins);
		}
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}
}

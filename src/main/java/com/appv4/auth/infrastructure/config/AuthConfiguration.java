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
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.appv4.auth.application.port.in.BuildGoogleAuthorizeUrlUseCase;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.in.LoginWithGoogleUseCase;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.GoogleOAuthClient;
import com.appv4.auth.application.service.BuildGoogleAuthorizeUrlService;
import com.appv4.auth.application.service.IssueJwtService;
import com.appv4.auth.application.service.LoginWithGoogleService;
import com.appv4.auth.infrastructure.adapter.in.web.JwtAuthenticationFilter;
import com.appv4.auth.infrastructure.adapter.out.google.HttpGoogleOAuthClient;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtTokenAdapter;
import com.appv4.configuration.CorsProperties;

/**
 * Point central du socle auth (SCRUM-93).
 *
 * <p><b>Métier :</b> l'API admin ({@code /api/int/**}) ne doit être utilisable
 * que par un parent/prof/admin connecté. L'API learning ({@code /api/ext/**})
 * reste ouverte pour l'instant (auth enfant = tickets plus tard).
 *
 * <p><b>Technique :</b> pas de session HTTP serveur (STATELESS). Le client
 * Angular envoie un JWT dans {@code Authorization: Bearer ...}. Spring Security
 * décide si la requête passe ({@code authenticated()}) ou renvoie 401.
 *
 * <p>Flux d'une requête protégée :
 * <ol>
 *   <li>{@link JwtAuthenticationFilter} lit le Bearer et, si le JWT est valide,
 *       remplit le {@code SecurityContext}</li>
 *   <li>{@code authorizeHttpRequests} vérifie que {@code /api/int/**} a une auth</li>
 *   <li>Sinon → 401 Unauthorized (pas de formulaire login HTML)</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({ JwtProperties.class, GoogleOAuthProperties.class })
public class AuthConfiguration {

	/**
	 * Hash des mots de passe (BCrypt).
	 *
	 * <p>On ne stocke jamais le mot de passe en clair en base : uniquement le hash.
	 * Utilisé dès le signup / login (tickets suivants).
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * Un seul bean JJWT qui sait à la fois créer et lire un token
	 * (évite les doublons de beans Spring).
	 */
	@Bean
	JjwtTokenAdapter jjwtTokenAdapter(JwtProperties jwtProperties) {
		return new JjwtTokenAdapter(jwtProperties);
	}

	/**
	 * Port métier « émettre un JWT » — les use cases login/oauth appelleront ça plus tard.
	 */
	@Bean
	IssueJwtUseCase issueJwtUseCase(JjwtTokenAdapter jjwtTokenAdapter) {
		return new IssueJwtService(jjwtTokenAdapter);
	}

	@Bean
	JwtAuthenticationFilter jwtAuthenticationFilter(JjwtTokenAdapter jjwtTokenAdapter) {
		return new JwtAuthenticationFilter(jjwtTokenAdapter);
	}

	@Bean
	RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}

	@Bean
	GoogleOAuthClient googleOAuthClient(
			GoogleOAuthProperties googleOAuthProperties,
			RestClient.Builder restClientBuilder) {
		return new HttpGoogleOAuthClient(googleOAuthProperties, restClientBuilder);
	}

	@Bean
	BuildGoogleAuthorizeUrlUseCase buildGoogleAuthorizeUrlUseCase(GoogleOAuthProperties googleOAuthProperties) {
		return new BuildGoogleAuthorizeUrlService(googleOAuthProperties);
	}

	@Bean
	LoginWithGoogleUseCase loginWithGoogleUseCase(
			GoogleOAuthClient googleOAuthClient,
			AuthUserAccountPort authUserAccountPort,
			IssueJwtUseCase issueJwtUseCase) {
		return new LoginWithGoogleService(googleOAuthClient, authUserAccountPort, issueJwtUseCase);
	}

	/**
	 * Règles de sécurité HTTP.
	 *
	 * <ul>
	 *   <li>CSRF off : API JSON + JWT, pas de cookies de session classiques</li>
	 *   <li>STATELESS : chaque requête doit prouver l'identité (Bearer)</li>
	 *   <li>health/info publics : pour Docker / Azure / monitoring</li>
	 *   <li>OPTIONS publics : préflight CORS navigateur</li>
	 *   <li>{@code /api/ext/**} public temporairement (learning)</li>
	 *   <li>OAuth Google public (SCRUM-96) : authorize-url + échange code</li>
	 *   <li>{@code /api/int/**} authentifié (admin)</li>
	 * </ul>
	 */
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
						.requestMatchers("/api/int/auth/oauth/google", "/api/int/auth/oauth/google/**").permitAll()
						// Plus tard : signup / login email / forgot en permitAll
						.requestMatchers("/api/int/**").authenticated()
						.anyRequest().permitAll())
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				// API REST : on veut 401 JSON/HTTP, pas une redirection vers une page login
				.exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
					response.sendError(
							HttpStatus.UNAUTHORIZED.value(),
							HttpStatus.UNAUTHORIZED.getReasonPhrase());
				}))
				// Le filtre JWT tourne AVANT le filtre username/password (inutile ici mais ordre standard)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	/**
	 * CORS aussi côté Security (sinon le navigateur bloque Angular :4201 → :8080).
	 * Les origines viennent de {@link CorsProperties} (profil local).
	 */
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

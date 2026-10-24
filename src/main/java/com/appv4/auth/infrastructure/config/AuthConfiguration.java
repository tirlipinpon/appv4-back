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

import com.appv4.auth.application.port.in.BuildAppleAuthorizeUrlUseCase;
import com.appv4.auth.application.port.in.BuildFacebookAuthorizeUrlUseCase;
import com.appv4.auth.application.port.in.BuildGoogleAuthorizeUrlUseCase;
import com.appv4.auth.application.port.in.CompleteRolesUseCase;
import com.appv4.auth.application.port.in.ForgotPasswordUseCase;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.in.LoginWithAppleUseCase;
import com.appv4.auth.application.port.in.LoginWithEmailUseCase;
import com.appv4.auth.application.port.in.LoginWithFacebookUseCase;
import com.appv4.auth.application.port.in.LoginWithGoogleUseCase;
import com.appv4.auth.application.port.in.ResetPasswordUseCase;
import com.appv4.auth.application.port.in.SignupWithEmailUseCase;
import com.appv4.auth.application.port.in.SwitchActiveRoleUseCase;
import com.appv4.auth.application.port.in.TeacherModerationUseCase;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.AppleOAuthClient;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.FacebookOAuthClient;
import com.appv4.auth.application.port.out.GoogleOAuthClient;
import com.appv4.auth.application.port.out.PasswordResetMailPort;
import com.appv4.auth.application.port.out.PasswordResetTokenPort;
import com.appv4.auth.application.port.out.SignupRolePort;
import com.appv4.auth.application.port.out.TeacherApprovedMailPort;
import com.appv4.auth.application.port.out.TeacherModerationPort;
import com.appv4.auth.application.service.BuildAppleAuthorizeUrlService;
import com.appv4.auth.application.service.BuildFacebookAuthorizeUrlService;
import com.appv4.auth.application.service.BuildGoogleAuthorizeUrlService;
import com.appv4.auth.application.service.CompleteRolesService;
import com.appv4.auth.application.service.ForgotPasswordService;
import com.appv4.auth.application.service.IssueJwtService;
import com.appv4.auth.application.service.LoginWithAppleService;
import com.appv4.auth.application.service.LoginWithEmailService;
import com.appv4.auth.application.service.LoginWithFacebookService;
import com.appv4.auth.application.service.LoginWithGoogleService;
import com.appv4.auth.application.service.ResetPasswordService;
import com.appv4.auth.application.service.SignupWithEmailService;
import com.appv4.auth.application.service.SwitchActiveRoleService;
import com.appv4.auth.application.service.TeacherModerationService;
import com.appv4.auth.infrastructure.adapter.in.web.JwtAuthenticationFilter;
import com.appv4.auth.infrastructure.adapter.out.apple.HttpAppleOAuthClient;
import com.appv4.auth.infrastructure.adapter.out.facebook.HttpFacebookOAuthClient;
import com.appv4.auth.infrastructure.adapter.out.google.HttpGoogleOAuthClient;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtTokenAdapter;
import com.appv4.auth.infrastructure.adapter.out.mail.LoggingPasswordResetMailAdapter;
import com.appv4.auth.infrastructure.adapter.out.mail.LoggingTeacherApprovedMailAdapter;
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
@EnableConfigurationProperties({
		JwtProperties.class,
		GoogleOAuthProperties.class,
		FacebookOAuthProperties.class,
		AppleOAuthProperties.class,
		PasswordResetProperties.class,
		CorsProperties.class })
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
	IssueJwtUseCase issueJwtUseCase(JjwtTokenAdapter jjwtTokenAdapter, ActiveRolePort activeRolePort) {
		return new IssueJwtService(jjwtTokenAdapter, activeRolePort);
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

	@Bean
	FacebookOAuthClient facebookOAuthClient(
			FacebookOAuthProperties facebookOAuthProperties,
			RestClient.Builder restClientBuilder) {
		return new HttpFacebookOAuthClient(facebookOAuthProperties, restClientBuilder);
	}

	@Bean
	BuildFacebookAuthorizeUrlUseCase buildFacebookAuthorizeUrlUseCase(
			FacebookOAuthProperties facebookOAuthProperties) {
		return new BuildFacebookAuthorizeUrlService(facebookOAuthProperties);
	}

	@Bean
	LoginWithFacebookUseCase loginWithFacebookUseCase(
			FacebookOAuthClient facebookOAuthClient,
			AuthUserAccountPort authUserAccountPort,
			IssueJwtUseCase issueJwtUseCase) {
		return new LoginWithFacebookService(facebookOAuthClient, authUserAccountPort, issueJwtUseCase);
	}

	@Bean
	AppleOAuthClient appleOAuthClient(
			AppleOAuthProperties appleOAuthProperties,
			RestClient.Builder restClientBuilder) {
		return new HttpAppleOAuthClient(appleOAuthProperties, restClientBuilder);
	}

	@Bean
	BuildAppleAuthorizeUrlUseCase buildAppleAuthorizeUrlUseCase(AppleOAuthProperties appleOAuthProperties) {
		return new BuildAppleAuthorizeUrlService(appleOAuthProperties);
	}

	@Bean
	LoginWithAppleUseCase loginWithAppleUseCase(
			AppleOAuthClient appleOAuthClient,
			AuthUserAccountPort authUserAccountPort,
			IssueJwtUseCase issueJwtUseCase) {
		return new LoginWithAppleService(appleOAuthClient, authUserAccountPort, issueJwtUseCase);
	}

	/**
	 * Inscription email/mdp (SCRUM-105) — hash BCrypt + JWT.
	 */
	@Bean
	SignupWithEmailUseCase signupWithEmailUseCase(
			AuthUserAccountPort authUserAccountPort,
			PasswordEncoder passwordEncoder,
			IssueJwtUseCase issueJwtUseCase) {
		return new SignupWithEmailService(authUserAccountPort, passwordEncoder, issueJwtUseCase);
	}

	/**
	 * Choix de rôle après création du compte (SCRUM-153), email ou réseau social.
	 */
	@Bean
	CompleteRolesUseCase completeRolesUseCase(
			ActiveRolePort activeRolePort,
			SignupRolePort signupRolePort,
			IssueJwtUseCase issueJwtUseCase) {
		return new CompleteRolesService(activeRolePort, signupRolePort, issueJwtUseCase);
	}

	/**
	 * Login email/mdp (SCRUM-117) — BCrypt + JWT ; 401 message unique si échec.
	 */
	@Bean
	LoginWithEmailUseCase loginWithEmailUseCase(
			AuthUserAccountPort authUserAccountPort,
			PasswordEncoder passwordEncoder,
			IssueJwtUseCase issueJwtUseCase) {
		return new LoginWithEmailService(authUserAccountPort, passwordEncoder, issueJwtUseCase);
	}

	/**
	 * Changement d'espace (SCRUM-124) — vérifie le rôle tenu, met à jour le souvenir, nouveau JWT.
	 */
	@Bean
	SwitchActiveRoleUseCase switchActiveRoleUseCase(
			ActiveRolePort activeRolePort,
			IssueJwtUseCase issueJwtUseCase) {
		return new SwitchActiveRoleService(activeRolePort, issueJwtUseCase);
	}

	@Bean
	PasswordResetMailPort passwordResetMailPort() {
		return new LoggingPasswordResetMailAdapter();
	}

	/**
	 * Modération des demandes professeur (SCRUM-142).
	 * Le rôle admin est relu en base. L'email part au moment du oui.
	 */
	@Bean
	TeacherApprovedMailPort teacherApprovedMailPort() {
		return new LoggingTeacherApprovedMailAdapter();
	}

	@Bean
	TeacherModerationUseCase teacherModerationUseCase(
			ActiveRolePort activeRolePort,
			TeacherModerationPort teacherModerationPort,
			TeacherApprovedMailPort teacherApprovedMailPort) {
		return new TeacherModerationService(activeRolePort, teacherModerationPort, teacherApprovedMailPort);
	}

	/**
	 * Forgot-password (SCRUM-108) — jeton hashé + lien (log local).
	 */
	@Bean
	ForgotPasswordUseCase forgotPasswordUseCase(
			AuthUserAccountPort authUserAccountPort,
			PasswordResetTokenPort passwordResetTokenPort,
			PasswordResetMailPort passwordResetMailPort,
			PasswordResetProperties passwordResetProperties) {
		return new ForgotPasswordService(
				authUserAccountPort,
				passwordResetTokenPort,
				passwordResetMailPort,
				passwordResetProperties);
	}

	/**
	 * Reset-password / renew (SCRUM-111) — consomme jeton + nouveau hash BCrypt.
	 */
	@Bean
	ResetPasswordUseCase resetPasswordUseCase(
			PasswordResetTokenPort passwordResetTokenPort,
			AuthUserAccountPort authUserAccountPort,
			PasswordEncoder passwordEncoder) {
		return new ResetPasswordService(passwordResetTokenPort, authUserAccountPort, passwordEncoder);
	}

	/**
	 * Règles de sécurité HTTP.
	 *
	 * <p><b>Business :</b> tout chemin non listé explicitement est refusé — un oubli de
	 * préfixe ne publie pas une future feature (issue #60).
	 * <p><b>Technique :</b> {@code authorizeHttpRequests} : permitAll ciblés, puis
	 * {@code /api/int/**} authentifié, puis {@code anyRequest().denyAll()}.
	 * <p><b>Métier / flow :</b> navigateur / monitoring → routes publiques ; admin JWT →
	 * {@code /api/int/**} ; le reste → 401 (anonyme) ou 403 (JWT valide hors inventaire).
	 *
	 * <ul>
	 *   <li>CSRF off : API JSON + JWT, pas de cookies de session classiques</li>
	 *   <li>STATELESS : chaque requête doit prouver l'identité (Bearer)</li>
	 *   <li>health/info publics : pour Docker / Azure / monitoring</li>
	 *   <li>{@code /error} public : page d'erreur Spring Boot (sinon 403 en boucle)</li>
	 *   <li>OPTIONS publics : préflight CORS navigateur</li>
	 *   <li>{@code /api/ext/**} public temporairement (learning)</li>
	 *   <li>OAuth Google / Facebook / Apple publics : authorize-url + échange code</li>
	 *   <li>Signup email/mdp public (SCRUM-105)</li>
	 *   <li>Login email/mdp public (SCRUM-117)</li>
	 *   <li>Forgot / reset-password publics (SCRUM-108 / SCRUM-111)</li>
	 *   <li>OpenAPI / Swagger UI publics (SCRUM-147) — UI off hors local</li>
	 *   <li>{@code /api/int/**} authentifié (admin)</li>
	 *   <li>Règle finale {@code denyAll} : chemins hors inventaire refusés</li>
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
						.requestMatchers("/error").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/v3/api-docs", "/v3/api-docs/**").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**").permitAll()
						.requestMatchers("/api/ext/**").permitAll()
						.requestMatchers("/api/int/auth/oauth/google", "/api/int/auth/oauth/google/**")
						.permitAll()
						.requestMatchers("/api/int/auth/oauth/facebook", "/api/int/auth/oauth/facebook/**")
						.permitAll()
						.requestMatchers("/api/int/auth/oauth/apple", "/api/int/auth/oauth/apple/**")
						.permitAll()
						.requestMatchers("/api/int/auth/signup").permitAll()
						.requestMatchers("/api/int/auth/login").permitAll()
						.requestMatchers("/api/int/auth/forgot-password").permitAll()
						.requestMatchers("/api/int/auth/reset-password").permitAll()
						.requestMatchers("/api/int/**").authenticated()
						.anyRequest().denyAll())
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
	 * Unique source CORS (Security). Sans ça le navigateur bloque Angular :4201 → :8080.
	 * Les origines viennent de {@link CorsProperties} (profil local).
	 * Pas de second mapping MVC — issue #61.
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

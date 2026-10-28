package com.appv4.auth.infrastructure.config;

import com.appv4.auth.application.service.question.QuestionFormHandler;
import com.appv4.auth.application.service.question.QuestionFormRegistry;
import com.appv4.auth.application.service.question.QcmQuestionForm;
import com.appv4.auth.application.service.question.MemoryQuestionForm;
import com.appv4.auth.application.service.question.OrderQuestionForm;
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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.appv4.auth.application.port.in.AddMissingSpaceUseCase;
import com.appv4.auth.application.port.in.BuildAppleAuthorizeUrlUseCase;
import com.appv4.auth.application.port.in.BuildFacebookAuthorizeUrlUseCase;
import com.appv4.auth.application.port.in.BuildGoogleAuthorizeUrlUseCase;
import com.appv4.auth.application.port.in.CompleteRolesUseCase;
import com.appv4.auth.application.port.in.ConnectedAccountUseCase;
import com.appv4.auth.application.port.in.ForgotPasswordUseCase;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.in.LoginWithAppleUseCase;
import com.appv4.auth.application.port.in.ResumeAppleOAuthCallbackUseCase;
import com.appv4.auth.application.port.in.LoginWithEmailUseCase;
import com.appv4.auth.application.port.in.LoginWithFacebookUseCase;
import com.appv4.auth.application.port.in.LoginWithGoogleUseCase;
import com.appv4.auth.application.port.in.ParentChildrenUseCase;
import com.appv4.auth.application.port.in.ResetPasswordUseCase;
import com.appv4.auth.application.port.in.SignupWithEmailUseCase;
import com.appv4.auth.application.port.in.SwitchActiveRoleUseCase;
import com.appv4.auth.application.port.in.TeacherModerationUseCase;
import com.appv4.auth.application.port.in.TeacherProfileUseCase;
import com.appv4.auth.application.port.in.TeacherStudentsUseCase;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase;
import com.appv4.auth.application.port.in.TeacherSubjectUseCase;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.AppleOAuthClient;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.EnrolledChildPort;
import com.appv4.auth.application.port.out.FacebookOAuthClient;
import com.appv4.auth.application.port.out.GoogleOAuthClient;
import com.appv4.auth.application.port.out.PasswordResetMailPort;
import com.appv4.auth.application.port.out.PasswordResetTokenPort;
import com.appv4.auth.application.port.out.SchoolCatalogPort;
import com.appv4.auth.application.port.out.SignupRolePort;
import com.appv4.auth.application.port.out.TeacherApprovedMailPort;
import com.appv4.auth.application.port.out.TeacherModerationPort;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.service.AddMissingSpaceService;
import com.appv4.auth.application.service.BuildAppleAuthorizeUrlService;
import com.appv4.auth.application.service.BuildFacebookAuthorizeUrlService;
import com.appv4.auth.application.service.BuildGoogleAuthorizeUrlService;
import com.appv4.auth.application.service.CompleteRolesService;
import com.appv4.auth.application.service.ConnectedAccountService;
import com.appv4.auth.application.service.ForgotPasswordService;
import com.appv4.auth.application.service.IssueJwtService;
import com.appv4.auth.application.service.LoginWithAppleService;
import com.appv4.auth.application.service.ResumeAppleOAuthCallbackService;
import com.appv4.auth.application.service.LoginWithEmailService;
import com.appv4.auth.application.service.LoginWithFacebookService;
import com.appv4.auth.application.service.LoginWithGoogleService;
import com.appv4.auth.application.service.ChildSubjectService;
import com.appv4.auth.application.service.ParentChildrenService;
import com.appv4.auth.application.service.ResetPasswordService;
import com.appv4.auth.application.service.SignupWithEmailService;
import com.appv4.auth.application.service.SwitchActiveRoleService;
import com.appv4.auth.application.service.TeacherModerationService;
import com.appv4.auth.application.service.TeacherProfileService;
import com.appv4.auth.application.service.TeacherStudentsService;
import com.appv4.auth.application.service.TeacherQuestionService;
import com.appv4.auth.application.service.TeacherSubjectService;
import com.appv4.auth.infrastructure.adapter.in.web.AuthRateLimitFilter;
import com.appv4.auth.infrastructure.adapter.in.web.InMemoryAuthRateLimiter;
import com.appv4.auth.infrastructure.adapter.in.web.JwtAuthenticationFilter;
import com.appv4.auth.infrastructure.adapter.out.apple.HttpAppleOAuthClient;
import com.appv4.auth.infrastructure.adapter.out.facebook.HttpFacebookOAuthClient;
import com.appv4.auth.infrastructure.adapter.out.google.HttpGoogleOAuthClient;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtTokenAdapter;
import com.appv4.auth.infrastructure.adapter.out.mail.LoggingPasswordResetMailAdapter;
import com.appv4.auth.infrastructure.adapter.out.mail.LoggingTeacherApprovedMailAdapter;
import com.appv4.auth.infrastructure.adapter.out.mail.NoOpPasswordResetMailAdapter;
import com.appv4.auth.infrastructure.adapter.out.mail.NoOpTeacherApprovedMailAdapter;
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
		CorsProperties.class,
		AuthRateLimitProperties.class,
		MailTransportProperties.class,
		HttpClientProperties.class })
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
	IssueJwtUseCase issueJwtUseCase(
			JjwtTokenAdapter jjwtTokenAdapter,
			ActiveRolePort activeRolePort,
			AuthUserAccountPort authUserAccountPort) {
		return new IssueJwtService(jjwtTokenAdapter, activeRolePort, authUserAccountPort);
	}

	@Bean
	JwtAuthenticationFilter jwtAuthenticationFilter(
			JjwtTokenAdapter jjwtTokenAdapter,
			AuthUserAccountPort authUserAccountPort,
			ActiveRolePort activeRolePort) {
		return new JwtAuthenticationFilter(jjwtTokenAdapter, authUserAccountPort, activeRolePort);
	}

	/**
	 * Limiteur en mémoire pour les routes auth publiques (issue #59).
	 */
	@Bean
	InMemoryAuthRateLimiter inMemoryAuthRateLimiter(AuthRateLimitProperties properties) {
		return new InMemoryAuthRateLimiter(properties.getMaxRequests(), properties.getWindowSeconds());
	}

	@Bean
	AuthRateLimitFilter authRateLimitFilter(
			AuthRateLimitProperties properties,
			InMemoryAuthRateLimiter inMemoryAuthRateLimiter) {
		return new AuthRateLimitFilter(properties, inMemoryAuthRateLimiter);
	}

	/**
	 * RestClient OAuth avec connect/read timeout explicites (issue #57).
	 */
	@Bean
	RestClient.Builder restClientBuilder(HttpClientProperties httpClientProperties) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(httpClientProperties.getConnectTimeout());
		factory.setReadTimeout(httpClientProperties.getReadTimeout());
		return RestClient.builder().requestFactory(factory);
	}

	@Bean
	GoogleOAuthClient googleOAuthClient(
			GoogleOAuthProperties googleOAuthProperties,
			RestClient.Builder restClientBuilder) {
		return new HttpGoogleOAuthClient(googleOAuthProperties, restClientBuilder);
	}

	@Bean
	BuildGoogleAuthorizeUrlUseCase buildGoogleAuthorizeUrlUseCase(GoogleOAuthProperties p) {
		return new BuildGoogleAuthorizeUrlService(() -> new com.appv4.auth.application.config.GoogleOAuthSettings(
				p.getClientId(), p.getAuthorizationUri(), p.isConfigured()));
	}

	@Bean
	org.springframework.transaction.support.TransactionTemplate transactionTemplate(
			org.springframework.transaction.PlatformTransactionManager transactionManager) {
		return new org.springframework.transaction.support.TransactionTemplate(transactionManager);
	}

	@Bean
	LoginWithGoogleUseCase loginWithGoogleUseCase(
			GoogleOAuthClient googleOAuthClient,
			AuthUserAccountPort authUserAccountPort,
			IssueJwtUseCase issueJwtUseCase,
			org.springframework.transaction.support.TransactionTemplate transactionTemplate) {
		return new LoginWithGoogleService(
				googleOAuthClient, authUserAccountPort, issueJwtUseCase, transactionTemplate);
	}

	@Bean
	FacebookOAuthClient facebookOAuthClient(
			FacebookOAuthProperties facebookOAuthProperties,
			RestClient.Builder restClientBuilder) {
		return new HttpFacebookOAuthClient(facebookOAuthProperties, restClientBuilder);
	}

	@Bean
	BuildFacebookAuthorizeUrlUseCase buildFacebookAuthorizeUrlUseCase(FacebookOAuthProperties p) {
		return new BuildFacebookAuthorizeUrlService(() -> new com.appv4.auth.application.config.FacebookOAuthSettings(
				p.getAppId(), p.getAuthorizationUri(), p.isConfigured()));
	}

	@Bean
	LoginWithFacebookUseCase loginWithFacebookUseCase(
			FacebookOAuthClient facebookOAuthClient,
			AuthUserAccountPort authUserAccountPort,
			IssueJwtUseCase issueJwtUseCase,
			org.springframework.transaction.support.TransactionTemplate transactionTemplate) {
		return new LoginWithFacebookService(
				facebookOAuthClient, authUserAccountPort, issueJwtUseCase, transactionTemplate);
	}

	@Bean
	AppleOAuthClient appleOAuthClient(
			AppleOAuthProperties appleOAuthProperties,
			RestClient.Builder restClientBuilder) {
		return new HttpAppleOAuthClient(appleOAuthProperties, restClientBuilder);
	}

	@Bean
	BuildAppleAuthorizeUrlUseCase buildAppleAuthorizeUrlUseCase(AppleOAuthProperties p) {
		return new BuildAppleAuthorizeUrlService(() -> new com.appv4.auth.application.config.AppleOAuthSettings(
				p.getClientId(),
				p.getAuthorizationUri(),
				p.getCallbackUri(),
				p.getAllowedReturnUriPrefixes(),
				p.isConfigured()));
	}

	@Bean
	com.appv4.auth.application.port.out.OAuthBrowserBindingPort oauthBrowserBindingPort(
			JwtProperties jwtProperties) {
		return new com.appv4.auth.infrastructure.adapter.out.oauth.HmacOAuthBrowserBindingAdapter(
				jwtProperties);
	}

	@Bean
	ResumeAppleOAuthCallbackUseCase resumeAppleOAuthCallbackUseCase(
			AppleOAuthProperties p,
			com.appv4.auth.application.port.out.OAuthBrowserBindingPort oauthBrowserBindingPort) {
		return new ResumeAppleOAuthCallbackService(
				() -> new com.appv4.auth.application.config.AppleOAuthSettings(
						p.getClientId(),
						p.getAuthorizationUri(),
						p.getCallbackUri(),
						p.getAllowedReturnUriPrefixes(),
						p.isConfigured()),
				oauthBrowserBindingPort);
	}

	@Bean
	LoginWithAppleUseCase loginWithAppleUseCase(
			AppleOAuthClient appleOAuthClient,
			AuthUserAccountPort authUserAccountPort,
			IssueJwtUseCase issueJwtUseCase,
			org.springframework.transaction.support.TransactionTemplate transactionTemplate) {
		return new LoginWithAppleService(
				appleOAuthClient, authUserAccountPort, issueJwtUseCase, transactionTemplate);
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
			SignupRolePort signupRolePort,
			IssueJwtUseCase issueJwtUseCase) {
		return new CompleteRolesService(signupRolePort, issueJwtUseCase);
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
	 * Espace manquant plus tard (SCRUM-152) — parent immédiat, professeur en demande.
	 * Ne passe pas par switch-role : ce chemin ne crée pas de rôle.
	 */
	@Bean
	AddMissingSpaceUseCase addMissingSpaceUseCase(
			SignupRolePort signupRolePort,
			ActiveRolePort activeRolePort,
			IssueJwtUseCase issueJwtUseCase) {
		return new AddMissingSpaceService(signupRolePort, activeRolePort, issueJwtUseCase);
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

	/**
	 * Mail reset : logging (local/test) ou noop hors local — jamais le jeton en prod (issue #58).
	 */
	@Bean
	PasswordResetMailPort passwordResetMailPort(MailTransportProperties mailTransportProperties) {
		if (mailTransportProperties.isLogging()) {
			return new LoggingPasswordResetMailAdapter();
		}
		return new NoOpPasswordResetMailAdapter();
	}

	/**
	 * Modération des demandes professeur (SCRUM-142).
	 * Le rôle admin est relu en base. L'email part au moment du oui (logging local / noop sinon).
	 */
	@Bean
	TeacherApprovedMailPort teacherApprovedMailPort(MailTransportProperties mailTransportProperties) {
		if (mailTransportProperties.isLogging()) {
			return new LoggingTeacherApprovedMailAdapter();
		}
		return new NoOpTeacherApprovedMailAdapter();
	}

	@Bean
	TeacherModerationUseCase teacherModerationUseCase(
			ActiveRolePort activeRolePort,
			TeacherModerationPort teacherModerationPort,
			TeacherApprovedMailPort teacherApprovedMailPort,
			AuthUserAccountPort authUserAccountPort) {
		return new TeacherModerationService(
				activeRolePort, teacherModerationPort, teacherApprovedMailPort, authUserAccountPort);
	}

	/**
	 * Nom du compte connecté, lu par l'en-tête parent ou professeur.
	 * Même nom quand la personne change d'espace : c'est le compte, pas le rôle.
	 */
	@Bean
	ConnectedAccountUseCase connectedAccountUseCase(AuthUserAccountPort authUserAccountPort) {
		return new ConnectedAccountService(authUserAccountPort);
	}

	/**
	 * Profil professeur (SCRUM-159) — nom du compte et écoles, si le référentiel en a.
	 */
	@Bean
	TeacherProfileUseCase teacherProfileUseCase(
			TeacherProfilePort teacherProfilePort,
			SchoolCatalogPort schoolCatalogPort,
			TeacherSubjectPort teacherSubjectPort) {
		return new TeacherProfileService(teacherProfilePort, schoolCatalogPort, teacherSubjectPort);
	}

	/**
	 * Matières du professeur (SCRUM-161, SCRUM-172, SCRUM-189) — créer, retirer,
	 * proposer un transfert ; le collègue accepte.
	 */
	@Bean
	TeacherSubjectUseCase teacherSubjectUseCase(
			TeacherSubjectPort teacherSubjectPort,
			TeacherProfilePort teacherProfilePort,
			SchoolCatalogPort schoolCatalogPort,
			ActiveRolePort activeRolePort) {
		return new TeacherSubjectService(
				teacherSubjectPort,
				teacherProfilePort,
				schoolCatalogPort,
				activeRolePort);
	}

	/**
	 * Choix multiple dans une matière (SCRUM-174). La liste des formes ne contient
	 * que ce qu'on sait enregistrer. L'écran élève n'est pas ici.
	 */
	@Bean
	QuestionFormHandler qcmQuestionForm() {
		return new QcmQuestionForm();
	}

	@Bean
	QuestionFormHandler memoryQuestionForm() {
		return new MemoryQuestionForm();
	}

	@Bean
	QuestionFormHandler orderQuestionForm() {
		return new OrderQuestionForm();
	}

	@Bean
	TeacherQuestionUseCase teacherQuestionUseCase(
			TeacherSubjectPort teacherSubjectPort,
			TeacherQuestionPort teacherQuestionPort, List<QuestionFormHandler> handlers) {
		return new TeacherQuestionService(teacherSubjectPort, teacherQuestionPort, new QuestionFormRegistry(handlers));
	}

	/**
	 * Élèves des classes du professeur (SCRUM-160) — lecture seule, sans PIN ni parent.
	 */
	@Bean
	TeacherStudentsUseCase teacherStudentsUseCase(
			EnrolledChildPort enrolledChildPort,
			TeacherProfilePort teacherProfilePort,
			SchoolCatalogPort schoolCatalogPort) {
		return new TeacherStudentsService(enrolledChildPort, teacherProfilePort, schoolCatalogPort);
	}

	/**
	 * Enfants du parent (SCRUM-156) — fiche, classe, école facultative, PIN hashé.
	 */
	@Bean
	ParentChildrenUseCase parentChildrenUseCase(
			ChildPort childPort,
			SchoolCatalogPort schoolCatalogPort,
			PasswordEncoder passwordEncoder,
			ChildSubjectPort childSubjectPort,
			ChildSubjectService childSubjectService) {
		return new ParentChildrenService(
				childPort, schoolCatalogPort, passwordEncoder, childSubjectPort, childSubjectService);
	}

	/**
	 * Matières d'un enfant (SCRUM-162) — le parent ajoute ou retire un lien.
	 * La matière du professeur n'est pas modifiée.
	 */
	@Bean
	ChildSubjectService childSubjectService(
			ChildPort childPort,
			ChildSubjectPort childSubjectPort,
			SchoolCatalogPort schoolCatalogPort) {
		return new ChildSubjectService(childPort, childSubjectPort, schoolCatalogPort);
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
				new com.appv4.auth.application.config.PasswordResetSettings(
						passwordResetProperties.getTokenTtl(),
						passwordResetProperties.getFrontendResetUrl()));
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
	 *   <li>Rate-limit auth publics (issue #59) : filtre avant JWT → 429</li>
	 * </ul>
	 */
	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtAuthenticationFilter jwtAuthenticationFilter,
			AuthRateLimitFilter authRateLimitFilter,
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
				.addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
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

package com.appv4.auth.infrastructure.config;

import com.appv4.auth.application.service.question.QuestionFormHandler;
import com.appv4.auth.application.service.question.QuestionFormRegistry;
import com.appv4.auth.application.service.question.QcmQuestionForm;
import com.appv4.auth.application.service.question.MemoryQuestionForm;
import com.appv4.auth.application.service.question.OrderQuestionForm;
import com.appv4.auth.application.service.question.CaseVideQuestionForm;
import com.appv4.auth.application.service.question.ClassificationQuestionForm;
import com.appv4.auth.application.service.question.ClickImageQuestionForm;
import com.appv4.auth.application.service.question.PuzzleQuestionForm;
import com.appv4.auth.application.service.question.LinkQuestionForm;
import com.appv4.auth.application.service.question.ReponseLibreQuestionForm;
import com.appv4.auth.application.service.question.SimonQuestionForm;
import com.appv4.auth.application.service.question.TexteQuestionForm;
import com.appv4.auth.application.service.question.TrueFalseQuestionForm;
import java.time.Clock;
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
import com.appv4.auth.application.port.in.RecoverIdentifierUseCase;
import com.appv4.auth.application.port.in.RecoveryPromptUseCase;
import com.appv4.auth.application.port.in.AppHoursUseCase;
import com.appv4.auth.application.port.in.AskChildHumeurUseCase;
import com.appv4.auth.application.port.in.HumeurLibraryUseCase;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.in.LoginWithAppleUseCase;
import com.appv4.auth.application.port.in.ResumeAppleOAuthCallbackUseCase;
import com.appv4.auth.application.port.in.LoginChildUseCase;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase;
import com.appv4.auth.application.port.in.ReadChildFeaturedQuestionsUseCase;
import com.appv4.auth.application.port.in.ReadParentHumeursUseCase;
import com.appv4.auth.application.port.in.SejourUseCase;
import com.appv4.auth.application.port.in.ReadChildLearningSubjectsUseCase;
import com.appv4.auth.application.port.in.ReadChildProfileUseCase;
import com.appv4.auth.application.port.in.ReadChildProgressUseCase;
import com.appv4.auth.application.port.in.ReadChildSubjectBoardUseCase;
import com.appv4.auth.application.port.in.ReadChildTodayUseCase;
import com.appv4.auth.application.port.in.LoginWithEmailUseCase;
import com.appv4.auth.application.port.in.LoginWithFacebookUseCase;
import com.appv4.auth.application.port.in.LoginWithGoogleUseCase;
import com.appv4.auth.application.port.in.ParentChildrenUseCase;
import com.appv4.auth.application.port.in.ResetPasswordUseCase;
import com.appv4.auth.application.port.in.SignupWithEmailUseCase;
import com.appv4.auth.application.port.in.SwitchActiveRoleUseCase;
import com.appv4.auth.application.port.in.TeacherModerationUseCase;
import com.appv4.auth.application.port.in.ParentProfileUseCase;
import com.appv4.auth.application.port.in.TeacherProfileUseCase;
import com.appv4.auth.application.port.in.TeacherStudentsUseCase;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase;
import com.appv4.auth.application.port.in.TeacherSubjectUseCase;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.AppleOAuthClient;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.RecoveryAccountPort;
import com.appv4.auth.application.port.out.RecoveryPromptPort;
import com.appv4.auth.application.port.out.ChildDailyMissionPort;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort;
import com.appv4.auth.application.port.out.ChildProgressPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildSimonChallengePort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.in.SuggestChildInterestsUseCase;
import com.appv4.auth.application.port.out.ChildInterestSuggestionPort;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.EnrolledChildPort;
import com.appv4.auth.application.port.out.FacebookOAuthClient;
import com.appv4.auth.application.port.out.GoogleOAuthClient;
import com.appv4.auth.application.port.out.AppHoursPort;
import com.appv4.auth.application.port.out.HumeurAnswerPort;
import com.appv4.auth.application.port.out.HumeurChoicePort;
import com.appv4.auth.application.port.out.PasswordResetMailPort;
import com.appv4.auth.application.port.out.PasswordResetTokenPort;
import com.appv4.auth.application.port.out.SchoolCatalogPort;
import com.appv4.auth.application.port.out.SejourPort;
import com.appv4.auth.application.port.out.SejourQuestionPort;
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
import com.appv4.auth.application.service.RecoverIdentifierService;
import com.appv4.auth.application.service.RecoveryPromptService;
import com.appv4.auth.application.service.IssueJwtService;
import com.appv4.auth.application.service.LoginWithAppleService;
import com.appv4.auth.application.service.ResumeAppleOAuthCallbackService;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.service.LoginChildService;
import com.appv4.auth.application.service.ReadChildProfileService;
import com.appv4.auth.application.service.LoginWithEmailService;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtChildSessionAdapter;
import com.appv4.auth.application.service.LoginWithFacebookService;
import com.appv4.auth.application.service.LoginWithGoogleService;
import com.appv4.auth.application.service.ChildSubjectService;
import com.appv4.auth.application.service.ParentChildrenService;
import com.appv4.auth.application.service.SuggestChildInterestsService;
import com.appv4.auth.application.service.ResetPasswordService;
import com.appv4.auth.application.service.SignupWithEmailService;
import com.appv4.auth.application.service.SwitchActiveRoleService;
import com.appv4.auth.application.service.AppHoursService;
import com.appv4.auth.application.service.AskChildHumeurService;
import com.appv4.auth.application.service.ReadParentHumeursService;
import com.appv4.auth.application.service.SejourService;
import com.appv4.auth.application.service.HumeurLibraryService;
import com.appv4.auth.application.service.TeacherModerationService;
import com.appv4.auth.application.service.ParentProfileService;
import com.appv4.auth.application.service.TeacherProfileService;
import com.appv4.auth.application.service.TeacherStudentsService;
import com.appv4.auth.application.service.TeacherQuestionService;
import com.appv4.auth.application.service.ChildDailyMissionService;
import com.appv4.auth.application.service.ChildLessonMembership;
import com.appv4.auth.application.service.ReadChildSubjectBoardService;
import com.appv4.auth.application.service.ChildQuestionPlayService;
import com.appv4.auth.application.service.ReadChildFeaturedQuestionsService;
import com.appv4.auth.application.service.ReadChildLearningSubjectsService;
import com.appv4.auth.application.port.in.ReadChildAvatarUseCase;
import com.appv4.auth.application.port.in.SaveChildAvatarStyleUseCase;
import com.appv4.auth.application.port.out.ChildAvatarPort;
import com.appv4.auth.application.port.in.ReadChildBadgesUseCase;
import com.appv4.auth.application.port.out.ChildBadgePort;
import com.appv4.auth.application.service.ChildBadgeProgress;
import com.appv4.auth.application.service.CharacterAvatarCatalog;
import com.appv4.auth.application.service.ReadChildAvatarService;
import com.appv4.auth.application.service.ReadChildBadgesService;
import com.appv4.auth.application.service.ReadChildProgressService;
import com.appv4.auth.application.service.UpdateChildProgressAfterAttemptService;
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
	 * Login enfant prénom + code (BACK-13). Le jeton n'authentifie pas {@code /api/int}.
	 */
	@Bean
	ChildSessionTokenPort childSessionTokenPort(JwtProperties jwtProperties) {
		return new JjwtChildSessionAdapter(jwtProperties);
	}

	@Bean
	LoginChildUseCase loginChildUseCase(
			ChildLoginPort childLoginPort,
			PasswordEncoder passwordEncoder,
			ChildSessionTokenPort childSessionTokenPort,
			java.time.Clock clock,
			AppHoursPort appHoursPort) {
		return new LoginChildService(
				childLoginPort, passwordEncoder, childSessionTokenPort, clock, appHoursPort);
	}

	/**
	 * Profil enfant pour l'accueil learning (BACK-14).
	 */
	@Bean
	ReadChildProfileUseCase readChildProfileUseCase(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort) {
		return new ReadChildProfileService(childLoginPort, childSessionTokenPort);
	}

	/**
	 * Matières et questions de l'enfant connecté (BACK-15).
	 */
	@Bean
	ReadChildLearningSubjectsUseCase readChildLearningSubjectsUseCase(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildSubjectPort childSubjectPort,
			TeacherQuestionPort teacherQuestionPort,
			QuestionFormRegistry questionFormRegistry,
			ChildQuestionAttemptPort childQuestionAttemptPort) {
		return new ReadChildLearningSubjectsService(
				childLoginPort, childSessionTokenPort, childSubjectPort, teacherQuestionPort, questionFormRegistry,
				childQuestionAttemptPort);
	}

	/**
	 * Questions d'accueil de l'enfant connecté (BACK-20).
	 */
	@Bean
	ReadChildFeaturedQuestionsUseCase readChildFeaturedQuestionsUseCase(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildSubjectPort childSubjectPort,
			TeacherQuestionPort teacherQuestionPort,
			ChildQuestionAttemptPort childQuestionAttemptPort) {
		return new ReadChildFeaturedQuestionsService(
				childLoginPort, childSessionTokenPort, childSubjectPort, teacherQuestionPort,
				childQuestionAttemptPort);
	}

	/**
	 * Horloge système. La progression et la Nuit la lisent en Europe/Paris.
	 * Le profil test est fixé à 16 h le jour courant, pour qu'une suite lancée
	 * la nuit puisse encore entrer dans l'app enfant.
	 */
	@Bean
	java.time.Clock clock(org.springframework.core.env.Environment environment) {
		if (environment.matchesProfiles("test")) {
			java.time.ZoneId paris = com.appv4.auth.application.service.ReadChildProgressService.PROGRESS_ZONE;
			java.time.LocalDate today = java.time.LocalDate.now(paris);
			return java.time.Clock.fixed(today.atTime(16, 0).atZone(paris).toInstant(), paris);
		}
		return java.time.Clock.systemUTC();
	}

	/**
	 * Mise à jour XP / streak après une tentative (BACK-17).
	 */
	@Bean
	UpdateChildProgressAfterAttemptService updateChildProgressAfterAttemptService(
			ChildProgressPort childProgressPort, java.time.Clock clock) {
		return new UpdateChildProgressAfterAttemptService(childProgressPort, clock);
	}

	/**
	 * Lecture XP / streak / palier / semaine (BACK-17).
	 */
	/**
	 * Lecture des badges enfant (BACK-19). Le même objet reçoit aussi les essais du jeu.
	 */
	@Bean
	ReadChildBadgesService readChildBadgesService(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildQuestionAttemptPort childQuestionAttemptPort,
			ChildBadgePort childBadgePort,
			ChildSubjectPort childSubjectPort,
			TeacherQuestionPort teacherQuestionPort,
			ChildDailyMissionPort childDailyMissionPort,
			java.time.Clock clock) {
		return new ReadChildBadgesService(
				childLoginPort, childSessionTokenPort, childQuestionAttemptPort, childBadgePort,
				childSubjectPort, teacherQuestionPort, childDailyMissionPort, clock);
	}

	@Bean
	ReadChildBadgesUseCase readChildBadgesUseCase(ReadChildBadgesService readChildBadgesService) {
		return readChildBadgesService;
	}

	@Bean
	ChildBadgeProgress childBadgeProgress(ReadChildBadgesService readChildBadgesService) {
		return readChildBadgesService;
	}

	@Bean
	CharacterAvatarCatalog characterAvatarCatalog() {
		return CharacterAvatarCatalog.load();
	}

	/**
	 * Personnage de l'enfant (BACK-48). La lecture ne recalcule pas l'XP.
	 */
	@Bean
	ReadChildAvatarService readChildAvatarService(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildProgressPort childProgressPort,
			ChildAvatarPort childAvatarPort,
			CharacterAvatarCatalog characterAvatarCatalog) {
		return new ReadChildAvatarService(
				childLoginPort,
				childSessionTokenPort,
				childProgressPort,
				childAvatarPort,
				characterAvatarCatalog);
	}

	@Bean
	ReadChildAvatarUseCase readChildAvatarUseCase(ReadChildAvatarService readChildAvatarService) {
		return readChildAvatarService;
	}

	@Bean
	SaveChildAvatarStyleUseCase saveChildAvatarStyleUseCase(ReadChildAvatarService readChildAvatarService) {
		return readChildAvatarService;
	}

	@Bean
	ReadChildProgressUseCase readChildProgressUseCase(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildProgressPort childProgressPort,
			java.time.Clock clock) {
		return new ReadChildProgressService(
				childLoginPort, childSessionTokenPort, childProgressPort, clock);
	}

	@Bean
	ChildLessonMembership childLessonMembership(
			TeacherQuestionPort teacherQuestionPort,
			ChildQuestionAttemptPort childQuestionAttemptPort,
			QuestionFormRegistry questionFormRegistry) {
		return new ChildLessonMembership(teacherQuestionPort, childQuestionAttemptPort, questionFormRegistry);
	}

	/**
	 * Board matière enfant (BACK-33).
	 */
	@Bean
	ReadChildSubjectBoardUseCase readChildSubjectBoardUseCase(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildSubjectPort childSubjectPort,
			ChildSubjectBoardPort childSubjectBoardPort,
			ChildLessonMembership childLessonMembership) {
		return new ReadChildSubjectBoardService(
				childLoginPort, childSessionTokenPort, childSubjectPort, childSubjectBoardPort,
				childLessonMembership);
	}

	/**
	 * Mission et plan du jour (BACK-18). Créée à la première lecture ou tentative utile.
	 */
	@Bean
	ChildDailyMissionService childDailyMissionService(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildSubjectPort childSubjectPort,
			TeacherQuestionPort teacherQuestionPort,
			ChildDailyMissionPort childDailyMissionPort,
			java.time.Clock clock) {
		return new ChildDailyMissionService(
				childLoginPort,
				childSessionTokenPort,
				childSubjectPort,
				teacherQuestionPort,
				childDailyMissionPort,
				clock);
	}

	/**
	 * Lecture HTTP de la mission du jour (BACK-18).
	 */
	@Bean
	ReadChildTodayUseCase readChildTodayUseCase(ChildDailyMissionService childDailyMissionService) {
		return childDailyMissionService;
	}

	/**
	 * Jeu enfant : question sans solution + tentative / score (BACK-16), puis progress (BACK-17)
	 * et mission du jour (BACK-18).
	 */
	@Bean
	ChildQuestionPlayUseCase childQuestionPlayUseCase(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildSubjectPort childSubjectPort,
			TeacherQuestionPort teacherQuestionPort,
			ChildQuestionAttemptPort childQuestionAttemptPort,
			ChildSimonChallengePort childSimonChallengePort,
			UpdateChildProgressAfterAttemptService updateChildProgressAfterAttemptService,
			ChildDailyMissionService childDailyMissionService,
			ChildBadgeProgress childBadgeProgress,
			ChildLessonMembership childLessonMembership) {
		return new ChildQuestionPlayService(
				childLoginPort,
				childSessionTokenPort,
				childSubjectPort,
				teacherQuestionPort,
				childQuestionAttemptPort,
				childSimonChallengePort,
				updateChildProgressAfterAttemptService,
				childDailyMissionService,
				childBadgeProgress,
				childLessonMembership);
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

	/**
	 * Bibliothèque des humeurs (BACK-68).
	 * Le rôle admin est relu en base. Le front n'invente pas de choix.
	 */
	@Bean
	HumeurLibraryUseCase humeurLibraryUseCase(ActiveRolePort activeRolePort, HumeurChoicePort humeurChoicePort) {
		return new HumeurLibraryService(activeRolePort, humeurChoicePort);
	}

	/**
	 * Horaires de l'app enfant.
	 * Seul l'admin les change. Le login et le séjour lisent la même ligne.
	 */
	@Bean
	AppHoursUseCase appHoursUseCase(ActiveRolePort activeRolePort, AppHoursPort appHoursPort) {
		return new AppHoursService(activeRolePort, appHoursPort);
	}

	/**
	 * Demande d'humeur (BACK-69).
	 * La tendance se lit sur les essais déjà enregistrés. L'horloge est celle du bean.
	 */
	@Bean
	AskChildHumeurUseCase askChildHumeurUseCase(
			com.appv4.auth.application.port.out.ChildSessionTokenPort sessions,
			com.appv4.auth.application.port.out.ChildLoginPort children,
			com.appv4.auth.application.port.out.ChildQuestionAttemptPort attempts,
			HumeurChoicePort humeurChoicePort,
			HumeurAnswerPort humeurAnswerPort,
			java.time.Clock clock) {
		return new AskChildHumeurService(sessions, children, attempts, humeurChoicePort, humeurAnswerPort, clock);
	}

	/**
	 * Lecture parent des humeurs (BACK-71).
	 * Les libellés de date et d'heure sont calculés ici, en heure de Bruxelles.
	 */
	@Bean
	ReadParentHumeursUseCase readParentHumeursUseCase(ChildPort childPort, HumeurAnswerPort humeurAnswerPort) {
		return new ReadParentHumeursService(childPort, humeurAnswerPort);
	}

	/**
	 * Séjour de l'enfant (BACK-38).
	 *
	 * <p><b>Business :</b> les signaux confirment le temps. Le parent lit les libellés.
	 * Une question jouée compte sur cette question.
	 * <p><b>Technique :</b> l'horloge du bean fixe l'instant. Le port garde les intervalles.
	 * Un autre port lit l'énoncé et le nom de matière.
	 * <p><b>Métier / flow :</b> routes enfant, parent et professeur → ce cas d'usage.
	 * Le professeur passe par le contrôle de classe déjà utilisé pour ses élèves.
	 */
	@Bean
	SejourUseCase sejourUseCase(
			com.appv4.auth.application.port.out.ChildSessionTokenPort sessions,
			com.appv4.auth.application.port.out.ChildLoginPort logins,
			ChildPort childPort,
			SejourPort sejourPort,
			SejourQuestionPort questions,
			java.time.Clock clock,
			TeacherStudentsUseCase teacherStudentsUseCase,
			com.appv4.auth.application.port.out.ChildSharePort childSharePort,
			AppHoursPort appHoursPort) {
		return new SejourService(
				sessions, logins, childPort, sejourPort, clock, questions, teacherStudentsUseCase, childSharePort,
				appHoursPort);
	}

	/**
	 * Suivi parent : créneau, minutes, tendance, formes ratées (BACK-40).
	 *
	 * <p><b>Business :</b> le parent qui a la fiche lit ces quatre blocs, rien d'autre.
	 * <p><b>Technique :</b> l'horloge du bean fixe les fenêtres de 7×24 h.
	 * <p><b>Métier / flow :</b> {@code GET /api/int/parent/children/{childId}/progress}.
	 */
	@Bean
	com.appv4.auth.application.port.in.ReadParentProgressUseCase readParentProgressUseCase(
			ChildPort childPort,
			com.appv4.auth.application.port.out.ChildProgressPort childProgressPort,
			com.appv4.auth.application.port.out.ChildQuestionAttemptPort attempts,
			SejourUseCase sejourUseCase,
			AppHoursPort appHoursPort,
			com.appv4.auth.application.service.question.QuestionFormRegistry questionFormRegistry,
			java.time.Clock clock) {
		return new com.appv4.auth.application.service.ReadParentProgressService(
				childPort, childProgressPort, attempts, sejourUseCase, appHoursPort, questionFormRegistry, clock);
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
	/**
	 * Sollicitation du téléphone et du mail de contact.
	 *
	 * <p><b>Business :</b> une seule fois, si l'une des deux infos manque.
	 * <p><b>Technique :</b> marque sur le compte. La fiche ne la pose pas.
	 * <p><b>Métier / flow :</b> bean du port in lu par /auth/me et écrit par /auth/recovery-prompt.
	 */
	@Bean
	RecoveryPromptUseCase recoveryPromptUseCase(RecoveryPromptPort recoveryPromptPort) {
		return new RecoveryPromptService(recoveryPromptPort);
	}

	@Bean
	ConnectedAccountUseCase connectedAccountUseCase(
			AuthUserAccountPort authUserAccountPort,
			RecoveryPromptUseCase recoveryPromptUseCase) {
		return new ConnectedAccountService(authUserAccountPort, recoveryPromptUseCase);
	}

	@Bean
	com.appv4.auth.application.service.SchoolCatalogSearchService schoolCatalogSearchService(SchoolCatalogPort catalog) {
		return new com.appv4.auth.application.service.SchoolCatalogSearchService(catalog);
	}

	/**
	 * Profil professeur (SCRUM-159) — nom du compte et écoles, si le référentiel en a.
	 */
	@Bean
	TeacherProfileUseCase teacherProfileUseCase(
			TeacherProfilePort teacherProfilePort,
			SchoolCatalogPort schoolCatalogPort,
			TeacherSubjectPort teacherSubjectPort,
			EnrolledChildPort enrolledChildPort,
			ChildSubjectPort childSubjectPort) {
		return new TeacherProfileService(
				teacherProfilePort,
				schoolCatalogPort,
				teacherSubjectPort,
				enrolledChildPort,
				childSubjectPort);
	}

	/**
	 * Fiche parent (SCRUM-200) — même compte que le professeur, sans toucher aux écoles.
	 */
	@Bean
	ParentProfileUseCase parentProfileUseCase(TeacherProfilePort teacherProfilePort) {
		return new ParentProfileService(teacherProfilePort);
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
			ActiveRolePort activeRolePort,
			TeacherQuestionPort teacherQuestionPort,
			ChildSubjectPort childSubjectPort) {
		return new TeacherSubjectService(
				teacherSubjectPort,
				teacherProfilePort,
				schoolCatalogPort,
				activeRolePort,
				teacherQuestionPort,
				childSubjectPort);
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
	QuestionFormHandler trueFalseQuestionForm() {
		return new TrueFalseQuestionForm();
	}

	@Bean
	QuestionFormHandler linkQuestionForm() {
		return new LinkQuestionForm();
	}

	@Bean
	QuestionFormHandler caseVideQuestionForm() {
		return new CaseVideQuestionForm();
	}

	/**
	 * Classement dans une matière : cartes à ranger dans N colonnes.
	 * L'élève joue via Learn ; le score reste calculé côté serveur.
	 */
	@Bean
	QuestionFormHandler classificationQuestionForm() {
		return new ClassificationQuestionForm();
	}

	/**
	 * Réponse libre dans une matière (SCRUM-198). L'élève écrira dans un seul champ.
	 * Cet écran n'a pas encore d'adresse : la forme et la comparaison sont déjà là.
	 */
	@Bean
	QuestionFormHandler texteQuestionForm() {
		return new TexteQuestionForm();
	}

	/**
	 * Saisie lettre par lettre dans une matière (SCRUM-182). Une case par lettre.
	 * Valider ignore les majuscules et les espaces au bord, pas les accents.
	 */
	@Bean
	QuestionFormHandler reponseLibreQuestionForm() {
		return new ReponseLibreQuestionForm();
	}

	/**
	 * Image à cliquer dans une matière (SCRUM-181). Le professeur dessine les zones.
	 * L'élève n'a pas encore d'écran.
	 */
	@Bean
	QuestionFormHandler clickImageQuestionForm() {
		return new ClickImageQuestionForm();
	}

	/**
	 * Puzzle dans une matière (SCRUM-183). Le professeur réutilise les formes du clic
	 * pour découper l'image. L'élève n'a pas encore d'écran.
	 */
	@Bean
	QuestionFormHandler puzzleQuestionForm() {
		return new PuzzleQuestionForm();
	}

	/**
	 * Simon dans une matière (ADMIN-25, BACK-54). Le professeur pose les éléments d'un genre
	 * et la suite ; l'élève la regarde, la refait, et le serveur la note.
	 */
	@Bean
	QuestionFormHandler simonQuestionForm() {
		return new SimonQuestionForm();
	}

	@Bean
	QuestionFormRegistry questionFormRegistry(List<QuestionFormHandler> handlers) {
		return new QuestionFormRegistry(handlers);
	}

	@Bean
	TeacherQuestionUseCase teacherQuestionUseCase(
			TeacherSubjectPort teacherSubjectPort,
			TeacherQuestionPort teacherQuestionPort,
			ChildSubjectPort childSubjectPort,
			QuestionFormRegistry questionFormRegistry) {
		return new TeacherQuestionService(
				teacherSubjectPort, teacherQuestionPort, childSubjectPort, questionFormRegistry);
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
	 * Fiches des enfants d'un parent.
	 *
	 * <p><b>Business :</b> le parent ajoute, corrige et relit ses enfants, avec leurs centres d'intérêt.
	 * <p><b>Technique :</b> le moment d'une copie nouvelle est l'horloge UTC.
	 * <p><b>Métier / flow :</b> bean du port in {@code /api/int/parent/children}.
	 */
	@Bean
	ParentChildrenUseCase parentChildrenUseCase(
			ChildPort childPort,
			SchoolCatalogPort schoolCatalogPort,
			PasswordEncoder passwordEncoder,
			ChildSubjectPort childSubjectPort,
			ChildSubjectService childSubjectService,
			ChildAvatarPort childAvatarPort,
			CharacterAvatarCatalog characterAvatarCatalog,
			com.appv4.auth.application.port.out.ChildSharePort childSharePort,
			com.appv4.auth.application.port.out.AuthUserAccountPort authUserAccountPort) {
		return new ParentChildrenService(
				childPort,
				schoolCatalogPort,
				passwordEncoder,
				childSubjectPort,
				childSubjectService,
				childAvatarPort,
				characterAvatarCatalog,
				Clock.systemUTC(),
				childSharePort,
				authUserAccountPort);
	}

	/**
	 * Suggestions de centres d'intérêt.
	 *
	 * <p><b>Business :</b> le parent voit un mot déjà écrit par deux familles, sans savoir lesquelles.
	 * <p><b>Technique :</b> lecture des copies encore actives et affichées.
	 * <p><b>Métier / flow :</b> bean du port in {@code GET /api/int/parent/interest-suggestions}.
	 */
	@Bean
	SuggestChildInterestsUseCase suggestChildInterestsUseCase(ChildInterestSuggestionPort suggestions) {
		return new SuggestChildInterestsService(suggestions);
	}

	/**
	 * Matières d'un enfant (SCRUM-162) — le parent ajoute ou retire un lien.
	 * La matière du professeur n'est pas modifiée.
	 */
	@Bean
	ChildSubjectService childSubjectService(
			ChildPort childPort,
			ChildSubjectPort childSubjectPort,
			SchoolCatalogPort schoolCatalogPort,
			TeacherQuestionPort teacherQuestionPort) {
		return new ChildSubjectService(childPort, childSubjectPort, schoolCatalogPort, teacherQuestionPort);
	}

	/**
	 * Forgot-password (SCRUM-108) — jeton hashé + lien (log local).
	 */
	/**
	 * Récupération de l'identifiant.
	 *
	 * <p><b>Business :</b> un seul compte dans la bande montre le masque de l'email de connexion.
	 * <p><b>Technique :</b> lecture des comptes, aucune écriture.
	 * <p><b>Métier / flow :</b> bean du port in {@code POST /api/int/auth/recover-identifier}.
	 */
	@Bean
	RecoverIdentifierUseCase recoverIdentifierUseCase(RecoveryAccountPort recoveryAccountPort) {
		return new RecoverIdentifierService(recoveryAccountPort);
	}

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
	 *   <li>{@code /api/ext/**} public (learning). Le login enfant est limité en débit ;
	 *       son jeton ne remplit pas le SecurityContext adulte</li>
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
			org.springframework.core.env.Environment environment,
			JwtAuthenticationFilter jwtAuthenticationFilter,
			AuthRateLimitFilter authRateLimitFilter,
			CorsConfigurationSource corsConfigurationSource) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> {
					if (environment.matchesProfiles("local & !prod & !production & !test")) {
						auth.requestMatchers(HttpMethod.GET, "/api/int/auth/dev-profiles").permitAll();
					}
					auth
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
						.requestMatchers("/api/int/auth/recover-identifier").permitAll()
						.requestMatchers("/api/int/auth/reset-password").permitAll()
						.requestMatchers("/api/int/**").authenticated()
						.anyRequest().denyAll();
				})
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

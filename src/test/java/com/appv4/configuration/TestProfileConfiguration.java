package com.appv4.configuration;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.mockito.quality.Strictness;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.SessionState;
import com.appv4.auth.application.port.out.PasswordResetTokenPort;
import com.appv4.auth.application.port.out.SchoolCatalogPort;
import com.appv4.auth.application.port.out.SignupRolePort;
import com.appv4.auth.application.port.out.TeacherModerationPort;
import com.appv4.hello.application.port.in.GetHelloMessageUseCase;
import com.appv4.hello.application.port.out.HelloMessageRepository;
import com.appv4.hello.application.service.GetHelloMessageService;
import com.appv4.hello.domain.model.HelloAudience;
import com.appv4.hello.domain.model.HelloMessage;

/**
 * Remplace la base pendant les tests web.
 *
 * <p><b>Business :</b> un test de contrôleur vérifie le HTTP, pas le contenu de Postgres.
 * <p><b>Technique :</b> profil {@code test}. Faux ports + transaction vide, donc pas de DataSource.
 * Session JWT « ouverte » par défaut (actif, sv=0, espaces parent+teacher) pour le filtre #49.
 * <p><b>Métier / flow :</b> les {@code @MockBean} des tests remplacent ces faux si le scénario en a besoin.
 */
@Configuration
@Profile("test")
public class TestProfileConfiguration {

	@Bean
	PlatformTransactionManager transactionManager() {
		return new NoDatabaseTransactionManager();
	}

	@Bean
	AuthUserAccountPort authUserAccountPort() {
		AuthUserAccountPort port = mock(AuthUserAccountPort.class, withSettings().strictness(Strictness.LENIENT));
		when(port.findSessionState(anyLong())).thenReturn(Optional.of(new SessionState(true, 0)));
		return port;
	}

	@Bean
	ActiveRolePort activeRolePort() {
		ActiveRolePort port = mock(ActiveRolePort.class, withSettings().strictness(Strictness.LENIENT));
		when(port.findSpaceRoles(anyLong())).thenReturn(List.of("parent", "teacher"));
		when(port.findLastActiveRole(anyLong())).thenReturn(Optional.empty());
		when(port.hasAdminRole(anyLong())).thenReturn(false);
		when(port.hasPendingTeacherRequest(anyLong())).thenReturn(false);
		when(port.teacherRequestStatus(anyLong())).thenReturn("none");
		when(port.loadAuthzSnapshot(anyLong())).thenReturn(
				new ActiveRolePort.AuthzSnapshot(List.of("parent", "teacher"), false, null, false));
		return port;
	}

	@Bean
	PasswordResetTokenPort passwordResetTokenPort() {
		return mock(PasswordResetTokenPort.class);
	}

	@Bean
	SignupRolePort signupRolePort() {
		SignupRolePort port = mock(SignupRolePort.class, withSettings().strictness(Strictness.LENIENT));
		when(port.hasCompletedInitialChoice(anyLong())).thenReturn(true);
		return port;
	}

	@Bean
	TeacherModerationPort teacherModerationPort() {
		return mock(TeacherModerationPort.class);
	}

	@Bean
	TeacherProfilePort teacherProfilePort() {
		return mock(TeacherProfilePort.class, withSettings().strictness(Strictness.LENIENT));
	}

	/**
	 * Les tests web ne démarrent pas Postgres. L'annuaire réel est vide ici ;
	 * les trois écoles d'exemple sont vérifiées sur la base locale.
	 */
	@Bean
	SchoolCatalogPort schoolCatalogPort() {
		return () -> List.of();
	}

	@Bean
	HelloMessageRepository helloMessageRepository() {
		return audience -> Optional.of(new HelloMessage(textFor(audience)));
	}

	@Bean
	GetHelloMessageUseCase getHelloMessageUseCase(HelloMessageRepository helloMessageRepository) {
		return new GetHelloMessageService(helloMessageRepository);
	}

	private static String textFor(HelloAudience audience) {
		if (audience == HelloAudience.ADMIN) {
			return "admin-helloworld";
		}
		return "hello learning";
	}

	/**
	 * Transaction qui ne parle à aucune base.
	 * Les services métier sont {@code @Transactional} : sans ce bean le contexte de test ne démarre pas.
	 */
	private static final class NoDatabaseTransactionManager implements PlatformTransactionManager {

		@Override
		public TransactionStatus getTransaction(TransactionDefinition definition) {
			return new SimpleTransactionStatus(true);
		}

		@Override
		public void commit(TransactionStatus status) {
			if (status.isRollbackOnly()) {
				rollback(status);
			}
		}

		@Override
		public void rollback(TransactionStatus status) {
			if (status.isCompleted()) {
				return;
			}
		}
	}
}

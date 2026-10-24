package com.appv4.configuration;

import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.PasswordResetTokenPort;
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
		return mock(AuthUserAccountPort.class);
	}

	@Bean
	ActiveRolePort activeRolePort() {
		return mock(ActiveRolePort.class);
	}

	@Bean
	PasswordResetTokenPort passwordResetTokenPort() {
		return mock(PasswordResetTokenPort.class);
	}

	@Bean
	SignupRolePort signupRolePort() {
		return mock(SignupRolePort.class);
	}

	@Bean
	TeacherModerationPort teacherModerationPort() {
		return mock(TeacherModerationPort.class);
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

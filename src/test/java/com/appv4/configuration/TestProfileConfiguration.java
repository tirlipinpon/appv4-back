package com.appv4.configuration;

import static org.mockito.ArgumentMatchers.any;
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
import com.appv4.auth.application.port.out.ChildDailyMissionPort;
import com.appv4.auth.application.port.out.ChildAvatarPort;
import com.appv4.auth.application.port.out.ChildProgressPort;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildSimonChallengePort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildInterestSuggestionPort;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildSharePort;
import com.appv4.auth.application.port.out.EnrolledChildPort;
import com.appv4.auth.application.port.out.HumeurAnswerPort;
import com.appv4.auth.application.port.out.HumeurChoicePort;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.SessionState;
import com.appv4.auth.application.port.out.PasswordResetTokenPort;
import com.appv4.auth.application.port.out.SchoolCatalogPort;
import com.appv4.auth.application.port.out.SignupRolePort;
import com.appv4.auth.application.port.out.TeacherModerationPort;

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

	/** Bibliothèque des humeurs (BACK-68). Pas de Postgres dans les tests web. */
	@Bean
	HumeurChoicePort humeurChoicePort() {
		return mock(HumeurChoicePort.class, withSettings().strictness(Strictness.LENIENT));
	}

	/** Réponses d'humeur (BACK-69). Pas de Postgres dans les tests web. */
	@Bean
	HumeurAnswerPort humeurAnswerPort() {
		return mock(HumeurAnswerPort.class, withSettings().strictness(Strictness.LENIENT));
	}

	@Bean
	TeacherProfilePort teacherProfilePort() {
		return mock(TeacherProfilePort.class, withSettings().strictness(Strictness.LENIENT));
	}

	@Bean
	TeacherSubjectPort teacherSubjectPort() {
		return mock(TeacherSubjectPort.class, withSettings().strictness(Strictness.LENIENT));
	}

	/** Choix multiples d'une matière (SCRUM-174). Pas de Postgres dans les tests web. */
	@Bean
	TeacherQuestionPort teacherQuestionPort() {
		return mock(TeacherQuestionPort.class, withSettings().strictness(Strictness.LENIENT));
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
	ChildPort childPort() {
		return mock(ChildPort.class, withSettings().strictness(Strictness.LENIENT));
	}

	@Bean
	ChildSharePort childSharePort() {
		return mock(ChildSharePort.class, withSettings().strictness(Strictness.LENIENT));
	}

	/** Suggestions de centres d'intérêt (BACK-73). Pas de Postgres dans les tests web. */
	@Bean
	ChildInterestSuggestionPort childInterestSuggestionPort() {
		return mock(ChildInterestSuggestionPort.class, withSettings().strictness(Strictness.LENIENT));
	}

	/** Login de l'app enfant (BACK-13). Pas de Postgres dans les tests web. */
	@Bean
	ChildLoginPort childLoginPort() {
		return mock(ChildLoginPort.class, withSettings().strictness(Strictness.LENIENT));
	}

	/** Lecture des élèves d'une classe (SCRUM-160). Pas de Postgres dans les tests web. */
	@Bean
	EnrolledChildPort enrolledChildPort() {
		return mock(EnrolledChildPort.class, withSettings().strictness(Strictness.LENIENT));
	}

	@Bean
	ChildSubjectPort childSubjectPort() {
		return mock(ChildSubjectPort.class, withSettings().strictness(Strictness.LENIENT));
	}

	/** Tentatives enfant (BACK-16). Pas de Postgres dans les tests web. */
	@Bean
	ChildQuestionAttemptPort childQuestionAttemptPort() {
		ChildQuestionAttemptPort port = mock(ChildQuestionAttemptPort.class, withSettings().strictness(Strictness.LENIENT));
		when(port.allMarks(anyLong())).thenReturn(java.util.Map.of());
		return port;
	}

	/** Niveaux de badges (BACK-19). Pas de Postgres dans les tests web. */
	@Bean
	com.appv4.auth.application.port.out.ChildBadgePort childBadgePort() {
		com.appv4.auth.application.port.out.ChildBadgePort port = mock(
				com.appv4.auth.application.port.out.ChildBadgePort.class,
				withSettings().strictness(Strictness.LENIENT));
		when(port.load(anyLong())).thenReturn(java.util.Map.of());
		return port;
	}

	/** Suite Simon tirée (BACK-55). Pas de Postgres dans les tests web. */
	@Bean
	ChildSimonChallengePort childSimonChallengePort() {
		return mock(ChildSimonChallengePort.class, withSettings().strictness(Strictness.LENIENT));
	}

	/** Mission du jour (BACK-18). Pas de Postgres dans les tests web. */
	@Bean
	ChildDailyMissionPort childDailyMissionPort() {
		ChildDailyMissionPort port = mock(ChildDailyMissionPort.class, withSettings().strictness(Strictness.LENIENT));
		when(port.findByChildAndDate(anyLong(), any())).thenReturn(Optional.empty());
		return port;
	}

	/** Board matière (BACK-33). Pas de Postgres dans les tests web. */
	@Bean
	ChildSubjectBoardPort childSubjectBoardPort() {
		ChildSubjectBoardPort port = mock(ChildSubjectBoardPort.class, withSettings().strictness(Strictness.LENIENT));
		when(port.findMeta(anyLong())).thenReturn(Optional.empty());
		when(port.topicsOf(anyLong())).thenReturn(List.of());
		when(port.lessonsOfTopics(any())).thenReturn(List.of());
		when(port.questionsOfLessons(any())).thenReturn(List.of());
		when(port.passedQuestionIds(anyLong(), anyLong())).thenReturn(java.util.Set.of());
		when(port.xpEarned(anyLong(), anyLong())).thenReturn(0);
		return port;
	}

	/** Avatar enfant (BACK-66). Pas de Postgres dans les tests web. */
	@Bean
	ChildAvatarPort childAvatarPort() {
		ChildAvatarPort port = mock(ChildAvatarPort.class, withSettings().strictness(Strictness.LENIENT));
		when(port.findByChildId(anyLong())).thenReturn(Optional.empty());
		return port;
	}

	/** Progression enfant (BACK-17). Pas de Postgres dans les tests web. */
	@Bean
	ChildProgressPort childProgressPort() {
		ChildProgressPort port = mock(ChildProgressPort.class, withSettings().strictness(Strictness.LENIENT));
		when(port.findByChildId(anyLong())).thenReturn(Optional.empty());
		when(port.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		return port;
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

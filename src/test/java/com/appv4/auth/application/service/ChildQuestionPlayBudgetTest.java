package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.Submission;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.Text;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.NewAttempt;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.PageClaim;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredAttempt;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort.ChildSession;
import com.appv4.auth.application.port.out.ChildSimonChallengePort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredFreeText;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredGamification;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.result.ChildAttemptResultView;

/**
 * Barème d'une page de jeu : le budget meurt avec la page, les points partent seulement si la réponse est juste.
 */
class ChildQuestionPlayBudgetTest {

	private final TeacherQuestionPort questions = mock(TeacherQuestionPort.class);
	private final MemoryAttempts attempts = new MemoryAttempts();
	private final UpdateChildProgressAfterAttemptService progress = mock(UpdateChildProgressAfterAttemptService.class);
	private ChildQuestionPlayService game;

	@BeforeEach
	void page() {
		var child = mock(ChildLoginPort.class);
		when(child.findActiveById(7L)).thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "hash")));
		var session = mock(ChildSessionTokenPort.class);
		when(session.read("lea")).thenReturn(Optional.of(new ChildSession(7L)));
		var subjects = mock(ChildSubjectPort.class);
		when(subjects.linksOf(7L)).thenReturn(List.of(new StoredLink(4L, false)));
		when(subjects.visibleByIds(List.of(4L)))
				.thenReturn(List.of(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math")));
		when(questions.findInSubject(4L, 19L)).thenReturn(Optional.of(
				new StoredQuestion(19L, 4L, "texte", "Capitale de la France", new StoredFreeText("Paris"))));
		game = new ChildQuestionPlayService(
				child, session, subjects, questions, attempts, mock(ChildSimonChallengePort.class),
				progress, mock(ChildDailyMissionService.class));
	}

	@Test
	void uneBonneReponseSurLeBaremeParDefautDonneDixPointsEtFermeLaPage() {
		ChildAttemptResultView result = game.submit("lea", 4L, 19L, answer("Paris", UUID.randomUUID()));

		assertThat(result.passed()).isTrue();
		assertThat(result.scorePercent()).isEqualTo(100);
		assertThat(result.attemptRank()).isEqualTo(1);
		assertThat(result.attemptsRemaining()).isZero();
		assertThat(result.pointsAwarded()).isEqualTo(10);
		assertThat(result.replayAvailable()).isFalse();
		verify(progress).apply(7L, 100);
	}

	@Test
	void laMemePageRefuseLeSecondEssaiQuandLePlafondEstUn() {
		UUID page = UUID.randomUUID();
		game.submit("lea", 4L, 19L, answer("Paris", page));

		assertThatThrownBy(() -> game.submit("lea", 4L, 19L, answer("Paris", page)))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ChildQuestionPlayService.ATTEMPTS_EXHAUSTED);
		assertThat(attempts.saved).hasSize(1);
	}

	@Test
	void unBaremePersonnaliseDonneVingtPuisDixPoints() {
		when(questions.gamificationOf(19L)).thenReturn(new StoredGamification(2, "modere", true, false, List.of(20, 10)));
		UUID page = UUID.randomUUID();

		ChildAttemptResultView first = game.submit("lea", 4L, 19L, answer("Paris", page));
		ChildAttemptResultView second = game.submit("lea", 4L, 19L, answer("Paris", page));

		assertThat(first.pointsAwarded()).isEqualTo(20);
		assertThat(first.attemptsRemaining()).isEqualTo(1);
		assertThat(first.replayAvailable()).isTrue();
		assertThat(second.attemptRank()).isEqualTo(2);
		assertThat(second.pointsAwarded()).isEqualTo(10);
		assertThat(second.attemptsRemaining()).isZero();
		assertThat(second.replayAvailable()).isFalse();
	}

	@Test
	void uneMauvaiseReponseNeDonneAucunPointMaisConsommeLEssai() {
		when(questions.gamificationOf(19L)).thenReturn(new StoredGamification(2, "facile", false, false, List.of()));
		UUID page = UUID.randomUUID();

		ChildAttemptResultView missed = game.submit("lea", 4L, 19L, answer("Lyon", page));
		ChildAttemptResultView found = game.submit("lea", 4L, 19L, answer("Paris", page));

		assertThat(missed.passed()).isFalse();
		assertThat(missed.pointsAwarded()).isZero();
		assertThat(missed.attemptRank()).isEqualTo(1);
		assertThat(missed.attemptsRemaining()).isEqualTo(1);
		assertThat(found.pointsAwarded()).isEqualTo(5);
		assertThat(found.attemptRank()).isEqualTo(2);
		verify(progress).apply(7L, 0);
	}

	@Test
	void laMemeSoumissionRenduLeMemeVerdictSansPrendreUnNouvelEssai() {
		when(questions.gamificationOf(19L)).thenReturn(new StoredGamification(2, "modere", true, false, List.of(20, 10)));
		UUID page = UUID.randomUUID();
		UUID submission = UUID.randomUUID();
		Submission again = new Submission("texte", new Text("Paris"), submission, null, page);

		ChildAttemptResultView first = game.submit("lea", 4L, 19L, again);
		ChildAttemptResultView replay = game.submit("lea", 4L, 19L, again);

		assertThat(replay).isEqualTo(first);
		assertThat(attempts.saved).hasSize(1);
	}

	@Test
	void uneNouvellePageRetrouveUnBudgetPlein() {
		UUID finished = UUID.randomUUID();
		game.submit("lea", 4L, 19L, answer("Paris", finished));

		ChildAttemptResultView fresh = game.submit("lea", 4L, 19L, answer("Paris", UUID.randomUUID()));

		assertThat(fresh.attemptRank()).isEqualTo(1);
		assertThat(fresh.pointsAwarded()).isEqualTo(10);
		assertThat(fresh.attemptsRemaining()).isZero();
	}

	private static Submission answer(String text, UUID playId) {
		return new Submission("texte", new Text(text), UUID.randomUUID(), null, playId);
	}

	/** Réserve les essais d'une page. Le verrou SQL vit dans l'adaptateur. */
	private static final class MemoryAttempts implements ChildQuestionAttemptPort {
		private final ConcurrentHashMap<UUID, Integer> used = new ConcurrentHashMap<>();
		private final ConcurrentHashMap<UUID, StoredAttempt> bySubmission = new ConcurrentHashMap<>();
		private final List<StoredAttempt> saved = new java.util.ArrayList<>();
		private long sequence = 40;

		@Override
		public StoredAttempt insert(NewAttempt attempt) {
			sequence++;
			StoredAttempt stored = new StoredAttempt(
					sequence, attempt.childId(), attempt.subjectId(), attempt.questionId(), attempt.formCode(),
					attempt.scorePercent(), attempt.passed(), attempt.attemptRank(), attempt.attemptsRemaining(),
					attempt.pointsAwarded());
			saved.add(stored);
			if (attempt.submissionId() != null) {
				bySubmission.put(attempt.submissionId(), stored);
			}
			return stored;
		}

		@Override
		public Optional<StoredAttempt> lockAndFind(long childId, UUID submissionId) {
			return Optional.ofNullable(bySubmission.get(submissionId));
		}

		@Override
		public int attemptsUsed(UUID playId, long childId, long subjectId, long questionId) {
			return used.getOrDefault(playId, 0);
		}

		@Override
		public Optional<PageClaim> claim(UUID playId, long childId, long subjectId, long questionId, int maxAttempts) {
			synchronized (used) {
				int current = used.getOrDefault(playId, 0);
				if (current >= maxAttempts) {
					return Optional.empty();
				}
				int rank = current + 1;
				used.put(playId, rank);
				return Optional.of(new PageClaim(rank, maxAttempts - rank));
			}
		}
	}
}

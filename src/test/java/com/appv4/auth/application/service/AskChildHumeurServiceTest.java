package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.AskChildHumeurUseCase.Prompt;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AttemptTrace;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort.ChildSession;
import com.appv4.auth.application.port.out.HumeurAnswerPort;
import com.appv4.auth.application.port.out.HumeurAnswerPort.StoredAnswer;
import com.appv4.auth.application.port.out.HumeurChoicePort;
import com.appv4.auth.application.port.out.HumeurChoicePort.HumeurChoice;
import com.appv4.auth.application.port.out.QuestionMark;

class AskChildHumeurServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");
	private static final Duration WEEK = Duration.ofHours(7 * 24);

	private final ChildSessionTokenPort sessions = mock(ChildSessionTokenPort.class);
	private final ChildLoginPort children = mock(ChildLoginPort.class);
	private final ChildQuestionAttemptPort attempts = mock(ChildQuestionAttemptPort.class);
	private final MemoryChoices choices = new MemoryChoices();
	private final MemoryAnswers answers = new MemoryAnswers();
	private final MoveableClock clock = new MoveableClock(NOW);
	private final List<AttemptTrace> traces = new ArrayList<>();
	private AskChildHumeurService humeur;

	@BeforeEach
	void akira() {
		when(sessions.read("akira")).thenReturn(Optional.of(new ChildSession(7L)));
		when(sessions.read("nao")).thenReturn(Optional.of(new ChildSession(8L)));
		when(sessions.read("")).thenReturn(Optional.empty());
		when(sessions.issuedAt(anyString())).thenReturn(Optional.empty());
		when(children.findActiveById(7L)).thenReturn(Optional.of(new ChildLoginCandidate(7L, "Akira", "hash")));
		when(children.findActiveById(8L)).thenReturn(Optional.of(new ChildLoginCandidate(8L, "Nao", "hash")));
		when(attempts.traces(7L)).thenAnswer(invocation -> List.copyOf(traces));
		when(attempts.traces(8L)).thenReturn(List.of());
		humeur = new AskChildHumeurService(sessions, children, attempts, choices, answers, clock);
	}

	@Test
	void uneConnexionMontreLeMessageMemeSiLeDelaiNestPasFini() {
		choices.insert(null, "calme");
		when(sessions.issuedAt("akira")).thenReturn(Optional.of(NOW));
		clock.plus(Duration.ofHours(-2));
		humeur.answer("akira", choices.list().get(0).id(), null);
		clock.plus(Duration.ofHours(2));
		presqueBaisseAt(NOW);

		Prompt prompt = humeur.ask("akira");

		assertThat(prompt.ask()).isTrue();
		assertThat(prompt.message()).isEqualTo("Comment tu te sens ?");
		assertThat(prompt.choices()).extracting(HumeurChoice::word).containsExactly("calme");
	}

	@Test
	void apresLaReponseDeCetteConnexionLeDelaiReprend() {
		choices.insert(null, "calme");
		when(sessions.issuedAt("akira")).thenReturn(Optional.of(NOW.minus(Duration.ofMinutes(5))));
		humeur.answer("akira", choices.list().get(0).id(), null);
		presqueBaisseAt(NOW.plus(Duration.ofMinutes(1)));
		clock.plus(Duration.ofMinutes(1));

		assertThat(humeur.ask("akira").ask()).isFalse();
	}

	@Test
	void sansEssaiEtSansReponseOnDemandeLeChoix() {
		choices.insert(null, "calme");

		Prompt prompt = humeur.ask("akira");

		assertThat(prompt.ask()).isTrue();
		assertThat(prompt.choices()).extracting(HumeurChoice::word).containsExactly("calme");
	}

	@Test
	void bibliothequeVideNeDemandePas() {
		assertThat(humeur.ask("akira").ask()).isFalse();
		assertThat(humeur.ask("akira").choices()).isEmpty();
	}

	@Test
	void uneTendanceDejaLaNeRetardePasLaPremiereDemande() {
		choices.insert(null, "calme");
		hausse();

		assertThat(humeur.ask("akira").ask()).isTrue();
	}

	@Test
	void baisseDeHuitPointsRedemandeAVingtQuatreHeures() {
		choices.insert(null, "calme");
		humeur.answer("akira", choices.list().get(0).id(), null);
		baisseAt(NOW.plus(Duration.ofHours(24)));

		clock.plus(Duration.ofHours(24).minusSeconds(1));
		assertThat(humeur.ask("akira").ask()).isFalse();
		clock.plus(Duration.ofSeconds(1));
		assertThat(humeur.ask("akira").ask()).isTrue();
	}

	@Test
	void hausseDeHuitPointsAttendSoixanteDouzeHeures() {
		choices.insert(null, "calme");
		humeur.answer("akira", choices.list().get(0).id(), null);
		hausseAt(NOW.plus(Duration.ofHours(72)));

		clock.plus(Duration.ofHours(72).minusSeconds(1));
		assertThat(humeur.ask("akira").ask()).isFalse();
		clock.plus(Duration.ofSeconds(1));
		assertThat(humeur.ask("akira").ask()).isTrue();
	}

	@Test
	void stableAttendCentHeuresEtSeptVirguleNeufResteStable() {
		choices.insert(null, "calme");
		humeur.answer("akira", choices.list().get(0).id(), null);
		presqueBaisseAt(NOW.plus(Duration.ofHours(100)));

		clock.plus(Duration.ofHours(100).minusSeconds(1));
		assertThat(humeur.ask("akira").ask()).isFalse();
		clock.plus(Duration.ofSeconds(1));
		assertThat(humeur.ask("akira").ask()).isTrue();
	}

	@Test
	void sansTendanceOnRedemandeAVingtQuatreHeures() {
		choices.insert(null, "calme");
		humeur.answer("akira", choices.list().get(0).id(), null);
		recentOnly(NOW.plus(Duration.ofHours(24)));

		clock.plus(Duration.ofHours(24).minusSeconds(1));
		assertThat(humeur.ask("akira").ask()).isFalse();
		clock.plus(Duration.ofSeconds(1));
		assertThat(humeur.ask("akira").ask()).isTrue();
	}

	@Test
	void uneBaisseApresUnePeriodeStableRaccourcitLeDelai() {
		choices.insert(null, "calme");
		humeur.answer("akira", choices.list().get(0).id(), null);
		baisseAt(NOW.plus(Duration.ofHours(24)));

		clock.plus(Duration.ofHours(24));
		assertThat(humeur.ask("akira").ask()).isTrue();
	}

	@Test
	void lessaiSurLaCoupeCompteDansLaFenetreAncienne() {
		choices.insert(null, "calme");
		humeur.answer("akira", choices.list().get(0).id(), null);
		Instant check = NOW.plus(Duration.ofHours(30));
		traces.clear();
		essai(check.minus(WEEK), false);
		essai(check.minus(Duration.ofHours(1)), true);

		clock.plus(Duration.ofHours(30));
		assertThat(humeur.ask("akira").ask()).isFalse();
	}

	@Test
	void laReponseCopieLeMotEtLIconeDuMoment() {
		HumeurChoice choice = choices.insert("🙂", "joie");

		StoredAnswer saved = humeur.answer("akira", choice.id(), null);
		choices.update(choice.id(), null, "fatigue");
		clock.plus(Duration.ofHours(24));

		assertThat(saved.icon()).isEqualTo("🙂");
		assertThat(saved.word()).isEqualTo("joie");
		assertThat(answers.latest(7L).orElseThrow().word()).isEqualTo("joie");
		assertThat(humeur.ask("akira").choices()).extracting(HumeurChoice::word).containsExactly("fatigue");
	}

	@Test
	void unIdentifiantInconnuNEcritRien() {
		assertThatThrownBy(() -> humeur.answer("akira", 99L, null))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.BAD_REQUEST);
		assertThat(answers.latest(7L)).isEmpty();
	}

	@Test
	void unCorpsNiChoixNiFermetureNEcritRien() {
		choices.insert(null, "calme");

		assertThatThrownBy(() -> humeur.answer("akira", choices.list().get(0).id(), true))
				.isInstanceOf(AuthDomainException.class);
		assertThatThrownBy(() -> humeur.answer("akira", null, false))
				.isInstanceOf(AuthDomainException.class);
		assertThat(answers.latest(7L)).isEmpty();
	}

	@Test
	void laFermetureNePointeAucunChoix() {
		StoredAnswer saved = humeur.answer("akira", null, true);

		assertThat(saved.closed()).isTrue();
		assertThat(saved.choiceId()).isNull();
		assertThat(saved.icon()).isNull();
		assertThat(saved.word()).isNull();
	}

	@Test
	void leSecondEnvoiFixeLeDelai() {
		choices.insert(null, "calme");
		hausseAt(NOW.plus(Duration.ofHours(110)));
		humeur.answer("akira", choices.list().get(0).id(), null);
		clock.plus(Duration.ofHours(80));
		humeur.answer("akira", null, true);
		clock.plus(Duration.ofHours(30));

		assertThat(humeur.ask("akira").ask()).isFalse();
	}

	@Test
	void unAutreEnfantNeLitPasLaReponse() {
		choices.insert(null, "calme");
		humeur.answer("akira", choices.list().get(0).id(), null);

		assertThat(humeur.ask("nao").ask()).isTrue();
		assertThat(answers.latest(8L)).isEmpty();
	}

	@Test
	void unJetonAbsentNEcritRien() {
		assertThatThrownBy(() -> humeur.answer("", null, true))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AskChildHumeurService.INVALID_SESSION_MESSAGE);
		assertThat(answers.latest(7L)).isEmpty();
	}

	private void hausse() {
		hausseAt(clock.instant());
	}

	private void baisseAt(Instant check) {
		traces.clear();
		fill(check.minus(WEEK).minus(Duration.ofHours(1)), 50, 0);
		fill(check.minus(Duration.ofHours(1)), 46, 4);
	}

	private void hausseAt(Instant check) {
		traces.clear();
		fill(check.minus(WEEK).minus(Duration.ofHours(1)), 46, 4);
		fill(check.minus(Duration.ofHours(1)), 50, 0);
	}

	private void presqueBaisseAt(Instant check) {
		traces.clear();
		fill(check.minus(WEEK).minus(Duration.ofHours(1)), 1000, 0);
		fill(check.minus(Duration.ofHours(1)), 921, 79);
	}

	private void recentOnly(Instant check) {
		traces.clear();
		fill(check.minus(Duration.ofHours(1)), 1, 1);
	}

	private void fill(Instant at, int passed, int failed) {
		for (int i = 0; i < passed; i++) {
			essai(at, true);
		}
		for (int i = 0; i < failed; i++) {
			essai(at, false);
		}
	}

	private void essai(Instant at, boolean passed) {
		traces.add(new AttemptTrace(1L, at, QuestionMark.NONE, passed, true));
	}

	private static final class MoveableClock extends Clock {
		private Instant now;

		private MoveableClock(Instant now) {
			this.now = now;
		}

		private void plus(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}
	}

	private static final class MemoryChoices implements HumeurChoicePort {
		private final List<HumeurChoice> rows = new ArrayList<>();
		private long nextId = 1L;

		@Override
		public List<HumeurChoice> list() {
			return rows.stream().sorted(Comparator.comparingLong(HumeurChoice::id)).toList();
		}

		@Override
		public HumeurChoice insert(String icon, String word) {
			HumeurChoice created = new HumeurChoice(nextId++, icon, word);
			rows.add(created);
			return created;
		}

		@Override
		public Optional<HumeurChoice> find(long id) {
			return rows.stream().filter(row -> row.id() == id).findFirst();
		}

		@Override
		public HumeurChoice update(long id, String icon, String word) {
			for (int index = 0; index < rows.size(); index++) {
				if (rows.get(index).id() == id) {
					HumeurChoice updated = new HumeurChoice(id, icon, word);
					rows.set(index, updated);
					return updated;
				}
			}
			throw new IllegalStateException("choix absent");
		}

		@Override
		public void delete(long id) {
			rows.removeIf(row -> row.id() == id);
		}
	}

	private static final class MemoryAnswers implements HumeurAnswerPort {
		private final List<StoredAnswer> rows = new ArrayList<>();
		private long nextId = 1L;

		@Override
		public Optional<StoredAnswer> latest(long childId) {
			return rows.stream()
					.filter(row -> row.childId() == childId)
					.max(Comparator.comparing(StoredAnswer::answeredAt).thenComparingLong(StoredAnswer::id));
		}

		@Override
		public StoredAnswer insert(
				long childId, Long choiceId, String icon, String word, boolean closed, Instant answeredAt) {
			StoredAnswer created = new StoredAnswer(nextId++, childId, choiceId, icon, word, closed, answeredAt);
			rows.add(created);
			return created;
		}
	}
}

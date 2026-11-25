package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ReadParentHumeursUseCase.ParentChildHumeur;
import com.appv4.auth.application.port.out.ChildPort;
import com.appv4.auth.application.port.out.ChildPort.ChildSaved;
import com.appv4.auth.application.port.out.HumeurAnswerPort;
import com.appv4.auth.application.port.out.HumeurAnswerPort.StoredAnswer;

class ReadParentHumeursServiceTest {

	private static final Instant ANSWERED = Instant.parse("2026-11-22T08:51:00Z");

	private final ChildPort children = mock(ChildPort.class);
	private final MemoryAnswers answers = new MemoryAnswers();
	private ReadParentHumeursService humeurs;

	@BeforeEach
	void parent() {
		when(children.findByParent(1L)).thenReturn(List.of(
				child(2L, "Akira", true),
				child(5L, "Nao", false)));
		when(children.findByParent(9L)).thenReturn(List.of());
		humeurs = new ReadParentHumeursService(children, answers);
	}

	@Test
	void unChoixMontreLaCopieLaDateEtLHeure() {
		answers.insert(2L, 4L, "🙂", "joie", false, ANSWERED);

		List<ParentChildHumeur> rows = humeurs.list(1L, "parent");

		assertThat(rows).extracting(ParentChildHumeur::firstName).containsExactly("Akira", "Nao");
		assertThat(rows.get(0).humeurs()).hasSize(1);
		assertThat(rows.get(0).humeurs().get(0).icon()).isEqualTo("🙂");
		assertThat(rows.get(0).humeurs().get(0).word()).isEqualTo("joie");
		assertThat(rows.get(0).humeurs().get(0).label()).isNull();
		assertThat(rows.get(0).humeurs().get(0).dateLabel()).isEqualTo("22 novembre 2026");
		assertThat(rows.get(0).humeurs().get(0).timeLabel()).isEqualTo("09:51");
		assertThat(rows.get(1).humeurs()).isEmpty();
	}

	@Test
	void uneFermetureNaNiIconeNiMot() {
		answers.insert(2L, null, null, null, true, ANSWERED);

		ParentChildHumeur row = humeurs.list(1L, "parent").get(0);

		assertThat(row.humeurs().get(0).label()).isEqualTo("Fermée sans réponse");
		assertThat(row.humeurs().get(0).icon()).isNull();
		assertThat(row.humeurs().get(0).word()).isNull();
		assertThat(row.humeurs().get(0).dateLabel()).isEqualTo("22 novembre 2026");
		assertThat(row.humeurs().get(0).timeLabel()).isEqualTo("09:51");
	}

	@Test
	void unMotSeulNeGagnePasDIcone() {
		answers.insert(2L, 4L, null, "calme", false, ANSWERED);

		assertThat(humeurs.list(1L, "parent").get(0).humeurs().get(0).icon()).isNull();
		assertThat(humeurs.list(1L, "parent").get(0).humeurs().get(0).word()).isEqualTo("calme");
	}

	@Test
	void unEnfantDesactiveResteDansLaListe() {
		assertThat(humeurs.list(1L, "parent").get(1).firstName()).isEqualTo("Nao");
	}

	@Test
	void unAutreParentNeVoitPasCesEnfants() {
		assertThat(humeurs.list(9L, "parent")).isEmpty();
	}

	@Test
	void unJetonEnfantEstRefuse() {
		assertThatThrownBy(() -> humeurs.list(1L, "child"))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.FORBIDDEN);
	}

	private static ChildSaved child(long id, String firstName, boolean active) {
		return new ChildSaved(id, 1L, firstName, "Iris", "F", 8, "FR", "CE1", null, "cat", false, active);
	}

	@Test
	void lesAnciennesHumeursRestentDeLaPlusRecenteALaPlusAncienne() {
		answers.insert(2L, 1L, "😴", "fatigue", false, ANSWERED.minusSeconds(3600));
		answers.insert(2L, null, null, null, true, ANSWERED);

		var rows = humeurs.list(1L, "parent").get(0).humeurs();

		assertThat(rows).hasSize(2);
		assertThat(rows.get(0).label()).isEqualTo("Fermée sans réponse");
		assertThat(rows.get(0).timeLabel()).isEqualTo("09:51");
		assertThat(rows.get(1).word()).isEqualTo("fatigue");
		assertThat(rows.get(1).icon()).isEqualTo("😴");
		assertThat(rows.get(1).timeLabel()).isEqualTo("08:51");
	}

	private static final class MemoryAnswers implements HumeurAnswerPort {
		private final List<StoredAnswer> rows = new ArrayList<>();
		private long nextId = 1L;

		@Override
		public List<StoredAnswer> history(long childId) {
			return rows.stream()
					.filter(row -> row.childId() == childId)
					.sorted(Comparator.comparing(StoredAnswer::answeredAt)
							.thenComparingLong(StoredAnswer::id)
							.reversed())
					.toList();
		}

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

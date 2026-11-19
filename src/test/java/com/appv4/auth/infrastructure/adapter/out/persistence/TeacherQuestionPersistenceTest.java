package com.appv4.auth.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.appv4.auth.application.port.out.TeacherQuestionPort.CardDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ChoiceDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ChoicesDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClassificationDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ColumnDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.FreeTextDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.GapWordDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.GapsDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ItemDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ItemsDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.LinkDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.LinksDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PairDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PairsDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.SimonDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.SimonElementDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StatementDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StatementsDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

/** Tests sur une base PostgreSQL jetable : toutes les migrations Flyway et le vrai SQL Hibernate. */
@DataJpaTest(showSql = false, properties = {
		"spring.jpa.hibernate.ddl-auto=validate",
		"spring.jpa.properties.hibernate.generate_statistics=true",
		"logging.level.org.hibernate.stat=OFF",
		"logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("persistence-test")
@Import({TeacherQuestionPersistenceAdapter.class, ChoiceQuestionContentStore.class,
		PairQuestionContentStore.class, ItemQuestionContentStore.class,
		StatementQuestionContentStore.class, LinkQuestionContentStore.class,
		ClassificationQuestionContentStore.class,
		CaseVideQuestionContentStore.class, TexteQuestionContentStore.class,
		LetterQuestionContentStore.class, SimonQuestionContentStore.class})
@EnabledIfEnvironmentVariable(named = "APPV4_PERSISTENCE_TEST_URL", matches = ".+")
class TeacherQuestionPersistenceTest {

	@Autowired
	private TeacherQuestionPersistenceAdapter adapter;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private long subjectId;

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> System.getenv("APPV4_PERSISTENCE_TEST_URL"));
		registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_USER", "appv4"));
		registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_PASSWORD", "appv4"));
	}

	@BeforeEach
	void seedSubject() {
		long userId = jdbc.queryForObject("""
				insert into back_users (email, display_name) values (?, 'Question test') returning id
				""", Long.class, UUID.randomUUID() + "@example.test");
		subjectId = jdbc.queryForObject("""
				insert into back_teacher_subjects (user_id, school_id, grade_label, name, name_key)
				values (?, 1, 'CE1', ?, ?) returning id
				""", Long.class, userId, "Test", UUID.randomUUID().toString());
		adapter.ensureForm(subjectId, "qcm", "Choix multiples", 1);
		adapter.ensureForm(subjectId, "memory", "Memory", 2);
		adapter.ensureForm(subjectId, "order", "Ordre", 3);
	}

	@Test
	void hundredMixedQuestionsTakeFourSelectsAndKeepTheirOwnOrderedContent() {
		for (int index = 0; index < 100; index++) {
			insert(index % 3);
		}
		Statistics statistics = resetStatistics();
		List<StoredQuestion> rows = adapter.findBySubject(subjectId, 0, 1000);
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
		assertThat(rows).hasSize(100);
		assertThat(rows).extracting(StoredQuestion::id).isSorted();
		assertThat(rows.get(0).choices()).extracting(choice -> choice.label()).containsExactly("B", "A");
		assertThat(rows.get(0).pairs()).isEmpty();
		assertThat(rows.get(1).pairs()).extracting(pair -> pair.questionSide()).containsExactly("Deux", "Un");
		assertThat(rows.get(2).items()).extracting(item -> item.label()).containsExactly("Début", "Fin");
	}

	@Test
	void cursorLoadsStableBoundedPages() {
		for (int index = 0; index < 7; index++) {
			insert(0);
		}
		var first = adapter.findBySubject(subjectId, 0, 3);
		var second = adapter.findBySubject(subjectId, first.get(2).id(), 3);
		var third = adapter.findBySubject(subjectId, second.get(2).id(), 3);
		assertThat(first).hasSize(3);
		assertThat(second).hasSize(3);
		assertThat(third).hasSize(1);
		assertThat(java.util.stream.Stream.of(first, second, third).flatMap(List::stream)
				.map(StoredQuestion::id).toList()).doesNotHaveDuplicates().isSorted();
	}

	@Test
	void hundredQcmQuestionsTakeOnlyTwoSelects() {
		for (int index = 0; index < 100; index++) {
			insert(0);
		}
		Statistics statistics = resetStatistics();
		assertThat(adapter.findBySubject(subjectId, 0, 1000)).hasSize(100);
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
	}

	@Test
	void emptySubjectAndQuestionFromAnotherSubjectDoNotLoadContent() {
		StoredQuestion question = insert(0);
		Statistics statistics = resetStatistics();
		assertThat(adapter.findBySubject(-1, 0, 1000)).isEmpty();
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
		statistics.clear();
		assertThat(adapter.findInSubject(-1, question.id())).isEmpty();
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
	}

	@Test
	void replacementPreservesOtherQuestionsAndReplacesOnlyItsOwnContent() {
		StoredQuestion qcm = insert(0);
		StoredQuestion memory = insert(1);
		StoredQuestion order = insert(2);
		adapter.replace(qcm.id(), "Corrigé", new ChoicesDraft(List.of(
				new ChoiceDraft("C", true), new ChoiceDraft("D", false))));
		adapter.replace(memory.id(), "Paires", new PairsDraft(List.of(new PairDraft("Trois", "3"))));
		adapter.replace(order.id(), "Ordre", new ItemsDraft(List.of(new ItemDraft("Milieu"))));
		resetStatistics();
		List<StoredQuestion> rows = adapter.findBySubject(subjectId, 0, 1000);
		assertThat(rows.get(0).prompt()).isEqualTo("Corrigé");
		assertThat(rows.get(0).choices()).extracting(choice -> choice.label()).containsExactly("C", "D");
		assertThat(rows.get(0).choices()).extracting(choice -> choice.id())
				.doesNotContain(qcm.choices().get(0).id(), qcm.choices().get(1).id());
		assertThat(rows.get(1).pairs()).extracting(pair -> pair.answerSide()).containsExactly("3");
		assertThat(rows.get(2).items()).extracting(item -> item.label()).containsExactly("Milieu");
	}

	@Test
	void formRegistrationIsIdempotent() {
		adapter.ensureForm(subjectId, "qcm", "Choix multiples", 1);
		adapter.ensureForm(subjectId, "qcm", "Choix multiples", 1);
		assertThat(jdbc.queryForObject("""
				select count(*) from back_subject_question_forms where subject_id = ? and form_code = 'qcm'
				""", Long.class, subjectId)).isEqualTo(1);
		assertThat(insert(0).id()).isPositive();
	}

	@Test
	void databaseRejectsDuplicatePositions() {
		StoredQuestion question = insert(0);
		assertThatThrownBy(() -> jdbc.update("""
				insert into back_subject_question_choices (question_id, label, correct, sort_order)
				values (?, 'X', false, 0)
				""", question.id())).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void trueFalseReplacementKeepsEachSide() {
		adapter.ensureForm(subjectId, "vrai_faux", "Vrai ou faux", 4);
		StoredQuestion saved = adapter.insert(subjectId, "vrai_faux", "", new StatementsDraft(List.of(
				new StatementDraft("2 + 2 = 4", true),
				new StatementDraft("3 + 3 = 5", false))));
		adapter.replace(saved.id(), "", new StatementsDraft(List.of(
				new StatementDraft("2 + 2 = 4", true),
				new StatementDraft("3 + 3 = 6", false))));
		StoredQuestion loaded = adapter.findInSubject(subjectId, saved.id()).orElseThrow();
		assertThat(loaded.statements()).extracting(row -> row.label() + ":" + row.truthful())
				.containsExactly("2 + 2 = 4:true", "3 + 3 = 6:false");
	}

	@Test
	void linksReplacementKeepsEachCouple() {
		adapter.ensureForm(subjectId, "liens", "Liens", 5);
		StoredQuestion saved = adapter.insert(subjectId, "liens", "Relie.", new LinksDraft(List.of(
				new LinkDraft("Pomme", "Fruit"),
				new LinkDraft("Chien", "Animal"))));
		adapter.replace(saved.id(), "Relie.", new LinksDraft(List.of(
				new LinkDraft("Pomme", "Fruit"),
				new LinkDraft("Voiture", "Véhicule"))));
		StoredQuestion loaded = adapter.findInSubject(subjectId, saved.id()).orElseThrow();
		assertThat(loaded.links()).extracting(row -> row.leftLabel() + "/" + row.rightLabel())
				.containsExactly("Pomme/Fruit", "Voiture/Véhicule");
	}

	@Test
	void classificationReplacementKeepsColumnsAndCards() {
		adapter.ensureForm(subjectId, "classement", "Classement", 7);
		StoredQuestion saved = adapter.insert(
				subjectId,
				"classement",
				"Classe.",
				new ClassificationDraft(
						List.of(new ColumnDraft("A"), new ColumnDraft("B")),
						List.of(new CardDraft("un", 0), new CardDraft("deux", 1))));
		adapter.replace(
				saved.id(),
				"Classe.",
				new ClassificationDraft(
						List.of(new ColumnDraft("Oui"), new ColumnDraft("Non"), new ColumnDraft("Parfois")),
						List.of(
								new CardDraft("un", 0),
								new CardDraft("deux", 1),
								new CardDraft("trois", 2))));
		StoredQuestion loaded = adapter.findInSubject(subjectId, saved.id()).orElseThrow();
		assertThat(loaded.columns()).extracting(row -> row.label()).containsExactly("Oui", "Non", "Parfois");
		assertThat(loaded.cards()).extracting(row -> row.label()).containsExactly("un", "deux", "trois");
		assertThat(loaded.cards().get(0).correctColumnId()).isEqualTo(loaded.columns().get(0).id());
		assertThat(loaded.cards().get(2).correctColumnId()).isEqualTo(loaded.columns().get(2).id());
	}

	@Test
	void gapsReplacementKeepsTheSentenceAndEachWord() {
		adapter.ensureForm(subjectId, "case_vide", "Case vide", 6);
		String sentence = "Le chat mange une ___ dans le jardin.";
		StoredQuestion saved = adapter.insert(subjectId, "case_vide", "Complète.", new GapsDraft(
				sentence,
				List.of(new GapWordDraft("pomme")),
				List.of(new GapWordDraft("poire"), new GapWordDraft("voiture"))));
		adapter.replace(saved.id(), "Complète.", new GapsDraft(
				sentence,
				List.of(new GapWordDraft("pomme")),
				List.of(new GapWordDraft("banane"), new GapWordDraft("voiture"))));
		StoredQuestion loaded = adapter.findInSubject(subjectId, saved.id()).orElseThrow();
		assertThat(loaded.sentence()).isEqualTo(sentence);
		assertThat(loaded.answers()).extracting(row -> row.label()).containsExactly("pomme");
		assertThat(loaded.decoys()).extracting(row -> row.label()).containsExactly("banane", "voiture");
	}

	@Test
	void freeTextReplacementKeepsTheExpectedAnswer() {
		adapter.ensureForm(subjectId, "texte", "Réponse libre", 11);
		StoredQuestion saved = adapter.insert(
				subjectId, "texte", "Capitale de la France", new FreeTextDraft("Paris"));
		adapter.replace(saved.id(), "Capitale du Portugal", new FreeTextDraft("Lisbonne"));
		entityManager.flush();
		entityManager.clear();
		StoredQuestion loaded = adapter.findInSubject(subjectId, saved.id()).orElseThrow();
		assertThat(loaded.formCode()).isEqualTo("texte");
		assertThat(loaded.prompt()).isEqualTo("Capitale du Portugal");
		assertThat(loaded.expectedAnswer()).isEqualTo("Lisbonne");
		adapter.replace(saved.id(), "Capitale du Portugal", new FreeTextDraft("Lisbonne"));
		entityManager.flush();
		entityManager.clear();
		assertThat(adapter.findInSubject(subjectId, saved.id()).orElseThrow().expectedAnswer()).isEqualTo("Lisbonne");
		assertThat(jdbc.queryForObject("""
				select form_code from back_subject_question_forms
				where subject_id = ? and form_code = 'texte'
				""", String.class, subjectId)).isEqualTo("texte");
	}

	@Test
	void letterReplacementKeepsTheExpectedWord() {
		adapter.ensureForm(subjectId, "reponse_libre", "Lettre par lettre", 9);
		StoredQuestion saved = adapter.insert(
				subjectId, "reponse_libre", "Capitale de la France", new FreeTextDraft("Paris"));
		// Charge la ligne en session comme le use case update (find puis replace).
		assertThat(adapter.findInSubject(subjectId, saved.id()).orElseThrow().expectedAnswer()).isEqualTo("Paris");
		adapter.replace(saved.id(), "ouvrir bouteille de vin", new FreeTextDraft("tire bouchon"));
		StoredQuestion loaded = adapter.findInSubject(subjectId, saved.id()).orElseThrow();
		assertThat(loaded.formCode()).isEqualTo("reponse_libre");
		assertThat(loaded.prompt()).isEqualTo("ouvrir bouteille de vin");
		assertThat(loaded.expectedAnswer()).isEqualTo("tire bouchon");
		assertThat(jdbc.queryForObject("""
				select expected_answer from back_subject_question_letters where question_id = ?
				""", String.class, saved.id())).isEqualTo("tire bouchon");
	}

	@Test
	void simonReplacementKeepsTheTypeTheElementsAndTheSequence() {
		adapter.ensureForm(subjectId, "simon", "Simon", 7);
		StoredQuestion saved = adapter.insert(subjectId, "simon", "", new SimonDraft("couleurs", List.of(
				new SimonElementDraft("rouge", "#ff0000"),
				new SimonElementDraft("bleu", "#0000ff"),
				new SimonElementDraft("vert", "#00ff00")),
				List.of(0, 1, 2, 0)));
		assertThat(saved.simonSequence()).containsExactly(0, 1, 2, 0);
		// Charge la question en session comme le use case update (find puis replace).
		assertThat(adapter.findInSubject(subjectId, saved.id()).orElseThrow().simonElementType()).isEqualTo("couleurs");
		adapter.replace(saved.id(), "Refais la suite.", new SimonDraft("lettres", List.of(
				new SimonElementDraft("A", null),
				new SimonElementDraft("B", null)),
				List.of(1, 1, 0)));
		entityManager.flush();
		entityManager.clear();

		StoredQuestion loaded = adapter.findInSubject(subjectId, saved.id()).orElseThrow();
		assertThat(loaded.prompt()).isEqualTo("Refais la suite.");
		assertThat(loaded.simonElementType()).isEqualTo("lettres");
		assertThat(loaded.simonElements()).extracting(row -> row.label() + ":" + row.color())
				.containsExactly("A:null", "B:null");
		assertThat(loaded.simonElements()).extracting(row -> row.id())
				.doesNotContainAnyElementsOf(saved.simonElements().stream().map(row -> row.id()).toList());
		assertThat(loaded.simonSequence()).containsExactly(1, 1, 0);
	}

	@Test
	void databaseRejectsASimonStepWithoutItsElement() {
		StoredQuestion saved = insertSimonNumbers();
		assertThatThrownBy(() -> jdbc.update("""
				insert into back_subject_question_simon_steps (question_id, element_index, sort_order)
				values (?, 2, 2)
				""", saved.id())).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void databaseRejectsASimonColorOutsideLowerCaseHex() {
		StoredQuestion saved = insertSimonNumbers();
		assertThatThrownBy(() -> jdbc.update("""
				insert into back_subject_question_simon_elements (question_id, label, color, sort_order)
				values (?, '3', '#FF0000', 2)
				""", saved.id())).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void databaseRejectsMissingSubjectForm() {
		assertThatThrownBy(() -> jdbc.update("""
				insert into back_subject_questions (subject_id, form_code, prompt) values (?, 'unknown', 'X')
				""", subjectId)).isInstanceOf(DataIntegrityViolationException.class);
	}

	private StoredQuestion insert(int type) {
		return switch (type) {
			case 0 -> adapter.insert(subjectId, "qcm", "Question", new ChoicesDraft(List.of(
					new ChoiceDraft("B", false), new ChoiceDraft("A", true))));
			case 1 -> adapter.insert(subjectId, "memory", "", new PairsDraft(List.of(
					new PairDraft("Deux", "2"), new PairDraft("Un", "1"))));
			default -> adapter.insert(subjectId, "order", "", new ItemsDraft(List.of(
					new ItemDraft("Début"), new ItemDraft("Fin"))));
		};
	}

	private StoredQuestion insertSimonNumbers() {
		adapter.ensureForm(subjectId, "simon", "Simon", 7);
		return adapter.insert(subjectId, "simon", "", new SimonDraft("nombres", List.of(
				new SimonElementDraft("1", null),
				new SimonElementDraft("2", null)),
				List.of(0, 1)));
	}

	private Statistics resetStatistics() {
		entityManager.flush();
		entityManager.clear();
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.clear();
		return statistics;
	}
}

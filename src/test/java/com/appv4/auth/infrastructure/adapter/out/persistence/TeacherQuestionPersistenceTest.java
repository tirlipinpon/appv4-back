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

import com.appv4.auth.application.port.out.TeacherQuestionPort.ChoiceDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ChoicesDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ItemDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ItemsDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.LinkDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.LinksDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PairDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PairsDraft;
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
		StatementQuestionContentStore.class, LinkQuestionContentStore.class})
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
		List<StoredQuestion> rows = adapter.findBySubject(subjectId);
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
		assertThat(rows).hasSize(100);
		assertThat(rows).extracting(StoredQuestion::id).isSorted();
		assertThat(rows.get(0).choices()).extracting(choice -> choice.label()).containsExactly("B", "A");
		assertThat(rows.get(0).pairs()).isEmpty();
		assertThat(rows.get(1).pairs()).extracting(pair -> pair.questionSide()).containsExactly("Deux", "Un");
		assertThat(rows.get(2).items()).extracting(item -> item.label()).containsExactly("Début", "Fin");
	}

	@Test
	void hundredQcmQuestionsTakeOnlyTwoSelects() {
		for (int index = 0; index < 100; index++) {
			insert(0);
		}
		Statistics statistics = resetStatistics();
		assertThat(adapter.findBySubject(subjectId)).hasSize(100);
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
	}

	@Test
	void emptySubjectAndQuestionFromAnotherSubjectDoNotLoadContent() {
		StoredQuestion question = insert(0);
		Statistics statistics = resetStatistics();
		assertThat(adapter.findBySubject(-1)).isEmpty();
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
		List<StoredQuestion> rows = adapter.findBySubject(subjectId);
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

	private Statistics resetStatistics() {
		entityManager.flush();
		entityManager.clear();
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.clear();
		return statistics;
	}
}

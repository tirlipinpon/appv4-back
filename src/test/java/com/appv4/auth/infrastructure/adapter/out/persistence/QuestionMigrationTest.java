package com.appv4.auth.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Vérifie aussi une montée de version avec des contenus déjà enregistrés en V32. */
@EnabledIfEnvironmentVariable(named = "APPV4_PERSISTENCE_TEST_URL", matches = ".+")
class QuestionMigrationTest {

	@Test
	void migrationKeepsExistingQuestionIdsAndAllThreeContents() throws Exception {
		String url = System.getenv("APPV4_PERSISTENCE_TEST_URL");
		String user = System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_USER", "appv4");
		String password = System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_PASSWORD", "appv4");
		String schema = "question_upgrade_" + UUID.randomUUID().toString().replace("-", "");
		Flyway.configure().dataSource(url, user, password).schemas(schema).defaultSchema(schema).target("32").load().migrate();
		try (var connection = DriverManager.getConnection(url, user, password);
				var statement = connection.createStatement()) {
			connection.setSchema(schema);
			statement.execute("""
					insert into back_users (id, email, display_name) values (1, 'upgrade@example.test', 'Upgrade');
					insert into back_teacher_subjects (id, user_id, school_id, grade_label, name, name_key)
					values (1, 1, 1, 'CE1', 'Test', 'test');
					insert into back_subject_question_forms (subject_id, form_code, label, sort_order)
					values (1, 'qcm', 'QCM', 1), (1, 'memory', 'Memory', 2), (1, 'order', 'Ordre', 3);
					insert into back_subject_questions (id, subject_id, form_code, prompt)
					values (11, 1, 'qcm', 'Question'), (12, 1, 'memory', ''), (13, 1, 'order', '');
					insert into back_subject_question_choices (id, question_id, label, correct, sort_order)
					values (21, 11, 'Oui', true, 0), (22, 11, 'Non', false, 1);
					insert into back_subject_question_pairs (id, question_id, question_side, answer_side, sort_order)
					values (31, 12, 'Deux', '2', 0), (32, 12, 'Un', '1', 1);
					insert into back_subject_question_items (id, question_id, label, sort_order)
					values (41, 13, 'Avant', 0), (42, 13, 'Après', 1);
					""");
			Flyway.configure().dataSource(url, user, password).schemas(schema).defaultSchema(schema).load().migrate();
			try (var result = statement.executeQuery("""
					select q.id, c.id, c.label, c.correct, p.id, p.answer_side, i.id, i.label
					from back_subject_questions q
					left join back_subject_question_choices c on c.question_id = q.id and c.sort_order = 0
					left join back_subject_question_pairs p on p.question_id = q.id and p.sort_order = 0
					left join back_subject_question_items i on i.question_id = q.id and i.sort_order = 0
					order by q.id
					""")) {
				assertThat(result.next()).isTrue();
				assertThat(result.getLong(1)).isEqualTo(11);
				assertThat(result.getLong(2)).isEqualTo(21);
				assertThat(result.getString(3)).isEqualTo("Oui");
				assertThat(result.getBoolean(4)).isTrue();
				assertThat(result.next()).isTrue();
				assertThat(result.getLong(1)).isEqualTo(12);
				assertThat(result.getLong(5)).isEqualTo(31);
				assertThat(result.getString(6)).isEqualTo("2");
				assertThat(result.next()).isTrue();
				assertThat(result.getLong(1)).isEqualTo(13);
				assertThat(result.getLong(7)).isEqualTo(41);
				assertThat(result.getString(8)).isEqualTo("Avant");
				assertThat(result.next()).isFalse();
			}
			// La cascade matière -> questions -> contenus reste valide avec la nouvelle clé étrangère.
			statement.executeUpdate("delete from back_teacher_subjects where id = 1");
			try (var result = statement.executeQuery("select count(*) from back_subject_questions")) {
				assertThat(result.next()).isTrue();
				assertThat(result.getLong(1)).isZero();
			}
		}
	}
}

package com.appv4.auth.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Verifies moderation pagination against a disposable PostgreSQL database. */
@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("persistence-test")
@Import(TeacherModerationPersistenceAdapter.class)
@EnabledIfEnvironmentVariable(named = "APPV4_PERSISTENCE_TEST_URL", matches = ".+")
class TeacherModerationPaginationPersistenceTest {

	@Autowired
	private TeacherModerationPersistenceAdapter adapter;
	@Autowired
	private JdbcTemplate jdbc;

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> System.getenv("APPV4_PERSISTENCE_TEST_URL"));
		registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_USER", "appv4"));
		registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_PASSWORD", "appv4"));
	}

	@Test
	void pagesHaveStableOrderAndReportRemainingItems() {
		for (int i = 0; i < 5; i++) {
			long user = jdbc.queryForObject("insert into back_users (email, display_name) values (?, 'Test') returning id",
					Long.class, UUID.randomUUID() + "@example.test");
			jdbc.update("insert into back_teacher_role_requests (user_id, status, created_at) "
					+ "values (?, 'pending', '2020-01-01T00:00:00Z')", user);
		}
		var first = adapter.findModerationQueue(2, 0);
		var second = adapter.findModerationQueue(2, 1);
		var third = adapter.findModerationQueue(2, 2);
		assertThat(first.items()).hasSize(2);
		assertThat(second.items()).hasSize(2);
		assertThat(third.items()).hasSize(1);
		assertThat(first.truncated()).isTrue();
		assertThat(second.truncated()).isTrue();
		assertThat(third.truncated()).isFalse();
		var ids = java.util.stream.Stream.of(first, second, third).flatMap(page -> page.items().stream())
				.map(item -> item.id()).toList();
		assertThat(ids).doesNotHaveDuplicates().isSorted();
	}
}

package com.appv4.auth.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Verifies reset token cleanup on PostgreSQL. */
@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("persistence-test")
@EnabledIfEnvironmentVariable(named = "APPV4_PERSISTENCE_TEST_URL", matches = ".+")
class PasswordResetCleanupPersistenceTest {

	@Autowired
	private BackPasswordResetTokenJpaRepository tokens;
	@Autowired
	private JdbcTemplate jdbc;

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> System.getenv("APPV4_PERSISTENCE_TEST_URL"));
		registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_USER", "appv4"));
		registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_PASSWORD", "appv4"));
	}

	@Test
	void cleanupDeletesOnlyExpiredOutsideRetentionInBoundedBatches() {
		long user = jdbc.queryForObject("insert into back_users (email, display_name) values (?, 'Test') returning id",
				Long.class, UUID.randomUUID() + "@example.test");
		for (int i = 0; i < 3; i++) {
			jdbc.update("insert into back_password_reset_tokens (user_id, token_hash, expires_at) values (?, ?, ?)",
					user, UUID.randomUUID().toString(),
					java.sql.Timestamp.from(java.time.Instant.parse("2020-01-01T00:00:00Z")));
		}
		jdbc.update("insert into back_password_reset_tokens (user_id, token_hash, expires_at) values (?, ?, ?)",
				user, UUID.randomUUID().toString(),
					java.sql.Timestamp.from(java.time.Instant.parse("2030-01-01T00:00:00Z")));
		jdbc.update("insert into back_password_reset_tokens (user_id, token_hash, expires_at) values (?, ?, ?)",
				user, UUID.randomUUID().toString(),
				java.sql.Timestamp.from(java.time.Instant.parse("2025-01-15T00:00:00Z")));
		var cutoff = java.time.Instant.parse("2025-01-01T00:00:00Z");
		assertThat(tokens.deleteExpiredBatch(cutoff, 2)).isEqualTo(2);
		assertThat(tokens.deleteExpiredBatch(cutoff, 2)).isEqualTo(1);
		assertThat(tokens.deleteExpiredBatch(cutoff, 2)).isZero();
		assertThat(jdbc.queryForObject("select count(*) from back_password_reset_tokens where user_id = ?", Long.class, user))
				.isEqualTo(2);
	}
}

package com.appv4.auth.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.hibernate.SessionFactory;
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

import com.appv4.auth.application.port.out.ChildAvatarPort;
import com.appv4.auth.application.port.out.ChildAvatarPort.StoredAvatar;
import com.appv4.auth.application.port.out.ChildAvatarPort.StoredPin;

import jakarta.persistence.EntityManager;

@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("persistence-test")
@EnabledIfEnvironmentVariable(named = "APPV4_PERSISTENCE_TEST_URL", matches = ".+")
@Import(ChildAvatarPersistenceAdapter.class)
class ChildAvatarPersistenceTest {
	@Autowired
	private ChildAvatarPort avatars;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private EntityManager entityManager;

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> System.getenv("APPV4_PERSISTENCE_TEST_URL"));
		registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_USER", "appv4"));
		registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_PASSWORD", "appv4"));
	}

	@Test
	void findByChildIds_loadsStylesAndVariantsInOneQuery_whenSomeChildrenHaveNoChoices() {
		long parent = parent();
		long composed = child(parent, "Léa");
		long styleOnly = child(parent, "Noah");
		long defaultAvatar = child(parent, "Camille");
		long otherFamily = child(parent(), "Rose");
		avatars.save(composed, "lorelei", "bare", List.of(new StoredPin("mouth", "happy01")));
		avatars.save(styleOnly, "bottts", null, List.of());
		avatars.save(otherFamily, "micah", null, List.of());

		List<StoredPin> variants = List.of(new StoredPin("glasses", "none"), new StoredPin("mouth", "happy02"));
		avatars.save(composed, "lorelei", "sepia", variants);
		StoredAvatar expected = new StoredAvatar(composed, "lorelei", "sepia", variants);
		// Le PUT relit avant la fin de sa transaction, y compris après remplacement des variantes.
		assertThat(avatars.findByChildId(composed)).contains(expected);

		var stats = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		boolean originallyEnabled = stats.isStatisticsEnabled();
		stats.setStatisticsEnabled(true);
		long before = stats.getPrepareStatementCount();
		try {
			assertThat(avatars.findByChildIds(List.of(composed, styleOnly, defaultAvatar)))
					.containsOnlyKeys(composed, styleOnly)
					.containsEntry(composed, expected)
					.containsEntry(styleOnly, new StoredAvatar(styleOnly, "bottts", null, List.of()));
			assertThat(avatars.findByChildIds(List.of())).isEmpty();
			assertThat(stats.getPrepareStatementCount() - before).isEqualTo(1);
		} finally {
			stats.setStatisticsEnabled(originallyEnabled);
		}
	}

	private long parent() {
		return jdbc.queryForObject("insert into back_users (email, display_name) values (?, 'Test') returning id",
				Long.class, UUID.randomUUID() + "@example.test");
	}

	private long child(long parent, String firstName) {
		return jdbc.queryForObject("""
				insert into back_children (parent_user_id, first_name, last_name, age, curriculum,
				grade_label, avatar_code, pin_hash)
				values (?, ?, 'Test', 8, 'FRANCE', 'CE1', 'cat', 'hash') returning id
				""", Long.class, parent, firstName);
	}
}

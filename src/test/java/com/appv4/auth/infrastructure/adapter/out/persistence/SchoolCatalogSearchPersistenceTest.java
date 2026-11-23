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

/** Verifies school search on PostgreSQL. */
@DataJpaTest(showSql = false, properties = {"spring.jpa.hibernate.ddl-auto=validate",
		"spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("persistence-test")
@Import(SchoolCatalogPersistenceAdapter.class)
@EnabledIfEnvironmentVariable(named = "APPV4_PERSISTENCE_TEST_URL", matches = ".+")
class SchoolCatalogSearchPersistenceTest {

	@Autowired
	private SchoolCatalogPersistenceAdapter adapter;
	@Autowired
	private JdbcTemplate jdbc;

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> System.getenv("APPV4_PERSISTENCE_TEST_URL"));
		registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_USER", "appv4"));
		registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_PASSWORD", "appv4"));
	}

	@Test
	void searchIsBoundedAccentInsensitiveAndUsesStableCursor() {
		String marker = UUID.randomUUID().toString();
		for (int i = 0; i < 45; i++) {
			jdbc.update("insert into back_schools (name, country, teaching_types) values (?, 'France', 'Primaire')",
					"École " + marker + " " + i);
		}
		var search = new com.appv4.auth.application.service.SchoolCatalogSearchService(adapter);
		var first = search.search("parent", "FRANCE", "ecole " + marker, 0, 40);
		assertThat(first.schools()).hasSize(40);
		assertThat(first.nextCursor()).isNotNull();
		assertThat(first.schools().get(0).grades()).contains("CE1");
		var second = search.search("teacher", "France", "école " + marker, first.nextCursor(), 40);
		assertThat(second.schools()).hasSize(5);
		assertThat(second.nextCursor()).isNull();
		assertThat(search.search("parent", "France", "%_", 0, 40).schools()).isEmpty();
		assertThat(search.search("parent", "Belgique", marker, 0, 40).schools()).isEmpty();
		assertThat(search.search("teacher", "France", marker, 0, 999).schools()).hasSize(40);
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> search.search("admin", "", "", 0, 40))
				.isInstanceOf(com.appv4.auth.application.exception.AuthDomainException.class);
	}
}

package com.appv4.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.appv4.auth.infrastructure.config.LocalAdminSeedRunner;

/**
 * Les tests web ne doivent plus ouvrir la base de développement ni créer l'admin local.
 */
@SpringBootTest
@ActiveProfiles("test")
class TestProfileIsolationTest {

	@Autowired
	private ApplicationContext context;

	@Test
	void webTestsDoNotOpenTheDevDatabaseOrSeedAdmin() {
		assertThat(context.getEnvironment().getActiveProfiles()).contains("test");
		assertThat(context.getBeanNamesForType(DataSource.class)).isEmpty();
		assertThat(context.getBeanNamesForType(Flyway.class)).isEmpty();
		assertThat(context.getBeanNamesForType(LocalAdminSeedRunner.class)).isEmpty();
	}
}

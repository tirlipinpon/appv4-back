package com.appv4.auth.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.service.LocalAdminSeed;

/**
 * Au démarrage local, pose le compte qui ouvre la modération (SCRUM-142).
 *
 * <p><b>Business :</b> ce compte n'existe pas en production.
 * <p><b>Technique :</b> profil {@code local} seulement. Email et mot de passe viennent du {@code .env}.
 * <p><b>Métier / flow :</b> boot → {@link LocalAdminSeed} → login email habituel.
 */
@Component
@Profile("local")
public class LocalAdminSeedRunner implements ApplicationRunner {

	private static final Logger LOG = LoggerFactory.getLogger(LocalAdminSeedRunner.class);

	private final LocalAdminSeed localAdminSeed;
	private final String email;
	private final String password;

	public LocalAdminSeedRunner(
			AuthUserAccountPort authUserAccountPort,
			PasswordEncoder passwordEncoder,
			ActiveRolePort activeRolePort,
			@Value("${appv4.auth.admin-seed.email:}") String email,
			@Value("${appv4.auth.admin-seed.password:}") String password) {
		this.localAdminSeed = new LocalAdminSeed(authUserAccountPort, passwordEncoder, activeRolePort);
		this.email = email;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (email == null || email.isBlank() || password == null || password.isBlank()) {
			LOG.info("Compte admin local ignoré : APPV4_ADMIN_EMAIL ou APPV4_ADMIN_PASSWORD est vide.");
			return;
		}
		localAdminSeed.ensure(email, password);
		LOG.info("Compte admin local prêt pour {}", email.trim().toLowerCase());
	}
}

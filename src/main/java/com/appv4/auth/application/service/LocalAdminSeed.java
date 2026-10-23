package com.appv4.auth.application.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;

/**
 * Compte admin de développement (SCRUM-142).
 *
 * <p><b>Business :</b> sans ce compte, personne ne peut ouvrir la modération.
 * <p><b>Technique :</b> hash du mot de passe, jamais le clair en base ni dans git.
 * Si le compte existe déjà, le hash est aligné et le rôle admin est posé.
 * <p><b>Métier / flow :</b> appelé au démarrage du profil local seulement.
 */
public class LocalAdminSeed {

	static final String DISPLAY_NAME = "Admin";

	private final AuthUserAccountPort authUserAccountPort;
	private final PasswordEncoder passwordEncoder;
	private final ActiveRolePort activeRolePort;

	public LocalAdminSeed(
			AuthUserAccountPort authUserAccountPort,
			PasswordEncoder passwordEncoder,
			ActiveRolePort activeRolePort) {
		this.authUserAccountPort = authUserAccountPort;
		this.passwordEncoder = passwordEncoder;
		this.activeRolePort = activeRolePort;
	}

	public void ensure(String email, String rawPassword) {
		if (email == null || email.isBlank() || rawPassword == null || rawPassword.isBlank()) {
			return;
		}
		String normalized = email.trim().toLowerCase(Locale.ROOT);
		String hash = passwordEncoder.encode(rawPassword);
		long userId = authUserAccountPort.findByEmail(normalized)
				.map(AuthUserAccount::id)
				.orElseGet(() -> authUserAccountPort.createWithPassword(normalized, DISPLAY_NAME, hash).id());
		authUserAccountPort.updatePasswordHash(userId, hash);
		activeRolePort.grantAdmin(userId);
	}
}

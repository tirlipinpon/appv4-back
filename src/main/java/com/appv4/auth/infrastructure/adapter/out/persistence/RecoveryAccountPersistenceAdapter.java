package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.RecoveryAccountPort;

/**
 * Lecture des comptes pour la récupération de l'identifiant.
 *
 * <p><b>Business :</b> le nom affiché, le téléphone et le mail de contact servent à retrouver
 * l'email de connexion. Cette lecture ne les change pas.
 * <p><b>Technique :</b> {@code back_users}. Absent du profil test, comme les autres adaptateurs JPA.
 * <p><b>Métier / flow :</b> port out → cas d'usage de récupération.
 */
@Component
@Profile("!test")
public class RecoveryAccountPersistenceAdapter implements RecoveryAccountPort {

	private final BackUserJpaRepository users;

	public RecoveryAccountPersistenceAdapter(BackUserJpaRepository users) {
		this.users = users;
	}

	@Override
	public List<RecoveryCompte> all() {
		return users.findAll().stream()
				.map(user -> new RecoveryCompte(
						user.getId(),
						user.getEmail(),
						user.getDisplayName(),
						user.getPhone(),
						user.getContactEmail(),
						user.isActive()))
				.toList();
	}
}

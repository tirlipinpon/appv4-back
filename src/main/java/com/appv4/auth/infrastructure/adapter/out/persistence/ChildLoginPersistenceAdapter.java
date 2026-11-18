package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.infrastructure.adapter.out.persistence.BackChildJpaRepository.ChildLoginRow;

/**
 * Lit les fiches enfant pour le login de l'app (BACK-13).
 *
 * <p><b>Business :</b> une fiche masquée ou désactivée ne peut pas ouvrir de session.
 * <p><b>Technique :</b> requêtes de {@link BackChildJpaRepository}. Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out du port {@link ChildLoginPort}.
 */
@Component
@Profile("!test")
public class ChildLoginPersistenceAdapter implements ChildLoginPort {

	private final BackChildJpaRepository childRepository;

	public ChildLoginPersistenceAdapter(BackChildJpaRepository childRepository) {
		this.childRepository = childRepository;
	}

	@Override
	public List<ChildLoginCandidate> findActiveByFirstName(String normalizedFirstName) {
		return childRepository.findActiveLoginByFirstName(normalizedFirstName).stream()
				.map(ChildLoginPersistenceAdapter::candidate)
				.toList();
	}

	@Override
	public Optional<ChildLoginCandidate> findActiveById(long childId) {
		return childRepository.findActiveLoginById(childId).map(ChildLoginPersistenceAdapter::candidate);
	}

	@Override
	public Optional<ChildProfile> findActiveProfileById(long childId) {
		return childRepository.findActiveProfileById(childId)
				.map(row -> new ChildProfile(
						row.getId(),
						row.getFirstName(),
						row.getAvatarCode(),
						row.getHasPhoto()));
	}

	@Override
	public Optional<ChildPhotoBytes> findActivePhotoById(long childId) {
		return childRepository.findById(childId)
				.filter(child -> !child.isHidden() && child.isActive())
				.filter(child -> child.getPhoto() != null)
				.map(child -> new ChildPhotoBytes(child.getPhoto(), child.getPhotoContentType()));
	}

	private static ChildLoginCandidate candidate(ChildLoginRow row) {
		return new ChildLoginCandidate(row.getId(), row.getFirstName(), row.getPinHash());
	}
}

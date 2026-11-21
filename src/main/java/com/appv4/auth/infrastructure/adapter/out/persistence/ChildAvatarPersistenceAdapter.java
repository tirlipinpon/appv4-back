package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.ChildAvatarPort;

/**
 * Persiste le style et la palette (BACK-66).
 *
 * <p><b>Business :</b> une palette refusée n'arrive pas jusqu'ici.
 * <p><b>Technique :</b> JPA sur {@code back_child_avatars}. Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out du port {@link ChildAvatarPort}.
 */
@Component
@Profile("!test")
public class ChildAvatarPersistenceAdapter implements ChildAvatarPort {

	private final BackChildAvatarJpaRepository repository;

	public ChildAvatarPersistenceAdapter(BackChildAvatarJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredAvatar> findByChildId(long childId) {
		return repository.findById(childId).map(entity -> new StoredAvatar(
				entity.getChildId(), entity.getStyleCode(), entity.getPresetId()));
	}

	@Override
	@Transactional
	public void saveStyle(long childId, String styleCode, String presetId) {
		BackChildAvatarEntity entity = repository.findById(childId)
				.orElseGet(() -> new BackChildAvatarEntity(childId, styleCode, presetId));
		entity.apply(styleCode, presetId);
		repository.save(entity);
	}
}

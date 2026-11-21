package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
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
	private final BackChildAvatarPinJpaRepository pins;

	public ChildAvatarPersistenceAdapter(
			BackChildAvatarJpaRepository repository,
			BackChildAvatarPinJpaRepository pins) {
		this.repository = repository;
		this.pins = pins;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredAvatar> findByChildId(long childId) {
		return repository.findById(childId).map(entity -> new StoredAvatar(
				entity.getChildId(), entity.getStyleCode(), entity.getPresetId()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredPin> findPins(long childId) {
		return pins.findByChildId(childId).stream()
				.map(pin -> new StoredPin(pin.getZoneCode(), pin.getVariantCode()))
				.toList();
	}

	@Override
	@Transactional
	public void save(long childId, String styleCode, String presetId, List<StoredPin> nextPins) {
		BackChildAvatarEntity entity = repository.findById(childId)
				.orElseGet(() -> new BackChildAvatarEntity(childId, styleCode, presetId));
		entity.apply(styleCode, presetId);
		repository.save(entity);
		pins.deleteByChildId(childId);
		pins.flush();
		for (StoredPin pin : nextPins) {
			pins.save(new BackChildAvatarPinEntity(childId, pin.zoneCode(), pin.variantCode()));
		}
	}
}

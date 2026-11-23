package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.ChildAvatarPort;
import com.appv4.auth.infrastructure.adapter.out.persistence.BackChildAvatarJpaRepository.AvatarRow;

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
	public Map<Long, StoredAvatar> findByChildIds(List<Long> childIds) {
		if (childIds.isEmpty()) {
			return Map.of();
		}
		return repository.findAvatarsWithPins(childIds).stream()
				.collect(Collectors.groupingBy(AvatarRow::getChildId))
				.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> storedAvatar(entry.getValue())));
	}

	private static StoredAvatar storedAvatar(List<AvatarRow> rows) {
		AvatarRow avatar = rows.getFirst();
		List<StoredPin> variants = rows.stream()
				.filter(row -> row.getZoneCode() != null)
				.map(row -> new StoredPin(row.getZoneCode(), row.getVariantCode()))
				.toList();
		return new StoredAvatar(avatar.getChildId(), avatar.getStyleCode(), avatar.getPresetId(), variants);
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

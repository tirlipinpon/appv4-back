package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Style et palette enregistrés pour un enfant (BACK-66).
 *
 * <p><b>Business :</b> sans ligne, le personnage reste Lorelei sans palette.
 * <p><b>Technique :</b> le style, le preset et les variantes sont lus ensemble.
 * <p><b>Métier / flow :</b> Profil et liste des enfants du parent.
 */
public interface ChildAvatarPort {

	default Optional<StoredAvatar> findByChildId(long childId) {
		return Optional.ofNullable(findByChildIds(List.of(childId)).get(childId));
	}

	/** Les enfants sans avatar enregistré n'ont pas d'entrée dans le résultat. */
	Map<Long, StoredAvatar> findByChildIds(List<Long> childIds);

	void save(long childId, String styleCode, String presetId, List<StoredPin> pins);

	/**
	 * @param presetId identifiant de palette, ou {@code null} si aucune n'est choisie
	 */
	record StoredAvatar(long childId, String styleCode, String presetId, List<StoredPin> pins) {
		public StoredAvatar {
			pins = List.copyOf(pins);
		}
	}

	record StoredPin(String zoneCode, String variantCode) {
	}
}

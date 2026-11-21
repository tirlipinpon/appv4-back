package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Style et palette enregistrés pour un enfant (BACK-66).
 *
 * <p><b>Business :</b> sans ligne, le personnage reste Lorelei sans palette.
 * <p><b>Technique :</b> table {@code back_child_avatars}. Les variantes arrivent plus tard.
 * <p><b>Métier / flow :</b> lecture et écriture d'avatar → cet port.
 */
public interface ChildAvatarPort {

	Optional<StoredAvatar> findByChildId(long childId);

	List<StoredPin> findPins(long childId);

	void save(long childId, String styleCode, String presetId, List<StoredPin> pins);

	/**
	 * @param presetId identifiant de palette, ou {@code null} si aucune n'est choisie
	 */
	record StoredAvatar(long childId, String styleCode, String presetId) {
	}

	record StoredPin(String zoneCode, String variantCode) {
	}
}

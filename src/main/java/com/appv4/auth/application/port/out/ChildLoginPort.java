package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Port sortant — retrouver une fiche enfant pour ouvrir sa session (BACK-13).
 *
 * <p><b>Business :</b> seul un enfant encore proposé (ni masqué, ni désactivé)
 * peut entrer dans l'app. Le hash du code ne sort pas de ce port vers le HTTP.
 * <p><b>Technique :</b> le prénom est déjà normalisé (coupé, minuscules) par l'appelant.
 * <p><b>Métier / flow :</b> use case login → cet port → adapter JPA {@code back_children}.
 */
public interface ChildLoginPort {

	List<ChildLoginCandidate> findActiveByFirstName(String normalizedFirstName);

	Optional<ChildLoginCandidate> findActiveById(long childId);

	/**
	 * @param pinHash hash BCrypt du code à 4 chiffres, jamais le code en clair
	 */
	record ChildLoginCandidate(long id, String firstName, String pinHash) {
	}
}

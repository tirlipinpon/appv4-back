package com.appv4.auth.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Enfants encore proposés, avec l'XP déjà stocké (BACK-22).
 *
 * <p><b>Business :</b> une fiche inactive ou masquée n'entre dans aucun classement.
 * <p><b>Technique :</b> joint l'école et la progression. Pas de ligne d'XP : zéro.
 * <p><b>Métier / flow :</b> port out de la lecture enfant du classement.
 */
public interface ChildRankingRosterPort {

	List<VisibleChild> visibleChildren();

	Optional<ChildPhoto> photo(long childId);

	/**
	 * @param xp total déjà stocké, zéro s'il n'y a pas de ligne
	 * @param createdAt sert à départager deux prénoms identiques
	 */
	record VisibleChild(
			long id,
			String firstName,
			int age,
			String grade,
			Long schoolId,
			String schoolName,
			String country,
			int xp,
			String avatarCode,
			boolean hasPhoto,
			Instant createdAt) {
	}

	record ChildPhoto(byte[] bytes, String contentType) {
	}
}

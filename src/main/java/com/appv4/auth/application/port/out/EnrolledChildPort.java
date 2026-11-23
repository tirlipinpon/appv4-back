package com.appv4.auth.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Enfants encore proposés, pour les rapprocher d'une classe de professeur (SCRUM-160).
 *
 * <p><b>Business :</b> le professeur voit un élève inscrit, pas le compte du parent
 * et pas le code PIN. Une fiche masquée ou désactivée n'est plus proposée.
 * <p><b>Technique :</b> lecture de {@code back_children}. Ni {@code pin_hash},
 * ni {@code parent_user_id} ne sortent de ce port.
 * <p><b>Métier / flow :</b> port out de {@code GET /api/int/teacher/students}.
 */
public interface EnrolledChildPort {

	/**
	 * Fiches encore proposées dont l'école est dans cette liste.
	 * Une école vide ne renvoie personne : un enfant sans école n'a pas de classe.
	 */
	List<EnrolledChild> findActiveInSchools(Collection<Long> schoolIds);

	/**
	 * Fiche encore proposée, avec une école. Vide si elle est masquée, désactivée, ou sans école.
	 */
	Optional<EnrolledChild> findActive(long childId);

	/** Photo de cette fiche, si elle en a une. Vide sinon. */
	Optional<EnrolledPhoto> findPhoto(long childId);

	/**
	 * @param schoolId    école de la fiche, jamais absente dans cette lecture
	 * @param active      faux : le parent l'a désactivée, elle ne se montre pas au professeur
	 * @param avatarCode  code d'avatar, ou null si une photo le remplace
	 */
	record EnrolledChild(
			long id,
			String firstName,
			String lastName,
			int age,
			String curriculum,
			String grade,
			Long schoolId,
			String avatarCode,
			boolean hasPhoto,
			boolean active) {
	}

	record EnrolledPhoto(byte[] bytes, String contentType) {
	}
}

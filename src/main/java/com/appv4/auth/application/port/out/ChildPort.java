package com.appv4.auth.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Fiches enfant d'un parent (SCRUM-156).
 *
 * <p><b>Business :</b> un parent voit ses enfants encore affichés, même désactivés.
 * Désactiver les garde dans la liste et les retire du reste de l'application.
 * Masquer les retire de l'affichage sans effacer la fiche. Le code PIN n'est jamais relu.
 * <p><b>Technique :</b> table {@code back_children}. {@code pin_hash} seulement.
 * <p><b>Métier / flow :</b> port out de {@code /api/int/parent/children}.
 */
public interface ChildPort {

	ChildSaved insert(NewChild child);

	List<ChildSaved> findByParent(long parentUserId);

	/**
	 * Fiche de ce parent. Vide si l'identifiant est à quelqu'un d'autre.
	 *
	 * <p><b>Business :</b> on ne part pas de l'enfant d'un autre parent, et on ne montre pas sa fiche.
	 */
	Optional<ChildSaved> findOwned(long parentUserId, long childId);

	/**
	 * Met à jour la fiche de ce parent. {@code pinHash} null garde l'ancien hash.
	 * {@code replaceImage} faux garde la photo ou l'avatar déjà enregistré.
	 */
	Optional<ChildSaved> update(long parentUserId, long childId, ChildChanges changes);

	/** @return faux si cet enfant n'est pas affiché pour ce parent */
	boolean deactivate(long parentUserId, long childId);

	/** @return faux si cet enfant n'est pas affiché pour ce parent */
	boolean reactivate(long parentUserId, long childId);

	/**
	 * Le retire de l'affichage. La ligne, le hash et la photo restent.
	 *
	 * @return faux si cet enfant n'est pas affiché pour ce parent
	 */
	boolean hide(long parentUserId, long childId);

	void insertLink(long parentUserId, long subjectChildId, long relatedChildId, String relation);

	/**
	 * Crée le lien, ou change le mot si ces deux enfants sont déjà liés, dans un sens ou l'autre.
	 *
	 * <p><b>Business :</b> modifier une fiche peut ajouter un frère, une sœur, un cousin.
	 * Le lien déjà affiché garde la même paire et change seulement de mot.
	 * <p><b>Technique :</b> une ligne {@code back_child_links}. Pas de seconde ligne pour la paire inverse.
	 * <p><b>Métier / flow :</b> appelé par la modification, après la validation du code fermé.
	 */
	void saveLink(long parentUserId, long subjectChildId, long relatedChildId, String relation);

	/**
	 * Retire le lien entre ces deux enfants, dans un sens ou dans l'autre.
	 *
	 * <p><b>Business :</b> « Aucun » sur une fiche déjà liée enlève la ligne affichée.
	 * Les autres liens de l'enfant restent.
	 * <p><b>Technique :</b> supprime la ligne {@code back_child_links}. Les fiches restent.
	 * <p><b>Métier / flow :</b> modification, quand l'autre enfant est choisi et le mot est Aucun.
	 */
	void deleteLink(long parentUserId, long subjectChildId, long relatedChildId);

	List<ChildLinkView> findLinks(long parentUserId);

	Optional<ChildPhoto> findPhoto(long parentUserId, long childId);

	/**
	 * Copies de centres d'intérêt de cet enfant, même s'il est retiré de l'affichage.
	 *
	 * <p><b>Business :</b> masquer ou désactiver la fiche ne les efface pas.
	 * <p><b>Technique :</b> lignes {@code back_child_interests}. L'ordre n'est pas le contrat.
	 * <p><b>Métier / flow :</b> relu pour trier la fiche, et pour garder le moment d'une réécriture.
	 */
	List<InterestCopy> findInterests(long childId);

	/**
	 * Remplace toutes les copies de cet enfant.
	 *
	 * <p><b>Business :</b> la liste envoyée est la liste entière. Une liste vide efface.
	 * <p><b>Technique :</b> supprime puis réécrit. Le moment vient de l'appelant.
	 * <p><b>Métier / flow :</b> après un ajout ou une correction de fiche déjà acceptés.
	 */
	void replaceInterests(long childId, List<InterestCopy> copies);

	/**
	 * @param pinHash hash du PIN, jamais les 4 chiffres
	 */
	record NewChild(
			long parentUserId,
			String firstName,
			String lastName,
			String sex,
			int age,
			String curriculum,
			String grade,
			Long schoolId,
			String avatarCode,
			byte[] photo,
			String photoContentType,
			String pinHash) {
	}

	record ChildSaved(
			long id,
			long parentUserId,
			String firstName,
			String lastName,
			String sex,
			int age,
			String curriculum,
			String grade,
			Long schoolId,
			String avatarCode,
			boolean hasPhoto,
			boolean active) {
	}

	record ChildPhoto(byte[] bytes, String contentType) {
	}

	/**
	 * Une copie de centre d'intérêt.
	 *
	 * <p><b>Business :</b> l'orthographe affichée, et le moment où cette copie a été écrite.
	 * <p><b>Technique :</b> pas la forme comparée.
	 * <p><b>Métier / flow :</b> gardée avec la fiche, remplacée en bloc à l'enregistrement.
	 */
	record InterestCopy(String text, Instant writtenAt) {
	}

	/**
	 * @param pinHash      null pour conserver le code déjà hashé
	 * @param replaceImage vrai seulement si une nouvelle photo ou un nouvel avatar est envoyé
	 */
	record ChildChanges(
			String firstName,
			String lastName,
			String sex,
			int age,
			String curriculum,
			String grade,
			Long schoolId,
			boolean replaceImage,
			String avatarCode,
			byte[] photo,
			String photoContentType,
			String pinHash) {
	}

	/**
	 * Lien déjà enregistré. Le sujet est le nouvel enfant, l'autre est l'enfant de départ.
	 *
	 * @param relation code fermé, par exemple {@code FRERE}
	 */
	record ChildLinkView(
			long subjectChildId,
			String subjectFirstName,
			String subjectSex,
			long relatedChildId,
			String relatedFirstName,
			String relatedSex,
			String relation) {
	}
}

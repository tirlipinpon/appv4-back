package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Matières visibles par un professeur (SCRUM-161, SCRUM-172).
 *
 * <p><b>Business :</b> une matière a un nom, une école et une année. Plusieurs
 * professeurs peuvent y avoir accès (partage). Déplacer ne laisse qu'un accès.
 * Ce n'est jamais une copie : même id, mêmes liens enfants, mêmes formes futures.
 * <p><b>Technique :</b> {@code back_teacher_subjects} + {@code back_teacher_subject_access}.
 * {@code name_key} est le nom en minuscules. {@code origin_subject_id} reste vide.
 * <p><b>Métier / flow :</b> port out de {@code /api/int/teacher/subjects}. Quand le
 * profil retire une année, le service profil appelle {@link #hide} : l'accès part,
 * la matière est cachée seulement s'il ne reste personne.
 */
public interface TeacherSubjectPort {

	/** Matières encore affichées auxquelles ce professeur a accès. */
	List<StoredSubject> findByUser(long userId);

	StoredSubject insert(long userId, long schoolId, String grade, String name, String nameKey);

	/**
	 * Matière visible à laquelle ce professeur a encore accès.
	 */
	Optional<StoredSubject> findOwned(long userId, long subjectId);

	/** Matière encore affichée, quel que soit l'accès (détail d'une demande). */
	Optional<StoredSubject> findVisible(long subjectId);

	Optional<StoredSubject> rename(long userId, long subjectId, String name, String nameKey);

	/**
	 * La matière reste dans les listes des professeurs qui y ont accès.
	 * Le reste de l'application ne la propose plus.
	 *
	 * @return faux si elle n'est pas affichée pour ce professeur
	 */
	boolean deactivate(long userId, long subjectId);

	/**
	 * @return faux si elle n'est pas affichée pour ce professeur
	 */
	boolean reactivate(long userId, long subjectId);

	/**
	 * Retire l'accès de ce professeur. S'il était le dernier, cache la matière.
	 * La ligne de matière reste en base.
	 *
	 * @return faux si ce professeur n'avait pas accès, ou si elle est déjà cachée
	 */
	boolean hide(long userId, long subjectId);

	/**
	 * Noms affichés des autres professeurs qui ont accès, tri français.
	 * Liste vide s'il est seul.
	 */
	List<String> otherAccessNames(long subjectId, long viewerUserId);

	/**
	 * Tous les noms affichés des accès, tri français (page parent).
	 */
	List<String> allAccessNames(long subjectId);

	/**
	 * Ajoute un accès sans toucher à {@code user_id} de la matière.
	 */
	void addAccess(long subjectId, long userId);

	/**
	 * Remplace tous les accès par celui du collègue et aligne {@code user_id}.
	 */
	void replaceAccessWith(long subjectId, long soleUserId);

	boolean hasAccess(long subjectId, long userId);

	/** Crée une demande pending. */
	StoredTransferRequest insertTransferRequest(
			long subjectId, long fromUserId, long toUserId, String mode);

	boolean hasPendingTransfer(long subjectId, long toUserId);

	List<StoredTransferRequest> findPendingTransfersTo(long toUserId);

	Optional<StoredTransferRequest> findPendingTransferForTarget(long requestId, long toUserId);

	void markTransferAccepted(long requestId);

	void markTransferRejected(long requestId);

	/** Annule les autres demandes pending de cette matière (après un move accepté). */
	void cancelPendingTransfers(long subjectId, Long exceptRequestId);

	/**
	 * Vrai si une matière encore affichée de cette école et de cette année a déjà ce nom.
	 * Le professeur qui l'a créée n'importe pas : la classe n'en a qu'une.
	 *
	 * @param exceptId matière en cours de renommage, ignorée ; null à la création
	 */
	boolean nameTaken(long schoolId, String grade, String nameKey, Long exceptId);

	/**
	 * @param grade  année scolaire, le libellé du profil (CE1, 3e primaire)
	 * @param active faux : visible seulement chez les professeurs qui y ont accès
	 */
	record StoredSubject(long id, long userId, long schoolId, String grade, String name, boolean active) {
	}

	/**
	 * Demande de transfert encore ouverte ou déjà tranchée.
	 *
	 * @param mode {@code move} ou {@code share}
	 */
	record StoredTransferRequest(
			long id,
			long subjectId,
			long fromUserId,
			long toUserId,
			String mode,
			String status) {
	}

	/**
	 * Une classe du profil : une école et une année. Sert à savoir quelles matières garder.
	 */
	record SubjectClassKey(long schoolId, String grade) {
	}
}

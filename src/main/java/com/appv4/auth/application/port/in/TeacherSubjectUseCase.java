package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.TeacherSubjectColleaguesView;
import com.appv4.auth.application.result.TeacherSubjectsView;
import com.appv4.auth.application.result.TeacherSubjectsView.SubjectLine;
import com.appv4.auth.application.result.TeacherTransferInvitesView;

/**
 * Créer, revoir, renommer, retirer et proposer un transfert de matières
 * (SCRUM-161, SCRUM-172, SCRUM-189).
 *
 * <p><b>Business :</b> le professeur ne pose une matière que sur une année déjà
 * cochée. Il propose un déplacement ou un partage à un collègue de la liste ;
 * le collègue accepte ou refuse avant que l'accès change.
 * <p><b>Technique :</b> l'identifiant vient du jeton, jamais du corps de la requête.
 * <p><b>Métier / flow :</b> port in de {@code /api/int/teacher/subjects}.
 */
public interface TeacherSubjectUseCase {

	TeacherSubjectsView list(long userId, String activeRole);

	SubjectLine create(long userId, String activeRole, Long schoolId, String grade, String name);

	SubjectLine rename(long userId, String activeRole, long subjectId, String name);

	void deactivate(long userId, String activeRole, long subjectId);

	void reactivate(long userId, String activeRole, long subjectId);

	/** Retire l'accès de ce professeur. La matière reste si d'autres y ont accès. */
	void remove(long userId, String activeRole, long subjectId);

	TeacherSubjectColleaguesView listColleagues(long userId, String activeRole, long subjectId);

	/**
	 * Propose un déplacement ou un partage. L'accès ne change qu'après acceptation.
	 *
	 * @param mode {@code move} ou {@code share}
	 */
	void requestTransfer(
			long userId, String activeRole, long subjectId, Long colleagueUserId, String mode);

	TeacherTransferInvitesView listTransferInvites(long userId, String activeRole);

	void acceptTransfer(long userId, String activeRole, long inviteId);

	void rejectTransfer(long userId, String activeRole, long inviteId);
}

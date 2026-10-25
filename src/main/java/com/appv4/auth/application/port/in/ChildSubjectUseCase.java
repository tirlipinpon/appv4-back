package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.ParentSubjectsView;

/**
 * Choisir les matières d'un enfant, et les retirer de sa liste (SCRUM-162).
 *
 * <p><b>Business :</b> la page montre chaque enfant. Les matières de son école
 * et de son année sont déjà dans sa liste. Il ajoute les autres, ou en retire.
 * <p><b>Technique :</b> le parent vient du jeton. L'enfant est dans l'adresse ou le corps.
 * <p><b>Métier / flow :</b> port in de {@code /api/int/parent/subjects}.
 */
public interface ChildSubjectUseCase {

	ParentSubjectsView loadAll(long parentUserId, String activeRole);

	ParentSubjectsView add(long parentUserId, String activeRole, long childId, Long subjectId);

	ParentSubjectsView remove(long parentUserId, String activeRole, long childId, long subjectId);
}

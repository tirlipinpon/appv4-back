package com.appv4.auth.application.port.in;

import java.util.List;

import com.appv4.auth.application.result.ParentSubjectsView;
import com.appv4.auth.application.result.ChildSubjectsView.SubjectOffer;

/**
 * Choisir les matières d'un enfant, et les retirer de sa liste (SCRUM-162).
 *
 * <p><b>Business :</b> la page montre chaque enfant. Les matières de son école
 * et de son année sont déjà dans sa liste. Il ajoute les autres, ou en retire.
 * <p><b>Technique :</b> le parent vient du jeton. L'enfant est dans l'adresse ou le corps.
 * <p><b>Métier / flow :</b> port in de {@code /api/int/parent/subjects}.
 */
public interface ChildSubjectUseCase {

	ParentSubjectsView loadSummary(long parentUserId, String activeRole);

	SubjectPage searchOffers(long parentUserId, String activeRole, long childId, String query,
			long schoolId, String grade, long afterId, int limit);

	SubjectOffer addOne(long parentUserId, String activeRole, long childId, Long subjectId);

	SubjectOffer removeOne(long parentUserId, String activeRole, long childId, long subjectId);

	record SubjectPage(List<SubjectOffer> offers, Long nextCursor) {
	}

	ParentSubjectsView loadAll(long parentUserId, String activeRole);

	ParentSubjectsView add(long parentUserId, String activeRole, long childId, Long subjectId);

	ParentSubjectsView remove(long parentUserId, String activeRole, long childId, long subjectId);
}

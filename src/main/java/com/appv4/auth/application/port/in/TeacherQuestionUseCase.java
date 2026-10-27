package com.appv4.auth.application.port.in;

import java.util.List;

import com.appv4.auth.application.result.TeacherQuestionFormsView;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.QuestionLine;

/**
 * Préparer un choix multiple dans une matière (SCRUM-174).
 *
 * <p><b>Business :</b> le professeur choisit une forme dans la liste que l'API
 * propose, puis saisit l'énoncé et les propositions. Aujourd'hui une seule
 * forme est proposable : le choix multiple. L'élève ne joue pas encore ici.
 * <p><b>Technique :</b> l'identifiant vient du jeton. Une proposition vide
 * n'est pas enregistrée. Moins de deux propositions, ou aucune bonne réponse,
 * est refusé.
 * <p><b>Métier / flow :</b> port in de {@code /api/int/teacher/question-forms}
 * et {@code /api/int/teacher/subjects/{id}/questions}. Corriger un énoncé
 * déjà enregistré passe par le même port, sans changer la forme.
 */
public interface TeacherQuestionUseCase {

	TeacherQuestionFormsView listForms(long userId, String activeRole);

	TeacherSubjectQuestionsView list(long userId, String activeRole, long subjectId);

	QuestionLine create(
			long userId,
			String activeRole,
			long subjectId,
			String formCode,
			String prompt,
			List<ChoiceInput> choices);

	/**
	 * Remplace l'énoncé et les propositions. La forme déjà enregistrée reste.
	 */
	QuestionLine update(
			long userId,
			String activeRole,
			long subjectId,
			long questionId,
			String formCode,
			String prompt,
			List<ChoiceInput> choices);

	/**
	 * @param correct {@code null} compte comme faux
	 */
	record ChoiceInput(String label, Boolean correct) {
	}
}

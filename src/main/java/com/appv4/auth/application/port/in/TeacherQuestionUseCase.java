package com.appv4.auth.application.port.in;

import java.util.List;

import com.appv4.auth.application.result.TeacherQuestionFormsView;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.QuestionLine;

/**
 * Préparer une question dans une matière (SCRUM-174, SCRUM-175).
 *
 * <p><b>Business :</b> le professeur choisit une forme dans la liste que l'API
 * propose. Le choix multiple a un énoncé et des propositions. Le Memory a des
 * paires, et l'énoncé peut rester vide. L'élève ne joue pas encore ici.
 * <p><b>Technique :</b> l'identifiant vient du jeton. Une proposition vide, ou
 * une paire incomplète, n'est pas enregistrée. Moins de deux propositions, ou
 * aucune bonne réponse, est refusé. Moins de deux paires aussi.
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
			List<ChoiceInput> choices,
			List<PairInput> pairs);

	/**
	 * Remplace le contenu. La forme déjà enregistrée reste.
	 */
	QuestionLine update(
			long userId,
			String activeRole,
			long subjectId,
			long questionId,
			String formCode,
			String prompt,
			List<ChoiceInput> choices,
			List<PairInput> pairs);

	/**
	 * @param correct {@code null} compte comme faux
	 */
	record ChoiceInput(String label, Boolean correct) {
	}

	/** Côté question et côté réponse d'une paire Memory. */
	record PairInput(String questionSide, String answerSide) {
	}
}

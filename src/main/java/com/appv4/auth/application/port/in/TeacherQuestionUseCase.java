package com.appv4.auth.application.port.in;

import java.util.List;

import com.appv4.auth.application.result.TeacherQuestionFormsView;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.QuestionLine;

/**
 * Préparer une question dans une matière (SCRUM-174, SCRUM-175, SCRUM-176, SCRUM-177).
 *
 * <p><b>Business :</b> le professeur choisit une forme dans la liste que l'API
 * propose. Le choix multiple a un énoncé et des propositions. Le Memory a des
 * paires. L'ordre a des éléments déjà rangés. Le vrai ou faux a des phrases,
 * chacune vraie ou fausse. L'énoncé du Memory, de l'ordre et du vrai ou faux
 * peut rester vide. L'élève ne joue pas encore ici.
 * <p><b>Technique :</b> l'identifiant vient du jeton. Une proposition vide, une
 * paire incomplète, ou un élément vide, n'est pas enregistré. Moins de deux
 * propositions, ou aucune bonne réponse, est refusé. Moins de deux paires, ou
 * moins de deux éléments, aussi.
 * <p><b>Métier / flow :</b> port in de {@code /api/int/teacher/question-forms}
 * et {@code /api/int/teacher/subjects/{id}/questions}. Corriger un énoncé
 * déjà enregistré passe par le même port, sans changer la forme.
 */
public interface TeacherQuestionUseCase {

	TeacherQuestionFormsView listForms(long userId, String activeRole);

	TeacherSubjectQuestionsView list(long userId, String activeRole, long subjectId);

	default QuestionLine create(
			long userId,
			String activeRole,
			long subjectId,
			String formCode,
			String prompt,
			List<ChoiceInput> choices,
			List<PairInput> pairs,
			List<ItemInput> items) {
		return create(userId, activeRole, subjectId, formCode, prompt, new LegacyInput(choices, pairs, items));
	}

	/**
	 * Remplace le contenu. La forme déjà enregistrée reste.
	 */
	default QuestionLine update(
			long userId,
			String activeRole,
			long subjectId,
			long questionId,
			String formCode,
			String prompt,
			List<ChoiceInput> choices,
			List<PairInput> pairs,
			List<ItemInput> items) {
		return update(userId, activeRole, subjectId, questionId, formCode, prompt, new LegacyInput(choices, pairs, items));
	}

	/** Entrée extensible : chaque forme peut déclarer sa structure propre. */
	QuestionLine create(long userId, String activeRole, long subjectId, String formCode, String prompt, ContentInput content);

	QuestionLine update(long userId, String activeRole, long subjectId, long questionId,
			String formCode, String prompt, ContentInput content);

	interface ContentInput {
	}

	/** Adaptation des formulaires QCM, Memory et Ordre déjà publiés. */
	record LegacyInput(List<ChoiceInput> choices, List<PairInput> pairs, List<ItemInput> items) implements ContentInput {
	}

	/**
	 * @param correct {@code null} compte comme faux
	 */
	record ChoiceInput(String label, Boolean correct) {
	}

	/** Côté question et côté réponse d'une paire Memory. */
	record PairInput(String questionSide, String answerSide) {
	}

	/** Texte d'un élément dans une liste déjà ordonnée. */
	record ItemInput(String label) {
	}

	/** Phrases d'un vrai ou faux. Chaque phrase non vide a un côté. */
	record StatementsInput(List<StatementInput> statements) implements ContentInput {
	}

	/**
	 * @param truthful {@code null} veut dire que le côté n'est pas choisi
	 */
	record StatementInput(String label, Boolean truthful) {
	}

	/** Couples mot / réponse d'un lien. Une ligne à moitié vide est refusée. */
	record LinksInput(List<LinkInput> links) implements ContentInput {
	}

	record LinkInput(String leftLabel, String rightLabel) {
	}

	/** Phrase à trous, mots justes dans l'ordre des ___ et leurres de la banque. */
	record GapsInput(String sentence, List<GapWordInput> answers, List<GapWordInput> decoys) implements ContentInput {
	}

	record GapWordInput(String label) {
	}
}

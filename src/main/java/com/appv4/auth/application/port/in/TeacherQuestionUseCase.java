package com.appv4.auth.application.port.in;

import java.util.List;

import com.appv4.auth.application.result.TeacherQuestionFormsView;
import com.appv4.auth.application.result.TeacherSubjectFollowersView;
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
 * Copier des questions vers une matière du même professeur crée de nouveaux
 * énoncés : les originaux restent sur la matière d'où ils viennent.
 * Supprimer une ou plusieurs questions les retire de cette matière. Avant ça,
 * le professeur voit combien d'enfants suivent le cours et la liste de leurs noms.
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

	/**
	 * Copie les questions choisies sur {@code targetSubjectId}.
	 * Chaque copie a un nouvel id. Un id cité deux fois n'est copié qu'une fois.
	 * La matière d'arrivée peut être celle de départ : les originaux restent.
	 */
	TeacherSubjectQuestionsView cloneQuestions(
			long userId,
			String activeRole,
			long sourceSubjectId,
			long targetSubjectId,
			List<Long> questionIds);

	/**
	 * Enfants qui suivent encore ce cours : ceux de la classe, sauf retrait du parent,
	 * plus ceux ajoutés depuis une autre classe. La phrase {@code warning} et les noms
	 * viennent d'ici : l'écran les affiche tels quels.
	 */
	TeacherSubjectFollowersView followers(long userId, String activeRole, long subjectId);

	/**
	 * Retire les questions choisies de cette matière. Un id cité deux fois n'est retiré
	 * qu'une fois. Si une question n'est pas dans la matière, rien n'est retiré.
	 *
	 * @return les id réellement retirés, dans l'ordre demandé
	 */
	List<Long> deleteQuestions(long userId, String activeRole, long subjectId, List<Long> questionIds);

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

	/** Énoncé déjà vérifié à part, et la réponse que l'élève devra écrire dans un seul champ. */
	record FreeTextInput(String expectedAnswer) implements ContentInput {
	}

	/**
	 * Image à cliquer. {@code imageWebp} est le fichier déjà allégé, en base64.
	 * {@code requireAllCorrect} dit si toutes les zones justes sont obligatoires.
	 */
	record ClickImageInput(String imageWebp, Boolean requireAllCorrect, List<ClickZoneInput> zones)
			implements ContentInput {
	}

	/** Une zone : nom, juste ou non, forme, points entre 0 et 1. */
	record ClickZoneInput(String label, Boolean correct, String shape, List<ClickPointInput> points) {
	}

	/** Coordonnée en proportion de l'image. */
	record ClickPointInput(Double x, Double y) {
	}

	/**
	 * Puzzle. {@code imageWebp} est le fichier déjà allégé, en base64.
	 * Les pièces n'ont pas de côté juste ou faux.
	 */
	record PuzzleInput(String imageWebp, List<PuzzlePieceInput> pieces) implements ContentInput {
	}

	/** Une pièce : nom, forme, points entre 0 et 1. */
	record PuzzlePieceInput(String label, String shape, List<ClickPointInput> points) {
	}
}

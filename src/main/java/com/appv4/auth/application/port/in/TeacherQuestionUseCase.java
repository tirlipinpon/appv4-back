package com.appv4.auth.application.port.in;

import java.util.List;

import com.appv4.auth.application.result.LetterAttemptView;
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
 * peut rester vide. La saisie lettre par lettre a un mot attendu : Valider
 * compare ce que l'élève a composé, sans retirer les accents.
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

	TeacherSubjectQuestionsView list(long userId, String activeRole, long subjectId, long afterId, int limit);

	/**
	 * Verdict de Valider pour une question {@code reponse_libre}.
	 * Le mot composé arrive déjà assemblé, une lettre par case.
	 */
	LetterAttemptView tryLetterAnswer(long userId, String activeRole, long subjectId, long questionId, String attempt);

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

	/**
	 * Texte d'un élément dans une liste déjà ordonnée.
	 * {@code axisLabel} : jalon de frise (ignoré si timeline désactivée).
	 */
	record ItemInput(String label, String axisLabel) {
		public ItemInput(String label) {
			this(label, null);
		}
	}

	/**
	 * Contenu Ordre : étapes + option frise (BACK-46).
	 *
	 * <p>{@code timelineEnabled} null compte comme faux.
	 */
	record OrderInput(Boolean timelineEnabled, List<ItemInput> items) implements ContentInput {
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

	/**
	 * Classement : colonnes nommées et cartes, chacune rattachée à une colonne
	 * via {@code columnIndex} (index dans {@code columns}).
	 */
	record ClassificationInput(List<ColumnInput> columns, List<CardInput> cards) implements ContentInput {
	}

	record ColumnInput(String label) {
	}

	/**
	 * @param columnIndex index 0-based dans la liste {@code columns} de la même requête
	 */
	record CardInput(String label, Integer columnIndex) {
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
	 * {@code clicksOrdered} null ou true : les zones justes ont un rang. false : aucun rang.
	 */
	record ClickImageInput(
			String imageWebp, Boolean requireAllCorrect, Boolean clicksOrdered, List<ClickZoneInput> zones)
			implements ContentInput {
		public ClickImageInput(String imageWebp, Boolean requireAllCorrect, List<ClickZoneInput> zones) {
			this(imageWebp, requireAllCorrect, null, zones);
		}
	}

	/**
	 * Une zone : nom, juste ou non, forme, points entre 0 et 1.
	 * {@code clickOrder} est le rang du clic demandé au professeur pour une zone juste.
	 */
	record ClickZoneInput(
			String label, Boolean correct, String shape, List<ClickPointInput> points, Integer clickOrder) {
	}

	/** Coordonnée en proportion de l'image. */
	record ClickPointInput(Double x, Double y) {
	}

	/**
	 * Puzzle. {@code imageWebp} sert à découper. {@code boardMode} dit le plateau :
	 * {@code fond} (image dédiée), {@code blanc} (cadre vide), {@code trous} (découpe trouée).
	 * {@code boardImageWebp} n'est requis qu'en mode fond. {@code snapDistance} est le magnétisme.
	 * En mode blanc et en mode fond, chaque pièce porte {@code slotX}/{@code slotY} (emplacement
	 * cible posé par le professeur) ; en mode fond, {@code slotWidth} donne en plus sa taille.
	 */
	record PuzzleInput(
			String imageWebp,
			String boardMode,
			String boardImageWebp,
			Double snapDistance,
			List<PuzzlePieceInput> pieces)
			implements ContentInput {
	}

	/**
	 * Une pièce : nom, forme, points entre 0 et 1.
	 * {@code slotX}/{@code slotY} : coin haut-gauche de l'emplacement sur le plateau (blanc, fond).
	 * {@code slotWidth} : largeur de la pièce sur le fond, en proportion du plateau (fond seulement).
	 */
	record PuzzlePieceInput(
			String label,
			String shape,
			List<ClickPointInput> points,
			Double slotX,
			Double slotY,
			Double slotWidth) {
	}
}

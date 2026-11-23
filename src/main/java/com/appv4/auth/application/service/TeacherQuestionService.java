package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.MemoryMatch;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.Submission;
import com.appv4.auth.application.result.ChildLetterLocksView;
import com.appv4.auth.application.result.ChildPlayQuestionView;
import com.appv4.auth.application.service.question.play.ChildAnswerGrade;
import com.appv4.auth.application.service.question.play.PlayableQuestionForm;
import com.appv4.auth.application.service.question.play.QuestionPlayEngine;
import com.appv4.auth.application.service.question.play.SimonSequenceDraw;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.GamificationInput;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.CardDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ChoiceDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ChoicesDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClassificationDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickImageDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickZoneDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ColumnDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.FreeTextDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.GapWordDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.GapsDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ItemDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ItemsDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.LinkDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.LinksDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PairDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PairsDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PuzzleDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PuzzlePieceDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.SimonDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.SimonElementDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StatementDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StatementsDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredCard;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoice;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoices;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClassification;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickImage;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickZone;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredColumn;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredFreeText;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredGapWord;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredGaps;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredItem;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredItems;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredLinks;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPair;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPairs;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPuzzle;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPuzzlePiece;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredSimon;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredSimonElement;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredStatement;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredStatements;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredSubject;
import com.appv4.auth.application.result.LetterAttemptView;
import com.appv4.auth.application.result.TeacherQuestionFormsView;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;
import com.appv4.auth.application.result.TeacherSubjectFollowersView;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.QuestionLine;
import com.appv4.auth.application.service.question.LetterByLetterMatch;
import com.appv4.auth.application.service.question.QuestionFormHandler;
import com.appv4.auth.application.service.question.QuestionGamification;
import com.appv4.auth.application.service.question.QuestionFormHandler.Prepared;
import com.appv4.auth.application.service.question.QuestionFormRegistry;

/**
 * Choix multiple, Memory, Ordre et Vrai ou faux (SCRUM-174, SCRUM-175, SCRUM-176, SCRUM-177).
 *
 * <p><b>Business :</b> le professeur ne voit que les formes qu'on sait enregistrer.
 * Le choix multiple exige un énoncé, deux propositions et une bonne réponse.
 * Le Memory exige deux paires complètes. L'ordre exige deux éléments déjà rangés.
 * Le vrai ou faux exige deux phrases, chacune marquée vraie ou fausse.
 * L'énoncé du Memory, de l'ordre et du vrai ou faux peut rester vide.
 * Corriger une question remplace son contenu. La forme reste.
 * Copier des questions en crée de nouvelles sur la matière choisie.
 * Les originaux restent. Le professeur peut aussi en supprimer une ou plusieurs.
 * Avant ça, il voit combien d'enfants suivent le cours et leurs noms.
 * Le nom de la matière ne change pas. L'élève ne joue pas ici.
 * La réponse libre (SCRUM-198) exige un énoncé et une réponse attendue.
 * La comparaison, quand l'élève validera, ignore majuscules, accents et espaces au bord.
 * La saisie lettre par lettre (SCRUM-182) exige aussi un énoncé et un mot.
 * L'élève a une case par lettre. Valider ignore les majuscules et les espaces
 * au bord, et refuse un accent différent.
 * Le Simon (ADMIN-25, BACK-54) exige un genre d'éléments, deux éléments de ce genre
 * et une suite d'au moins deux pas ; son énoncé peut rester vide.
 * <p><b>Technique :</b> registre de validateurs indépendants par forme.
 * Les énoncés passent par {@link TeacherQuestionPort}. L'accès matière passe par
 * {@link TeacherSubjectPort#findOwned}. Les enfants passent par {@link ChildSubjectPort}.
 * <p><b>Métier / flow :</b> port in → ce service → ports out. L'écran élève
 * n'est pas dans ce service.
 */
public class TeacherQuestionService implements TeacherQuestionUseCase {

	public static final String QCM = "qcm";
	public static final String QCM_LABEL = "Choix multiples";
	public static final int QCM_ORDER = 1;

	public static final String MEMORY = "memory";
	public static final String MEMORY_LABEL = "Memory";
	public static final int MEMORY_ORDER = 2;

	public static final String ORDER = "order";
	public static final String ORDER_LABEL = "Ordre";
	public static final int ORDER_SORT = 3;

	public static final String PROMPT_REQUIRED = "L'énoncé est obligatoire.";
	public static final String PROMPT_TOO_LONG = "L'énoncé dépasse 500 caractères.";
	public static final String CHOICE_TOO_LONG = "Une proposition dépasse 200 caractères.";
	public static final String CHOICES_REQUIRED = "Il faut au moins deux propositions.";
	public static final String CORRECT_REQUIRED = "Il faut marquer au moins une bonne réponse.";
	public static final String FORM_UNKNOWN = "Cette forme de question n'est pas disponible.";
	public static final String FORM_LOCKED = "La forme de la question reste celle déjà enregistrée.";
	public static final String QUESTION_MISSING = "Cette question n'existe pas.";
	public static final String PAIRS_REQUIRED = "Il faut au moins deux paires.";
	public static final String PAIR_TOO_LONG = "Un côté de paire dépasse 200 caractères.";
	public static final String ITEMS_REQUIRED = "Il faut au moins deux éléments.";
	public static final String ITEM_TOO_LONG = "Un élément dépasse 200 caractères.";
	public static final String AXIS_LABEL_REQUIRED = "Chaque jalon de la ligne du temps doit avoir un libellé.";
	public static final String AXIS_LABEL_TOO_LONG = "Un libellé de jalon dépasse 80 caractères.";

	public static final String TRUE_FALSE = "vrai_faux";
	public static final String TRUE_FALSE_LABEL = "Vrai ou faux";
	public static final int TRUE_FALSE_ORDER = 4;

	public static final String PHRASES_REQUIRED = "Il faut au moins deux phrases.";
	public static final String SIDE_REQUIRED = "Chaque phrase doit être marquée vraie ou fausse.";
	public static final String PHRASE_TOO_LONG = "Une phrase dépasse 200 caractères.";

	public static final String LINKS = "liens";
	public static final String LINKS_LABEL = "Liens";
	public static final int LINKS_ORDER = 5;

	public static final String LINKS_REQUIRED = "Il faut au moins deux liens.";
	public static final String LINK_INCOMPLETE = "Chaque mot doit avoir une réponse.";
	public static final String LINK_TOO_LONG = "Un mot ou une réponse dépasse 200 caractères.";

	public static final String CASE_VIDE = "case_vide";
	public static final String CASE_VIDE_LABEL = "Case vide";
	public static final int CASE_VIDE_ORDER = 6;

	public static final String CLASSIFICATION = "classement";
	public static final String CLASSIFICATION_LABEL = "Classement";
	public static final int CLASSIFICATION_ORDER = 7;
	public static final String COLUMNS_REQUIRED = "Il faut au moins deux colonnes.";
	public static final String CARDS_REQUIRED = "Il faut au moins deux cartes.";
	public static final String COLUMN_LABEL_REQUIRED = "Chaque colonne doit avoir un nom.";
	public static final String CARD_LABEL_REQUIRED = "Chaque carte doit avoir un texte.";
	public static final String COLUMN_LABEL_TOO_LONG = "Un nom de colonne dépasse 200 caractères.";
	public static final String CARD_LABEL_TOO_LONG = "Un texte de carte dépasse 200 caractères.";
	public static final String CARD_COLUMN_REQUIRED = "Chaque carte doit être rattachée à une colonne.";
	public static final String COLUMN_EMPTY = "Chaque colonne doit contenir au moins une carte.";

	public static final String GAP_REQUIRED = "La phrase doit contenir au moins un trou.";
	public static final String GAP_WORD_REQUIRED = "Chaque trou doit avoir un mot juste.";
	public static final String GAP_WORD_TOO_LONG = "Un mot dépasse 200 caractères.";
	public static final String GAP_SENTENCE_TOO_LONG = "La phrase dépasse 500 caractères.";

	public static final String TEXTE = "texte";
	public static final String TEXTE_LABEL = "Réponse libre";
	public static final int TEXTE_ORDER = 11;
	public static final String ANSWER_REQUIRED = "La réponse attendue est obligatoire.";
	public static final String ANSWER_TOO_LONG = "La réponse attendue dépasse 500 caractères.";

	public static final String REPONSE_LIBRE = "reponse_libre";
	public static final String REPONSE_LIBRE_LABEL = "Lettre par lettre";
	public static final int REPONSE_LIBRE_ORDER = 9;
	public static final String LETTER_ACCEPTED = "La réponse est acceptée.";
	public static final String LETTER_REFUSED = "La réponse n'est pas acceptée.";
	public static final String LETTER_FORM = "Cette question n'utilise pas la saisie lettre par lettre.";

	public static final String IMAGE_CLICK = "image_interactive";
	public static final String IMAGE_CLICK_LABEL = "Image à cliquer";
	public static final int IMAGE_CLICK_ORDER = 8;
	public static final String SHAPE_POLYGON = "polygon";
	public static final String SHAPE_RECT = "rect";
	public static final String SHAPE_ELLIPSE = "ellipse";
	public static final String IMAGE_REQUIRED = "L'image est obligatoire.";
	public static final String BOARD_IMAGE_REQUIRED = "L'image de fond est obligatoire.";
	public static final String BOARD_MODE_INVALID = "Choisis un plateau : fond, blanc ou trous.";
	public static final String BOARD_MODE_FOND = "fond";
	public static final String BOARD_MODE_BLANC = "blanc";
	public static final String BOARD_MODE_TROUS = "trous";
	public static final String BOARD_MODE_DEFAULT = BOARD_MODE_TROUS;
	public static final String IMAGE_NOT_WEBP = "L'image doit être au format WebP.";
	public static final String IMAGE_TOO_LARGE = "L'image dépasse 5 Mo.";
	public static final String PIECE_BOTH = "Une pièce ne peut pas être un texte et une image.";
	public static final String PIECE_EMPTY = "Chaque pièce est un texte ou une image.";
	public static final String SIMON_FACE = "Chaque élément est une image, une couleur, ou un texte.";
	public static final String ZONES_REQUIRED = "Il faut au moins une zone.";
	public static final String ZONE_NAME_REQUIRED = "Chaque zone doit avoir un nom.";
	public static final String ZONE_NAME_TOO_LONG = "Un nom de zone dépasse 200 caractères.";
	public static final String ZONE_SIDE_REQUIRED = "Chaque zone doit être marquée correcte ou incorrecte.";
	public static final String ZONE_RULE_REQUIRED = "Indique s'il faut cliquer toutes les zones correctes.";
	public static final String ZONE_CLICK_ORDER =
			"L'ordre de clic des zones correctes va de 1 à leur nombre, sans trou ni doublon. "
					+ "Une zone incorrecte n'a pas d'ordre.";
	public static final String ZONE_SHAPE = "La forme de la zone n'est pas reconnue.";
	public static final String ZONE_POINTS = "Les points de la zone ne restent pas sur l'image.";

	public static final String PUZZLE = "puzzle";
	public static final String PUZZLE_LABEL = "Puzzle";
	public static final int PUZZLE_ORDER = 10;
	public static final String PIECES_REQUIRED = "Il faut au moins deux pièces.";
	public static final String PIECE_NAME_REQUIRED = "Chaque pièce doit avoir un nom.";
	public static final String PIECE_NAME_TOO_LONG = "Un nom de pièce dépasse 200 caractères.";
	public static final String PIECE_SHAPE = "La forme de la pièce n'est pas reconnue.";
	public static final String PIECE_POINTS = "Les points de la pièce ne restent pas sur l'image.";
	public static final String PIECE_SLOT_REQUIRED =
			"Sur le cadre blanc, pose chaque pièce pour fixer son emplacement.";
	public static final String PIECE_SLOT_REQUIRED_FOND =
			"Sur le fond, pose chaque pièce à sa place pour fixer son emplacement.";
	public static final String PIECE_SLOT_WIDTH_RANGE =
			"La taille d'une pièce sur le fond doit rester entre 2 et 100 % de la largeur du plateau.";
	public static final String PIECE_SLOT_RANGE =
			"L'emplacement d'une pièce dépasse trop le plateau.";
	public static final String PIECE_SLOT_OFF_BOARD =
			"L'emplacement d'une pièce sort entièrement du plateau.";
	/** Magnétisme par défaut : 8 % de la largeur de l'image. */
	public static final double SNAP_DEFAULT = 0.08;
	/** Magnétisme maximum : 25 % de la largeur de l'image. */
	public static final double SNAP_MAX = 0.25;
	public static final String SNAP_RANGE =
			"Le magnétisme doit rester entre 0 et 25 % de la largeur de l'image.";

	public static final String SIMON = "simon";
	public static final String SIMON_LABEL = "Simon";
	/** Même rang que le classement : le catalogue départage par code. */
	public static final int SIMON_ORDER = 7;
	public static final String SIMON_COLORS = "couleurs";
	public static final String SIMON_NUMBERS = "nombres";
	public static final String SIMON_LETTERS = "lettres";
	public static final String SIMON_SHAPES = "formes";
	public static final String SIMON_TYPE_INVALID =
			"Choisis un genre d'éléments : couleurs, nombres, lettres ou formes.";
	public static final String SIMON_ELEMENTS_REQUIRED = "Il faut au moins deux éléments.";
	public static final String SIMON_ELEMENT_NAME_REQUIRED = "Chaque élément doit avoir un nom.";
	public static final String SIMON_ELEMENT_NAME_TOO_LONG = "Un nom d'élément dépasse 200 caractères.";
	public static final String SIMON_ELEMENT_DUPLICATE = "Deux éléments portent le même nom.";
	public static final String SIMON_NUMBER_REQUIRED =
			"Avec le genre nombres, chaque élément doit être un nombre.";
	public static final String SIMON_LETTER_REQUIRED =
			"Avec le genre lettres, chaque élément doit être une seule lettre.";
	public static final String SIMON_COLOR_REQUIRED =
			"Avec le genre couleurs, chaque élément doit avoir un code couleur comme #ff0000.";
	public static final String SIMON_COLOR_FORBIDDEN = "Seul le genre couleurs accepte un code couleur.";
	public static final String SIMON_COLOR_DUPLICATE = "Deux éléments ont le même code couleur.";
	public static final String SIMON_SEQUENCE_REQUIRED = "La suite doit avoir au moins deux pas.";
	public static final String SIMON_STEP_UNKNOWN = "Chaque pas de la suite doit désigner un élément de la liste.";

	public static final String CLONE_TARGET = "Choisis une matière.";
	public static final String CLONE_REQUIRED = "Choisis au moins une question.";
	public static final String DELETE_REQUIRED = "Choisis au moins une question à supprimer.";

	private final QuestionFormRegistry forms;

	private final TeacherSubjectPort teacherSubjectPort;
	private final TeacherQuestionPort teacherQuestionPort;
	private final ChildSubjectPort childSubjectPort;

	public TeacherQuestionService(
			TeacherSubjectPort teacherSubjectPort,
			TeacherQuestionPort teacherQuestionPort,
			ChildSubjectPort childSubjectPort,
			QuestionFormRegistry forms) {
		this.teacherSubjectPort = teacherSubjectPort;
		this.teacherQuestionPort = teacherQuestionPort;
		this.childSubjectPort = childSubjectPort;
		this.forms = forms;
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherQuestionFormsView listForms(long userId, String activeRole) {
		requireTeacher(activeRole);
		return new TeacherQuestionFormsView(forms.forms());
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherSubjectQuestionsView list(long userId, String activeRole, long subjectId, long afterId, int limit) {
		requireTeacher(activeRole);
		requireOwned(userId, subjectId);
		int capped = Math.min(100, Math.max(1, limit));
		List<StoredQuestion> fetched = teacherQuestionPort.findBySubject(subjectId, Math.max(0, afterId), capped + 1);
		boolean hasNext = fetched.size() > capped;
		List<StoredQuestion> page = hasNext ? fetched.subList(0, capped) : fetched;
		Long next = hasNext ? page.get(page.size() - 1).id() : null;
		return new TeacherSubjectQuestionsView(lines(page), next);
	}

	@Override
	@Transactional(readOnly = true)
	public QuestionLine get(long userId, String activeRole, long subjectId, long questionId) {
		requireTeacher(activeRole);
		requireOwned(userId, subjectId);
		StoredQuestion question = teacherQuestionPort.findInSubject(subjectId, questionId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, QUESTION_MISSING));
		return line(question);
	}

	@Override
	@Transactional
	public QuestionLine create(long userId, String activeRole, long subjectId, String formCode, String prompt,
			List<ChoiceInput> choices, List<PairInput> pairs, List<ItemInput> items) {
		return create(userId, activeRole, subjectId, formCode, prompt, new LegacyInput(choices, pairs, items));
	}

	@Override
	@Transactional
	public QuestionLine create(
			long userId,
			String activeRole,
			long subjectId,
			String formCode,
			String prompt,
			ContentInput content) {
		return create(userId, activeRole, subjectId, formCode, prompt, content, null);
	}

	@Override
	@Transactional
	public QuestionLine create(
			long userId,
			String activeRole,
			long subjectId,
			String formCode,
			String prompt,
			ContentInput content,
			GamificationInput gamification) {
		requireTeacher(activeRole);
		requireOwned(userId, subjectId);
		QuestionFormHandler handler = forms.require(formCode);
		FormLine form = handler.form();
		Prepared prepared = handler.prepare(prompt, content);
		QuestionGamification.Resolved resolved = resolve(gamification);
		teacherQuestionPort.ensureForm(subjectId, form.code(), form.label(), form.sortOrder());
		StoredQuestion saved = teacherQuestionPort.insert(
				subjectId,
				form.code(),
				prepared.prompt(),
				prepared.content());
		teacherQuestionPort.saveGamification(saved.id(), stored(resolved));
		return line(saved);
	}

	@Override
	@Transactional
	public QuestionLine update(long userId, String activeRole, long subjectId, long questionId, String formCode, String prompt,
			List<ChoiceInput> choices, List<PairInput> pairs, List<ItemInput> items) {
		return update(userId, activeRole, subjectId, questionId, formCode, prompt, new LegacyInput(choices, pairs, items));
	}

	@Override
	@Transactional
	public QuestionLine update(
			long userId,
			String activeRole,
			long subjectId,
			long questionId,
			String formCode,
			String prompt,
			ContentInput content) {
		return update(userId, activeRole, subjectId, questionId, formCode, prompt, content, null);
	}

	@Override
	@Transactional
	public QuestionLine update(
			long userId,
			String activeRole,
			long subjectId,
			long questionId,
			String formCode,
			String prompt,
			ContentInput content,
			GamificationInput gamification) {
		requireTeacher(activeRole);
		requireOwned(userId, subjectId);
		QuestionFormHandler handler = forms.require(formCode);
		FormLine form = handler.form();
		StoredQuestion current = teacherQuestionPort.findInSubject(subjectId, questionId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, QUESTION_MISSING));
		if (!form.code().equals(current.formCode())) {
			throw bad(FORM_LOCKED);
		}
		Prepared prepared = handler.prepare(prompt, content);
		QuestionGamification.Resolved resolved = gamification == null
				? keep(questionId)
				: resolve(gamification);
		StoredQuestion saved = teacherQuestionPort.replace(questionId, prepared.prompt(), prepared.content());
		teacherQuestionPort.saveGamification(saved.id(), stored(resolved));
		return line(saved);
	}

	/**
	 * Compare le mot composé au mot enregistré.
	 * Une autre forme, ou une question absente, ne donne pas de verdict.
	 */
	@Override
	@Transactional(readOnly = true)
	public LetterAttemptView tryLetterAnswer(
			long userId, String activeRole, long subjectId, long questionId, String attempt) {
		StoredQuestion current = requirePreviewQuestion(userId, activeRole, subjectId, questionId);
		if (!REPONSE_LIBRE.equals(current.formCode())) {
			throw bad(LETTER_FORM);
		}
		boolean accepted = LetterByLetterMatch.accepted(current.expectedAnswer(), attempt);
		return new LetterAttemptView(accepted, accepted ? LETTER_ACCEPTED : LETTER_REFUSED);
	}

	@Override
	@Transactional(readOnly = true)
	public ChildPlayQuestionView preview(long userId, String activeRole, long subjectId, long questionId) {
		StoredQuestion question = requirePreviewQuestion(userId, activeRole, subjectId, questionId);
		List<Integer> sequence = PlayableQuestionForm.SIMON.code().equals(question.formCode())
				? QuestionPlayEngine.drawSimon(question) : List.of();
		return QuestionPlayEngine.project(question, null, sequence, false);
	}

	@Override
	@Transactional(readOnly = true)
	public int previewMaxAttempts(long userId, String activeRole, long subjectId, long questionId) {
		requirePreviewQuestion(userId, activeRole, subjectId, questionId);
		return keep(questionId).maxAttempts();
	}

	@Override
	@Transactional(readOnly = true)
	public PreviewVerdict previewAnswer(long userId, String activeRole, long subjectId, long questionId,
			Submission submission, List<Integer> previewSequence) {
		StoredQuestion question = requirePreviewQuestion(userId, activeRole, subjectId, questionId);
		List<Integer> sequence = previewSequence == null ? List.of() : previewSequence;
		if (PlayableQuestionForm.SIMON.code().equals(question.formCode())) {
			int count = question.simonElements().size();
			if (sequence.size() != SimonSequenceDraw.length(count)
					|| sequence.stream().anyMatch(step -> step == null || step < 0 || step >= count)) {
				throw QuestionPlayEngine.invalidAnswer();
			}
		} else if (!sequence.isEmpty()) {
			throw QuestionPlayEngine.invalidAnswer();
		}
		ChildAnswerGrade grade = QuestionPlayEngine.grade(question, submission, sequence);
		return new PreviewVerdict(grade.scorePercent(), grade.passed());
	}

	@Override
	@Transactional(readOnly = true)
	public ChildLetterLocksView previewLetterLocks(
			long userId, String activeRole, long subjectId, long questionId, String attempt) {
		return QuestionPlayEngine.letterLocks(requirePreviewQuestion(userId, activeRole, subjectId, questionId), attempt);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean previewMemoryPair(long userId, String activeRole, long subjectId, long questionId, MemoryMatch match) {
		return QuestionPlayEngine.memoryPair(requirePreviewQuestion(userId, activeRole, subjectId, questionId), match);
	}

	private StoredQuestion requirePreviewQuestion(long userId, String activeRole, long subjectId, long questionId) {
		requireTeacher(activeRole);
		requireOwned(userId, subjectId);
		return teacherQuestionPort.findInSubject(subjectId, questionId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, QUESTION_MISSING));
	}

	@Override
	@Transactional
	public TeacherSubjectQuestionsView cloneQuestions(
			long userId,
			String activeRole,
			long sourceSubjectId,
			long targetSubjectId,
			List<Long> questionIds) {
		requireTeacher(activeRole);
		if (targetSubjectId <= 0) {
			throw bad(CLONE_TARGET);
		}
		List<Long> ids = selectedIds(questionIds);
		if (ids.isEmpty()) {
			throw bad(CLONE_REQUIRED);
		}
		requireOwned(userId, sourceSubjectId);
		requireOwned(userId, targetSubjectId);
		List<StoredQuestion> sources = new ArrayList<>();
		for (Long id : ids) {
			sources.add(teacherQuestionPort.findInSubject(sourceSubjectId, id)
					.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, QUESTION_MISSING)));
		}
		List<QuestionLine> copies = new ArrayList<>();
		for (StoredQuestion source : sources) {
			FormLine form = forms.require(source.formCode()).form();
			teacherQuestionPort.ensureForm(targetSubjectId, form.code(), form.label(), form.sortOrder());
			StoredQuestion saved = teacherQuestionPort.insert(
					targetSubjectId, form.code(), source.prompt(), draftOf(source));
			teacherQuestionPort.saveGamification(saved.id(), stored(keep(source.id())));
			copies.add(line(saved));
		}
		return new TeacherSubjectQuestionsView(List.copyOf(copies));
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherSubjectFollowersView followers(long userId, String activeRole, long subjectId) {
		requireTeacher(activeRole);
		StoredSubject subject = requireOwned(userId, subjectId);
		List<String> names = SubjectFollowers.names(
				childSubjectPort, subject.schoolId(), subject.grade(), subjectId);
		return new TeacherSubjectFollowersView(names.size(), followersWarning(names.size()), names);
	}

	@Override
	@Transactional
	public List<Long> deleteQuestions(long userId, String activeRole, long subjectId, List<Long> questionIds) {
		requireTeacher(activeRole);
		List<Long> ids = selectedIds(questionIds);
		if (ids.isEmpty()) {
			throw bad(DELETE_REQUIRED);
		}
		requireOwned(userId, subjectId);
		for (Long id : ids) {
			teacherQuestionPort.findInSubject(subjectId, id)
					.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, QUESTION_MISSING));
		}
		for (Long id : ids) {
			teacherQuestionPort.delete(id);
		}
		return ids;
	}

	/**
	 * Phrase affichée avant la suppression. Le nombre est celui des noms renvoyés.
	 */
	public static String followersWarning(int count) {
		if (count <= 0) {
			return "Aucun enfant ne suit ce cours.";
		}
		if (count == 1) {
			return "1 enfant suit ce cours.";
		}
		return count + " enfants suivent ce cours.";
	}

	private StoredSubject requireOwned(long userId, long subjectId) {
		return teacherSubjectPort.findOwned(userId, subjectId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, TeacherSubjectService.SUBJECT_MISSING));
	}

	private static <T> List<T> orEmpty(List<T> rows) {
		return rows == null ? List.of() : rows;
	}

	private List<QuestionLine> lines(List<StoredQuestion> stored) {
		if (stored == null || stored.isEmpty()) {
			return List.of();
		}
		List<QuestionLine> lines = new ArrayList<>();
		for (StoredQuestion question : stored) {
			lines.add(line(question));
		}
		return List.copyOf(lines);
	}

	private QuestionLine line(StoredQuestion question) {
		return new QuestionLine(question.id(), question.formCode(), forms.labelOf(question.formCode()),
				question.prompt(), question.content(), keep(question.id()));
	}

	private QuestionGamification.Resolved keep(long questionId) {
		TeacherQuestionPort.StoredGamification stored = teacherQuestionPort.gamificationOf(questionId);
		if (stored == null) {
			return QuestionGamification.defaults();
		}
		return QuestionGamification.resolve(
				stored.maxAttempts(),
				stored.difficulty(),
				stored.customizePoints(),
				stored.gamificationDisabled(),
				stored.customizePoints() ? stored.customPoints() : null);
	}

	private static QuestionGamification.Resolved resolve(GamificationInput input) {
		if (input == null) {
			return QuestionGamification.defaults();
		}
		return QuestionGamification.resolve(
				input.maxAttempts(),
				input.difficulty(),
				input.customizePoints(),
				input.gamificationDisabled(),
				input.points());
	}

	private static TeacherQuestionPort.StoredGamification stored(QuestionGamification.Resolved resolved) {
		return new TeacherQuestionPort.StoredGamification(
				resolved.maxAttempts(),
				resolved.difficulty(),
				resolved.customizePoints(),
				resolved.gamificationDisabled(),
				resolved.customizePoints() ? resolved.points() : List.of());
	}

	private static void requireTeacher(String activeRole) {
		if (!ActiveRoles.TEACHER.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceAccess.DENIED);
		}
	}

	private static AuthDomainException bad(String message) {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST, message);
	}

	/** Un id cité deux fois, ou vide, ne donne pas une deuxième copie. */
	private static List<Long> selectedIds(List<Long> questionIds) {
		if (questionIds == null || questionIds.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		for (Long id : questionIds) {
			if (id != null && id > 0) {
				ids.add(id);
			}
		}
		return List.copyOf(ids);
	}

	/**
	 * Reprend le contenu déjà enregistré, sans les id des lignes.
	 * Une forme nouvelle doit être ajoutée ici, sinon la copie est refusée.
	 */
	private static DraftContent draftOf(StoredQuestion source) {
		StoredContent content = source.content();
		if (content instanceof StoredChoices choices) {
			List<ChoiceDraft> drafts = new ArrayList<>();
			for (StoredChoice choice : choices.choices()) {
				drafts.add(new ChoiceDraft(choice.label(), choice.correct(), choice.imageWebp()));
			}
			return new ChoicesDraft(drafts);
		}
		if (content instanceof StoredPairs pairs) {
			List<PairDraft> drafts = new ArrayList<>();
			for (StoredPair pair : pairs.pairs()) {
				drafts.add(new PairDraft(
						pair.questionSide(), pair.answerSide(), pair.questionImageWebp(), pair.answerImageWebp()));
			}
			return new PairsDraft(drafts);
		}
		if (content instanceof StoredItems items) {
			List<ItemDraft> drafts = new ArrayList<>();
			for (StoredItem item : items.items()) {
				drafts.add(new ItemDraft(item.label(), item.axisLabel(), item.imageWebp()));
			}
			return new ItemsDraft(drafts, items.timelineEnabled());
		}
		if (content instanceof StoredStatements statements) {
			List<StatementDraft> drafts = new ArrayList<>();
			for (StoredStatement statement : statements.statements()) {
				drafts.add(new StatementDraft(statement.label(), statement.truthful(), statement.imageWebp()));
			}
			return new StatementsDraft(drafts);
		}
		if (content instanceof StoredLinks links) {
			List<LinkDraft> drafts = new ArrayList<>();
			for (StoredLink link : links.links()) {
				drafts.add(new LinkDraft(link.leftLabel(), link.rightLabel(), link.leftImageWebp(), link.rightImageWebp()));
			}
			return new LinksDraft(drafts);
		}
		if (content instanceof StoredClassification classification) {
			List<ColumnDraft> columns = new ArrayList<>();
			for (StoredColumn column : classification.columns()) {
				columns.add(new ColumnDraft(column.label()));
			}
			Map<Long, Integer> columnIndexById = new HashMap<>();
			List<StoredColumn> storedColumns = classification.columns();
			for (int i = 0; i < storedColumns.size(); i++) {
				columnIndexById.put(storedColumns.get(i).id(), i);
			}
			List<CardDraft> cards = new ArrayList<>();
			for (StoredCard card : classification.cards()) {
				Integer index = columnIndexById.get(card.correctColumnId());
				if (index == null) {
					throw new IllegalStateException("Carte sans colonne pour la copie : " + card.id());
				}
				cards.add(new CardDraft(card.label(), index, card.imageWebp()));
			}
			return new ClassificationDraft(columns, cards);
		}
		if (content instanceof StoredGaps gaps) {
			List<GapWordDraft> answers = new ArrayList<>();
			for (StoredGapWord answer : gaps.answers()) {
				answers.add(new GapWordDraft(answer.label()));
			}
			List<GapWordDraft> decoys = new ArrayList<>();
			for (StoredGapWord decoy : gaps.decoys()) {
				decoys.add(new GapWordDraft(decoy.label()));
			}
			return new GapsDraft(gaps.sentence(), answers, decoys);
		}
		if (content instanceof StoredFreeText text) {
			return new FreeTextDraft(text.expectedAnswer());
		}
		if (content instanceof StoredClickImage image) {
			List<ClickZoneDraft> zones = new ArrayList<>();
			for (StoredClickZone zone : image.zones()) {
				zones.add(new ClickZoneDraft(
						zone.label(), zone.correct(), zone.shape(), zone.points(), zone.clickOrder()));
			}
			return new ClickImageDraft(
					image.imageWebp(), image.requireAllCorrect(), image.clicksOrdered(), zones);
		}
		if (content instanceof StoredPuzzle puzzle) {
			List<PuzzlePieceDraft> pieces = new ArrayList<>();
			for (StoredPuzzlePiece piece : puzzle.pieces()) {
				pieces.add(new PuzzlePieceDraft(
						piece.label(),
						piece.shape(),
						piece.points(),
						piece.slotX(),
						piece.slotY(),
						piece.slotWidth()));
			}
			return new PuzzleDraft(
					puzzle.imageWebp(),
					puzzle.boardMode(),
					puzzle.boardImageWebp(),
					puzzle.snapDistance(),
					pieces);
		}
		if (content instanceof StoredSimon simon) {
			List<SimonElementDraft> elements = new ArrayList<>();
			for (StoredSimonElement element : simon.elements()) {
				elements.add(new SimonElementDraft(element.label(), element.color(), element.imageWebp()));
			}
			return new SimonDraft(simon.elementType(), elements);
		}
		throw new IllegalStateException("Copie non prévue pour la forme : " + source.formCode());
	}

}

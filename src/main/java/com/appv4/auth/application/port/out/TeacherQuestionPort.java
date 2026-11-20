package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Énoncés d'une matière et la forme qui leur est attachée (SCRUM-174).
 *
 * <p><b>Business :</b> une question reste sur la matière. La partager ou la
 * déplacer ne la copie pas : même id de matière, mêmes questions.
 * <p><b>Technique :</b> {@code back_subject_questions}, {@code back_subject_question_choices},
 * {@code back_subject_question_pairs}, {@code back_subject_question_items},
 * {@code back_subject_question_statements}, {@code back_subject_question_texts},
 * {@code back_subject_question_click_images}, {@code back_subject_question_click_zones},
 * {@code back_subject_question_simon_options}, {@code _simon_elements},
 * et une ligne {@code back_subject_question_forms} à la première question de cette forme.
 * <p><b>Métier / flow :</b> port out de {@code /api/int/teacher/subjects/{id}/questions}.
 */
public interface TeacherQuestionPort {

	List<StoredQuestion> findBySubject(long subjectId, long afterId, int limit);

	List<QuestionSummary> findSummariesBySubject(long subjectId, long afterId, int limit);

	Optional<Long> firstQuestionIdByForm(long subjectId, Set<String> formCodes);

	record QuestionSummary(long id, String formCode, String prompt) {
	}

	/**
	 * Total de questions par matière.
	 *
	 * <p><b>Business :</b> le professeur et le parent voient ce nombre sur la liste
	 * des matières, sans ouvrir chaque matière.
	 * <p><b>Technique :</b> une entrée absente de la map vaut 0. Les id inconnus
	 * ne sont pas inventés : le service lit {@code getOrDefault(id, 0L)}.
	 * <p><b>Métier / flow :</b> alimente {@code questionCount} sur les listes
	 * {@code /teacher/subjects} (matières affichées et cours déjà créés)
	 * et {@code /parent/subjects}.
	 */
	Map<Long, Long> countBySubjectIds(List<Long> subjectIds);

	/**
	 * Enregistre l'énoncé et les propositions déjà nettoyées (non vides, au moins deux,
	 * au moins une juste). L'ordre de {@code choices} est l'ordre saisi.
	 */
	StoredQuestion insert(
			long subjectId,
			String formCode,
			String prompt,
			DraftContent content);

	/**
	 * La question de cette matière, ou vide si l'id n'y est pas.
	 * Une question d'une autre matière compte comme absente.
	 */
	Optional<StoredQuestion> findInSubject(long subjectId, long questionId);

	/**
	 * Remplace l'énoncé et le contenu. La forme et la matière ne bougent pas.
	 * Les anciennes lignes sont retirées : les nouvelles portent d'autres id.
	 */
	StoredQuestion replace(
			long questionId,
			String prompt,
			DraftContent content);

	/**
	 * Note la forme sur la matière si elle n'y est pas encore.
	 * Une deuxième question du même code ne crée pas une deuxième ligne.
	 */
	void ensureForm(long subjectId, String formCode, String label, int sortOrder);

	/**
	 * Retire la question et son contenu. La matière et les autres questions restent.
	 * L'appelant a déjà vérifié que la question est dans la matière du professeur.
	 */
	void delete(long questionId);

	record ChoiceDraft(String label, boolean correct, byte[] imageWebp) {
		public ChoiceDraft(String label, boolean correct) {
			this(label, correct, null);
		}
	}

	/** Les deux faces d'une paire Memory, déjà non vides. */
	record PairDraft(String questionSide, String answerSide, byte[] questionImageWebp, byte[] answerImageWebp) {
		public PairDraft(String questionSide, String answerSide) {
			this(questionSide, answerSide, null, null);
		}
	}

	/**
	 * Texte d'un élément Ordre, déjà non vide. L'ordre de la liste est l'ordre saisi.
	 * {@code axisLabel} est le jalon de frise (null si timeline désactivée).
	 */
	record ItemDraft(String label, String axisLabel, byte[] imageWebp) {
		public ItemDraft(String label, String axisLabel) {
			this(label, axisLabel, null);
		}

		public ItemDraft(String label) {
			this(label, null, null);
		}
	}

	/** Phrase Vrai ou faux déjà non vide. {@code truthful} dit si la phrase est vraie. */
	record StatementDraft(String label, boolean truthful, byte[] imageWebp) {
		public StatementDraft(String label, boolean truthful) {
			this(label, truthful, null);
		}
	}

	/** Mot de gauche et réponse de droite d'un lien, déjà non vides. */
	record LinkDraft(String leftLabel, String rightLabel, byte[] leftImageWebp, byte[] rightImageWebp) {
		public LinkDraft(String leftLabel, String rightLabel) {
			this(leftLabel, rightLabel, null, null);
		}
	}

	/** Colonne d'un classement, déjà non vide. */
	record ColumnDraft(String label) {
	}

	/**
	 * Carte d'un classement, déjà non vide.
	 * {@code columnIndex} pointe la colonne juste dans le brouillon colonnes.
	 */
	record CardDraft(String label, int columnIndex, byte[] imageWebp) {
		public CardDraft(String label, int columnIndex) {
			this(label, columnIndex, null);
		}
	}

	/** Mot juste ou leurre d'une case vide, déjà non vide. */
	record GapWordDraft(String label) {
	}

	/** Réponse attendue d'une question libre, déjà non vide. */
	record FreeTextDraft(String expectedAnswer) implements DraftContent {
	}

	/** Point d'une zone, entre 0 et 1, en proportion de l'image. */
	record ClickPoint(double x, double y) {
	}

	/**
	 * Zone déjà nommée, juste ou non, avec sa forme.
	 * {@code clickOrder} est le rang du clic (1, 2, 3…) si la zone est juste, sinon null.
	 */
	record ClickZoneDraft(String label, boolean correct, String shape, List<ClickPoint> points, Integer clickOrder) {
		public ClickZoneDraft {
			points = List.copyOf(points);
		}
	}

	/**
	 * Image WebP déjà allégée, et les zones qui restent collées à ce dessin.
	 * {@code clicksOrdered} true : les zones justes portent un rang. false : aucun rang.
	 */
	record ClickImageDraft(
			byte[] imageWebp, boolean requireAllCorrect, boolean clicksOrdered, List<ClickZoneDraft> zones)
			implements DraftContent {
		public ClickImageDraft(byte[] imageWebp, boolean requireAllCorrect, List<ClickZoneDraft> zones) {
			this(imageWebp, requireAllCorrect, true, zones);
		}

		public ClickImageDraft {
			imageWebp = imageWebp.clone();
			zones = List.copyOf(zones);
		}
	}

	record StoredChoice(long id, String label, boolean correct, byte[] imageWebp) {
		public StoredChoice(long id, String label, boolean correct) {
			this(id, label, correct, null);
		}
	}

	record StoredPair(long id, String questionSide, String answerSide, byte[] questionImageWebp, byte[] answerImageWebp) {
		public StoredPair(long id, String questionSide, String answerSide) {
			this(id, questionSide, answerSide, null, null);
		}
	}

	record StoredItem(long id, String label, String axisLabel, byte[] imageWebp) {
		public StoredItem(long id, String label, String axisLabel) {
			this(id, label, axisLabel, null);
		}

		public StoredItem(long id, String label) {
			this(id, label, null, null);
		}
	}

	record StoredStatement(long id, String label, boolean truthful, byte[] imageWebp) {
		public StoredStatement(long id, String label, boolean truthful) {
			this(id, label, truthful, null);
		}
	}

	record StoredLink(long id, String leftLabel, String rightLabel, byte[] leftImageWebp, byte[] rightImageWebp) {
		public StoredLink(long id, String leftLabel, String rightLabel) {
			this(id, leftLabel, rightLabel, null, null);
		}
	}

	record StoredColumn(long id, String label) {
	}

	record StoredCard(long id, String label, long correctColumnId, byte[] imageWebp) {
		public StoredCard(long id, String label, long correctColumnId) {
			this(id, label, correctColumnId, null);
		}
	}

	record StoredGapWord(long id, String label) {
	}

	/** Chaque nouvelle structure fournit son propre type, sans agrandir toutes les signatures. */
	interface DraftContent {
	}

	interface StoredContent {
	}

	record ChoicesDraft(List<ChoiceDraft> choices) implements DraftContent {
		public ChoicesDraft {
			choices = List.copyOf(choices);
		}
	}

	record PairsDraft(List<PairDraft> pairs) implements DraftContent {
		public PairsDraft {
			pairs = List.copyOf(pairs);
		}
	}

	record ItemsDraft(List<ItemDraft> items, boolean timelineEnabled) implements DraftContent {
		public ItemsDraft {
			items = List.copyOf(items);
		}

		public ItemsDraft(List<ItemDraft> items) {
			this(items, false);
		}
	}

	record StatementsDraft(List<StatementDraft> statements) implements DraftContent {
		public StatementsDraft {
			statements = List.copyOf(statements);
		}
	}

	record LinksDraft(List<LinkDraft> links) implements DraftContent {
		public LinksDraft {
			links = List.copyOf(links);
		}
	}

	record ClassificationDraft(List<ColumnDraft> columns, List<CardDraft> cards) implements DraftContent {
		public ClassificationDraft {
			columns = List.copyOf(columns);
			cards = List.copyOf(cards);
		}
	}

	record GapsDraft(String sentence, List<GapWordDraft> answers, List<GapWordDraft> decoys) implements DraftContent {
		public GapsDraft {
			answers = List.copyOf(answers);
			decoys = List.copyOf(decoys);
		}
	}

	record StoredChoices(List<StoredChoice> choices) implements StoredContent {
		public StoredChoices {
			choices = List.copyOf(choices);
		}
	}

	record StoredPairs(List<StoredPair> pairs) implements StoredContent {
		public StoredPairs {
			pairs = List.copyOf(pairs);
		}
	}

	record StoredItems(List<StoredItem> items, boolean timelineEnabled) implements StoredContent {
		public StoredItems {
			items = List.copyOf(items);
		}

		public StoredItems(List<StoredItem> items) {
			this(items, false);
		}
	}

	record StoredStatements(List<StoredStatement> statements) implements StoredContent {
		public StoredStatements {
			statements = List.copyOf(statements);
		}
	}

	record StoredLinks(List<StoredLink> links) implements StoredContent {
		public StoredLinks {
			links = List.copyOf(links);
		}
	}

	record StoredClassification(List<StoredColumn> columns, List<StoredCard> cards) implements StoredContent {
		public StoredClassification {
			columns = List.copyOf(columns);
			cards = List.copyOf(cards);
		}
	}

	record StoredGaps(String sentence, List<StoredGapWord> answers, List<StoredGapWord> decoys)
			implements StoredContent {
		public StoredGaps {
			answers = List.copyOf(answers);
			decoys = List.copyOf(decoys);
		}
	}

	/** Réponse attendue telle que le professeur l'a enregistrée. */
	record StoredFreeText(String expectedAnswer) implements StoredContent {
	}

	/** Zone relue, avec l'id de la ligne et le rang de clic s'il y en a un. */
	record StoredClickZone(
			long id, String label, boolean correct, String shape, List<ClickPoint> points, Integer clickOrder) {
		public StoredClickZone {
			points = List.copyOf(points);
		}
	}

	/** Image et zones telles que le professeur les a enregistrées. */
	record StoredClickImage(
			byte[] imageWebp, boolean requireAllCorrect, boolean clicksOrdered, List<StoredClickZone> zones)
			implements StoredContent {
		public StoredClickImage(byte[] imageWebp, boolean requireAllCorrect, List<StoredClickZone> zones) {
			this(imageWebp, requireAllCorrect, true, zones);
		}

		public StoredClickImage {
			imageWebp = imageWebp.clone();
			zones = List.copyOf(zones);
		}
	}

	/**
	 * Pièce déjà nommée, sans côté juste ou faux.
	 * {@code slotX}/{@code slotY} : emplacement cible sur le plateau (modes blanc et fond), sinon null.
	 * {@code slotWidth} : largeur sur le fond (mode fond), null = taille de la découpe.
	 */
	record PuzzlePieceDraft(
			String label,
			String shape,
			List<ClickPoint> points,
			Double slotX,
			Double slotY,
			Double slotWidth) {
		public PuzzlePieceDraft {
			points = List.copyOf(points);
		}
	}

	/**
	 * Image de découpe, mode de plateau, fond optionnel, magnétisme, et pièces.
	 * {@code snapDistance} est une fraction de la largeur : 0,08 veut dire 8 %.
	 * {@code boardImageWebp} est null hors mode {@code fond}.
	 */
	record PuzzleDraft(
			byte[] imageWebp,
			String boardMode,
			byte[] boardImageWebp,
			double snapDistance,
			List<PuzzlePieceDraft> pieces)
			implements DraftContent {
		public PuzzleDraft {
			imageWebp = imageWebp.clone();
			boardImageWebp = boardImageWebp == null ? null : boardImageWebp.clone();
			pieces = List.copyOf(pieces);
		}
	}

	/**
	 * Pièce relue, avec l'id de la ligne. Slots null en mode trous, et sur les anciens fonds
	 * enregistrés avant la pose sur le fond (la cible reste alors la découpe).
	 */
	record StoredPuzzlePiece(
			long id,
			String label,
			String shape,
			List<ClickPoint> points,
			Double slotX,
			Double slotY,
			Double slotWidth) {
		public StoredPuzzlePiece {
			points = List.copyOf(points);
		}
	}

	/** Découpe, mode de plateau, fond optionnel, magnétisme et pièces enregistrés. */
	record StoredPuzzle(
			byte[] imageWebp,
			String boardMode,
			byte[] boardImageWebp,
			double snapDistance,
			List<StoredPuzzlePiece> pieces)
			implements StoredContent {
		public StoredPuzzle {
			imageWebp = imageWebp.clone();
			boardImageWebp = boardImageWebp == null ? null : boardImageWebp.clone();
			pieces = List.copyOf(pieces);
		}
	}

	/**
	 * Élément d'un Simon, déjà nommé et du genre choisi.
	 * {@code color} est un code {@code #rrggbb} en minuscules pour le genre couleurs, sinon null.
	 */
	record SimonElementDraft(String label, String color, byte[] imageWebp) {
		public SimonElementDraft(String label, String color) {
			this(label, color, null);
		}
	}

	/**
	 * Genre des éléments et éléments dans l'ordre saisi. Pas de suite : elle est tirée
	 * pour l'enfant à chaque ouverture (BACK-55).
	 */
	record SimonDraft(String elementType, List<SimonElementDraft> elements) implements DraftContent {
		public SimonDraft {
			elements = List.copyOf(elements);
		}
	}

	/** Élément de Simon relu, avec l'id de la ligne. */
	record StoredSimonElement(long id, String label, String color, byte[] imageWebp) {
		public StoredSimonElement(long id, String label, String color) {
			this(id, label, color, null);
		}
	}

	/** Genre et éléments tels que le professeur les a enregistrés. */
	record StoredSimon(String elementType, List<StoredSimonElement> elements) implements StoredContent {
		public StoredSimon {
			elements = List.copyOf(elements);
		}
	}

	record StoredQuestion(
			long id,
			long subjectId,
			String formCode,
			String prompt,
			StoredContent content) {

		/** Compatibilité avec les trois formes du contrat HTTP existant. */
		public StoredQuestion(long id, long subjectId, String formCode, String prompt,
				List<StoredChoice> choices, List<StoredPair> pairs, List<StoredItem> items) {
			this(id, subjectId, formCode, prompt, switch (formCode) {
				case "memory" -> new StoredPairs(pairs);
				case "order" -> new StoredItems(items);
				default -> new StoredChoices(choices);
			});
		}

		public List<StoredChoice> choices() {
			return content instanceof StoredChoices value ? value.choices() : List.of();
		}

		public List<StoredPair> pairs() {
			return content instanceof StoredPairs value ? value.pairs() : List.of();
		}

		public List<StoredItem> items() {
			return content instanceof StoredItems value ? value.items() : List.of();
		}

		public List<StoredStatement> statements() {
			return content instanceof StoredStatements value ? value.statements() : List.of();
		}

		public List<StoredLink> links() {
			return content instanceof StoredLinks value ? value.links() : List.of();
		}

		public List<StoredColumn> columns() {
			return content instanceof StoredClassification value ? value.columns() : List.of();
		}

		public List<StoredCard> cards() {
			return content instanceof StoredClassification value ? value.cards() : List.of();
		}

		public String sentence() {
			return content instanceof StoredGaps value ? value.sentence() : "";
		}

		public List<StoredGapWord> answers() {
			return content instanceof StoredGaps value ? value.answers() : List.of();
		}

		public List<StoredGapWord> decoys() {
			return content instanceof StoredGaps value ? value.decoys() : List.of();
		}

		public String expectedAnswer() {
			return content instanceof StoredFreeText value ? value.expectedAnswer() : "";
		}

		public byte[] imageWebp() {
			if (content instanceof StoredClickImage click) {
				return click.imageWebp();
			}
			if (content instanceof StoredPuzzle puzzle) {
				return puzzle.imageWebp();
			}
			return new byte[0];
		}

		public Boolean requireAllCorrect() {
			return content instanceof StoredClickImage value ? value.requireAllCorrect() : null;
		}

		/** Clics ordonnés. {@code null} hors image à cliquer. */
		public Boolean clicksOrdered() {
			return content instanceof StoredClickImage value ? value.clicksOrdered() : null;
		}

		/** Frise Ordre active. {@code null} hors forme {@code order}. */
		public Boolean timelineEnabled() {
			return content instanceof StoredItems value ? value.timelineEnabled() : null;
		}

		public List<StoredClickZone> clickZones() {
			return content instanceof StoredClickImage value ? value.zones() : List.of();
		}

		/** Mode de plateau puzzle. {@code null} hors forme {@code puzzle}. */
		public String boardMode() {
			return content instanceof StoredPuzzle value ? value.boardMode() : null;
		}

		/** Fond dédié (mode fond). Vide hors puzzle fond. */
		public byte[] boardImageWebp() {
			if (!(content instanceof StoredPuzzle puzzle) || puzzle.boardImageWebp() == null) {
				return new byte[0];
			}
			return puzzle.boardImageWebp();
		}

		/** Magnétisme puzzle (fraction de largeur). {@code null} hors puzzle. */
		public Double snapDistance() {
			return content instanceof StoredPuzzle value ? value.snapDistance() : null;
		}

		public List<StoredPuzzlePiece> puzzlePieces() {
			return content instanceof StoredPuzzle value ? value.pieces() : List.of();
		}

		/** Genre des éléments Simon. {@code null} hors forme {@code simon}. */
		public String simonElementType() {
			return content instanceof StoredSimon value ? value.elementType() : null;
		}

		public List<StoredSimonElement> simonElements() {
			return content instanceof StoredSimon value ? value.elements() : List.of();
		}

		/** La suite Simon n'est plus stockée sur la question : elle est tirée à la partie. */
		public List<Integer> simonSequence() {
			return List.of();
		}
	}

	/**
	 * Réglage enregistré. {@code customPoints} n'est rempli que si le professeur les a saisis.
	 */
	record StoredGamification(
			int maxAttempts,
			String difficulty,
			boolean customizePoints,
			boolean gamificationDisabled,
			List<Integer> customPoints) {
	}

	/** Absent en test : le service reprend alors le défaut (1 essai, Facile). */
	default StoredGamification gamificationOf(long questionId) {
		return null;
	}

	default void saveGamification(long questionId, StoredGamification gamification) {
	}
}

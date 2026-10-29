package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Énoncés d'une matière et la forme qui leur est attachée (SCRUM-174).
 *
 * <p><b>Business :</b> une question reste sur la matière. La partager ou la
 * déplacer ne la copie pas : même id de matière, mêmes questions.
 * <p><b>Technique :</b> {@code back_subject_questions}, {@code back_subject_question_choices},
 * {@code back_subject_question_pairs}, {@code back_subject_question_items},
 * {@code back_subject_question_statements}, {@code back_subject_question_texts},
 * {@code back_subject_question_click_images}, {@code back_subject_question_click_zones},
 * et une ligne {@code back_subject_question_forms} à la première question de cette forme.
 * <p><b>Métier / flow :</b> port out de {@code /api/int/teacher/subjects/{id}/questions}.
 */
public interface TeacherQuestionPort {

	List<StoredQuestion> findBySubject(long subjectId);

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

	record ChoiceDraft(String label, boolean correct) {
	}

	/** Les deux faces d'une paire Memory, déjà non vides. */
	record PairDraft(String questionSide, String answerSide) {
	}

	/** Texte d'un élément Ordre, déjà non vide. L'ordre de la liste est l'ordre saisi. */
	record ItemDraft(String label) {
	}

	/** Phrase Vrai ou faux déjà non vide. {@code truthful} dit si la phrase est vraie. */
	record StatementDraft(String label, boolean truthful) {
	}

	/** Mot de gauche et réponse de droite d'un lien, déjà non vides. */
	record LinkDraft(String leftLabel, String rightLabel) {
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

	/** Zone déjà nommée, juste ou non, avec sa forme. */
	record ClickZoneDraft(String label, boolean correct, String shape, List<ClickPoint> points) {
		public ClickZoneDraft {
			points = List.copyOf(points);
		}
	}

	/** Image WebP déjà allégée, et les zones qui restent collées à ce dessin. */
	record ClickImageDraft(byte[] imageWebp, boolean requireAllCorrect, List<ClickZoneDraft> zones)
			implements DraftContent {
		public ClickImageDraft {
			imageWebp = imageWebp.clone();
			zones = List.copyOf(zones);
		}
	}

	record StoredChoice(long id, String label, boolean correct) {
	}

	record StoredPair(long id, String questionSide, String answerSide) {
	}

	record StoredItem(long id, String label) {
	}

	record StoredStatement(long id, String label, boolean truthful) {
	}

	record StoredLink(long id, String leftLabel, String rightLabel) {
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

	record ItemsDraft(List<ItemDraft> items) implements DraftContent {
		public ItemsDraft {
			items = List.copyOf(items);
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

	record StoredItems(List<StoredItem> items) implements StoredContent {
		public StoredItems {
			items = List.copyOf(items);
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

	/** Zone relue, avec l'id de la ligne. */
	record StoredClickZone(long id, String label, boolean correct, String shape, List<ClickPoint> points) {
		public StoredClickZone {
			points = List.copyOf(points);
		}
	}

	/** Image et zones telles que le professeur les a enregistrées. */
	record StoredClickImage(byte[] imageWebp, boolean requireAllCorrect, List<StoredClickZone> zones)
			implements StoredContent {
		public StoredClickImage {
			imageWebp = imageWebp.clone();
			zones = List.copyOf(zones);
		}
	}

	/** Pièce déjà nommée, sans côté juste ou faux. */
	record PuzzlePieceDraft(String label, String shape, List<ClickPoint> points) {
		public PuzzlePieceDraft {
			points = List.copyOf(points);
		}
	}

	/** Image WebP déjà allégée, et les pièces qui restent collées à ce dessin. */
	record PuzzleDraft(byte[] imageWebp, List<PuzzlePieceDraft> pieces) implements DraftContent {
		public PuzzleDraft {
			imageWebp = imageWebp.clone();
			pieces = List.copyOf(pieces);
		}
	}

	/** Pièce relue, avec l'id de la ligne. */
	record StoredPuzzlePiece(long id, String label, String shape, List<ClickPoint> points) {
		public StoredPuzzlePiece {
			points = List.copyOf(points);
		}
	}

	/** Image et pièces telles que le professeur les a enregistrées. */
	record StoredPuzzle(byte[] imageWebp, List<StoredPuzzlePiece> pieces) implements StoredContent {
		public StoredPuzzle {
			imageWebp = imageWebp.clone();
			pieces = List.copyOf(pieces);
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

		public List<StoredClickZone> clickZones() {
			return content instanceof StoredClickImage value ? value.zones() : List.of();
		}
	}
}

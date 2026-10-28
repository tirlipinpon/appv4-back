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

	record ChoiceDraft(String label, boolean correct) {
	}

	/** Les deux faces d'une paire Memory, déjà non vides. */
	record PairDraft(String questionSide, String answerSide) {
	}

	/** Texte d'un élément Ordre, déjà non vide. L'ordre de la liste est l'ordre saisi. */
	record ItemDraft(String label) {
	}

	record StoredChoice(long id, String label, boolean correct) {
	}

	record StoredPair(long id, String questionSide, String answerSide) {
	}

	record StoredItem(long id, String label) {
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
	}
}

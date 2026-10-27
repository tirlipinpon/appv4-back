package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Énoncés d'une matière et la forme qui leur est attachée (SCRUM-174).
 *
 * <p><b>Business :</b> une question reste sur la matière. La partager ou la
 * déplacer ne la copie pas : même id de matière, mêmes questions.
 * <p><b>Technique :</b> {@code back_subject_questions}, {@code back_subject_question_choices},
 * et une ligne {@code back_subject_question_forms} à la première question de cette forme.
 * <p><b>Métier / flow :</b> port out de {@code /api/int/teacher/subjects/{id}/questions}.
 */
public interface TeacherQuestionPort {

	List<StoredQuestion> findBySubject(long subjectId);

	/**
	 * Enregistre l'énoncé et les propositions déjà nettoyées (non vides, au moins deux,
	 * au moins une juste). L'ordre de {@code choices} est l'ordre saisi.
	 */
	StoredQuestion insert(long subjectId, String formCode, String prompt, List<ChoiceDraft> choices);

	/**
	 * La question de cette matière, ou vide si l'id n'y est pas.
	 * Une question d'une autre matière compte comme absente.
	 */
	Optional<StoredQuestion> findInSubject(long subjectId, long questionId);

	/**
	 * Remplace l'énoncé et les propositions. La forme et la matière ne bougent pas.
	 * Les anciennes propositions sont retirées : les nouvelles portent d'autres id.
	 */
	StoredQuestion replace(long questionId, String prompt, List<ChoiceDraft> choices);

	/**
	 * Note la forme sur la matière si elle n'y est pas encore.
	 * Une deuxième question du même code ne crée pas une deuxième ligne.
	 */
	void ensureForm(long subjectId, String formCode, String label, int sortOrder);

	record ChoiceDraft(String label, boolean correct) {
	}

	record StoredChoice(long id, String label, boolean correct) {
	}

	record StoredQuestion(long id, long subjectId, String formCode, String prompt, List<StoredChoice> choices) {
	}
}

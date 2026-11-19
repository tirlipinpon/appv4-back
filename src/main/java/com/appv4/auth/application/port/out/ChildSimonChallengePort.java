package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Suite Simon tirée pour un enfant et une question (BACK-55).
 *
 * <p><b>Business :</b> le professeur ne choisit pas la suite. Chaque fois que l'enfant ouvre
 * un Simon, le serveur en tire une nouvelle ; sa reprise est notée contre celle-là, jamais
 * contre une suite que l'écran enverrait lui-même.
 * <p><b>Technique :</b> une ligne par (enfant, question) dans {@code back_child_simon_challenges}.
 * Une nouvelle ouverture remplace la suite ; la note la retire.
 * <p><b>Métier / flow :</b> GET play → {@link #save} ; POST attempts → {@link #find}, puis
 * {@link #delete} une fois la tentative notée.
 */
public interface ChildSimonChallengePort {

	/** Garde la suite montrée à l'enfant, à la place de la précédente pour cette question. */
	void save(long childId, long questionId, List<Integer> sequence);

	/** Suite montrée à la dernière ouverture ; vide si l'enfant n'a pas ouvert la question ou a déjà répondu. */
	Optional<List<Integer>> find(long childId, long questionId);

	void delete(long childId, long questionId);
}

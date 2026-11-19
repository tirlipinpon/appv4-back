package com.appv4.auth.application.result;

import java.util.List;

/**
 * Verrous case par case pour la saisie lettre par lettre.
 *
 * <p><b>Business :</b> l'enfant voit en vert les lettres déjà justes ; elles ne
 * se modifient plus. Les espaces du mot sont toujours verrouillés (saut).
 * <p><b>Technique :</b> réponse POST letter-locks, sans tentative enregistrée.
 * <p><b>Métier / flow :</b> Learn saisit une lettre → ce payload → cases figées.
 */
public record ChildLetterLocksView(List<Boolean> locks) {

	public ChildLetterLocksView {
		locks = locks == null ? List.of() : List.copyOf(locks);
	}
}

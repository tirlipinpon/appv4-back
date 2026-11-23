package com.appv4.auth.application.port.in;

import java.util.List;

import com.appv4.auth.application.port.out.HumeurChoicePort.HumeurChoice;

/**
 * Tenir la bibliothèque des humeurs (BACK-68).
 *
 * <p><b>Business :</b> seul l'admin compose les choix que l'enfant pourra voir.
 * <p><b>Technique :</b> le rôle admin est relu en base. Un choix vide est refusé.
 * <p><b>Métier / flow :</b> page admin → ce port → persistance. Le front n'ajoute rien.
 */
public interface HumeurLibraryUseCase {

	List<HumeurChoice> list(long callerId);

	HumeurChoice create(long callerId, String icon, String word);

	HumeurChoice update(long callerId, long choiceId, String icon, String word);

	void delete(long callerId, long choiceId);
}

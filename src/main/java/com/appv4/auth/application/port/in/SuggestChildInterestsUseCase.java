package com.appv4.auth.application.port.in;

import java.util.List;

/**
 * Propose des centres d'intérêt déjà écrits par d'autres parents.
 *
 * <p><b>Business :</b> le parent retrouve une orthographe déjà utilisée, sans savoir par qui.
 * <p><b>Technique :</b> au plus huit mots, préfixe d'au moins deux caractères.
 * <p><b>Métier / flow :</b> port in de {@code GET /api/int/parent/interest-suggestions}.
 */
public interface SuggestChildInterestsUseCase {

	List<String> suggest(String activeRole, String query);
}

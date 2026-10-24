package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.SchoolCatalogPort;

/**
 * Référentiel d'écoles encore vide (SCRUM-159, en attendant SCRUM-163).
 *
 * <p><b>Business :</b> aucune école n'est inventée pour débloquer l'écran.
 * Le professeur peut quand même enregistrer son nom.
 * <p><b>Technique :</b> liste vide, pas de table {@code back_schools}.
 * SCRUM-163 supprime ce bean et lit l'annuaire. Deux beans {@link SchoolCatalogPort}
 * ne peuvent pas coexister.
 * <p><b>Métier / flow :</b> adapter out du port référentiel, appelé par le profil professeur.
 */
@Component
public class EmptySchoolCatalogAdapter implements SchoolCatalogPort {

	@Override
	public List<CatalogSchool> list() {
		return List.of();
	}
}

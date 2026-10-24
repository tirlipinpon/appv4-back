package com.appv4.auth.application.port.out;

import java.util.List;

/**
 * Référentiel des écoles qu'on peut rattacher à un professeur (SCRUM-159).
 *
 * <p><b>Business :</b> le professeur choisit une école dans une liste fermée.
 * Tant que la liste est vide, il ne peut pas en inventer une.
 * <p><b>Technique :</b> aujourd'hui l'adaptateur renvoie une liste vide.
 * SCRUM-163 le remplace par la lecture de {@code back_schools} (id BIGINT, nom, pays).
 * <p><b>Métier / flow :</b> port out du profil professeur. Le front n'écrit pas d'école.
 */
public interface SchoolCatalogPort {

	List<CatalogSchool> list();

	/**
	 * @param id   identifiant stable, le même que {@code back_schools.id} une fois la liste là
	 * @param name nom affiché, tel que le référentiel le donne
	 */
	record CatalogSchool(long id, String name) {
	}
}

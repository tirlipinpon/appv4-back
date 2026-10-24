package com.appv4.auth.application.port.out;

import java.util.List;

/**
 * Référentiel des écoles qu'on peut rattacher à un professeur (SCRUM-159).
 *
 * <p><b>Business :</b> le professeur choisit une école dans une liste fermée.
 * Tant que la liste est vide, il ne peut pas en inventer une.
 * <p><b>Technique :</b> lecture de {@code back_schools} : id, nom, pays et types d'enseignement.
 * Le pays choisit les noms de classes. Les types disent quels niveaux l'école a.
 * <p><b>Métier / flow :</b> port out du profil professeur. Le front n'écrit pas d'école.
 */
public interface SchoolCatalogPort {

	List<CatalogSchool> list();

	/**
	 * @param id            identifiant stable, le même que {@code back_schools.id}
	 * @param name          nom affiché, tel que le référentiel le donne
	 * @param country       pays, par exemple Belgique
	 * @param teachingTypes types d'enseignement recopiés du fichier, une seule chaîne
	 */
	record CatalogSchool(long id, String name, String country, String teachingTypes) {
	}
}

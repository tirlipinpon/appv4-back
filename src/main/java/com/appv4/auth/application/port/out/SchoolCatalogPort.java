package com.appv4.auth.application.port.out;

import java.util.List;

/**
 * Référentiel des écoles qu'on peut rattacher à un professeur (SCRUM-159).
 *
 * <p><b>Business :</b> le professeur choisit une école dans une liste fermée.
 * Tant que la liste est vide, il ne peut pas en inventer une.
 * <p><b>Technique :</b> lecture de {@code back_schools} : id, nom, pays, types d'enseignement
 * et code postal. Le pays choisit les noms de classes. Les types disent quels niveaux l'école a.
 * Le code postal distingue deux écoles qui portent le même nom.
 * <p><b>Métier / flow :</b> port out du profil professeur. Le front n'écrit pas d'école.
 */
public interface SchoolCatalogPort {

	List<CatalogSchool> list();

	/** Resolves only requested schools for personal profile views. */
	default List<CatalogSchool> findByIds(List<Long> ids) {
		return list().stream().filter(school -> ids.contains(school.id())).toList();
	}

	/** Bounded search ordered by id, for user-facing school selection. */
	default List<CatalogSchool> search(String country, String query, long afterId, int limit) {
		return list().stream().filter(school -> school.id() > afterId)
				.filter(school -> country == null || country.isBlank() || country.equalsIgnoreCase(school.country()))
				.filter(school -> java.text.Normalizer.normalize(school.name().toLowerCase(java.util.Locale.ROOT),
						java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "").contains(query))
				.sorted(java.util.Comparator.comparingLong(CatalogSchool::id)).limit(limit).toList();
	}


	/**
	 * @param id            identifiant stable, le même que {@code back_schools.id}
	 * @param name          nom affiché, tel que le référentiel le donne
	 * @param country       pays, par exemple Belgique
	 * @param teachingTypes types d'enseignement recopiés du fichier, une seule chaîne
	 * @param postalCode    code postal de l'annuaire, vide si le référentiel n'en a pas
	 */
	record CatalogSchool(long id, String name, String country, String teachingTypes, String postalCode) {

		public CatalogSchool {
			postalCode = postalCode == null ? "" : postalCode;
		}

		/** Code postal absent : l'appelant ne le fournit pas. */
		public CatalogSchool(long id, String name, String country, String teachingTypes) {
			this(id, name, country, teachingTypes, "");
		}
	}
}

package com.appv4.ai.application.catalogue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.appv4.ai.application.FournisseurCatalogue;
import com.appv4.ai.application.ModeleCatalogue;

/**
 * Réunit les lignes des catalogues sans en inventer (catalogue IA).
 *
 * <p><b>Business :</b> le même modèle cité par deux sources n'apparaît qu'une fois, avec les deux noms de source.
 * <p><b>Technique :</b> la clé est le code fournisseur plus l'identifiant modèle, en minuscules.
 */
public final class ReunionCatalogues {

	private ReunionCatalogues() {
	}

	public static List<FournisseurCatalogue> reunir(List<EntreeCatalogue> entrees) {
		Map<String, FournisseurAccumule> fournisseurs = new LinkedHashMap<>();
		for (EntreeCatalogue entree : entrees) {
			if (entree.fournisseurCode().isBlank() || entree.modeleId().isBlank()) {
				continue;
			}
			String cleFournisseur = entree.fournisseurCode().toLowerCase();
			FournisseurAccumule fournisseur = fournisseurs.computeIfAbsent(cleFournisseur, cle -> new FournisseurAccumule(
					entree.fournisseurCode(),
					nom(entree.fournisseurNom(), entree.fournisseurCode())));
			String cleModele = entree.modeleId().toLowerCase();
			ModeleAccumule modele = fournisseur.modeles.computeIfAbsent(
					cleModele,
					cle -> new ModeleAccumule(entree.modeleId(), nom(entree.modeleNom(), entree.modeleId())));
			if (!modele.catalogues.contains(entree.catalogue())) {
				modele.catalogues.add(entree.catalogue());
			}
		}
		return fournisseurs.values().stream()
				.map(FournisseurAccumule::figer)
				.sorted((gauche, droite) -> gauche.nom().compareToIgnoreCase(droite.nom()))
				.toList();
	}

	private static String nom(String nom, String repli) {
		return nom == null || nom.isBlank() ? repli : nom;
	}

	private static final class FournisseurAccumule {
		private final String code;
		private final String nom;
		private final Map<String, ModeleAccumule> modeles = new LinkedHashMap<>();

		private FournisseurAccumule(String code, String nom) {
			this.code = code;
			this.nom = nom;
		}

		private FournisseurCatalogue figer() {
			List<ModeleCatalogue> liste = modeles.values().stream()
					.map(ModeleAccumule::figer)
					.sorted((gauche, droite) -> gauche.nom().compareToIgnoreCase(droite.nom()))
					.toList();
			return new FournisseurCatalogue(code, nom, liste);
		}
	}

	private static final class ModeleAccumule {
		private final String id;
		private final String nom;
		private final List<String> catalogues = new ArrayList<>();

		private ModeleAccumule(String id, String nom) {
			this.id = id;
			this.nom = nom;
		}

		private ModeleCatalogue figer() {
			return new ModeleCatalogue(id, nom, List.copyOf(catalogues));
		}
	}
}

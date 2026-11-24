package com.appv4.ai.application.catalogue;

/**
 * Une ligne brute sortie d'un catalogue, avant la réunion.
 *
 * <p><b>Business :</b> le code fournisseur et l'identifiant modèle viennent de la source.
 */
public record EntreeCatalogue(
		String catalogue,
		String fournisseurCode,
		String fournisseurNom,
		String modeleId,
		String modeleNom) {
}

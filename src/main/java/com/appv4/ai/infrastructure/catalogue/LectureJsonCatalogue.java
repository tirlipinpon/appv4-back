package com.appv4.ai.infrastructure.catalogue;

import java.util.ArrayList;
import java.util.List;

import com.appv4.ai.application.catalogue.EntreeCatalogue;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Lit les trois formes de catalogue, sans compléter ce qu'elles n'envoient pas.
 *
 * <p><b>Business :</b> un modèle sans identifiant est ignoré.
 * <p><b>Technique :</b> OpenRouter, models.dev et Artificial Analysis n'ont pas le même JSON.
 */
public final class LectureJsonCatalogue {

	private LectureJsonCatalogue() {
	}

	public static List<EntreeCatalogue> openRouter(JsonNode root) {
		List<EntreeCatalogue> entrees = new ArrayList<>();
		for (JsonNode item : root.path("data")) {
			String id = texte(item, "id");
			if (id.isBlank()) {
				continue;
			}
			int coupe = id.indexOf('/');
			String fournisseur = coupe > 0 ? id.substring(0, coupe) : id;
			entrees.add(new EntreeCatalogue("openrouter", fournisseur, fournisseur, id, texteOu(item, "name", id)));
		}
		return entrees;
	}

	public static List<EntreeCatalogue> modelsDev(JsonNode root) {
		List<EntreeCatalogue> entrees = new ArrayList<>();
		root.fields().forEachRemaining(fournisseur -> {
			JsonNode corps = fournisseur.getValue();
			JsonNode modeles = corps.path("models");
			if (!modeles.isObject()) {
				return;
			}
			String code = fournisseur.getKey();
			String nom = texteOu(corps, "name", code);
			modeles.fields().forEachRemaining(modele -> {
				String id = modele.getKey();
				if (id.isBlank()) {
					return;
				}
				entrees.add(new EntreeCatalogue(
						"modelsdev",
						code,
						nom,
						id,
						texteOu(modele.getValue(), "name", id)));
			});
		});
		return entrees;
	}

	public static List<EntreeCatalogue> artificialAnalysis(JsonNode root) {
		List<EntreeCatalogue> entrees = new ArrayList<>();
		for (JsonNode item : root.path("data")) {
			JsonNode createur = item.path("model_creator");
			String code = premierTexte(createur, "slug", "id");
			String id = premierTexte(item, "slug", "id");
			if (code.isBlank() || id.isBlank()) {
				continue;
			}
			entrees.add(new EntreeCatalogue(
					"artificial-analysis",
					code,
					texteOu(createur, "name", code),
					id,
					texteOu(item, "name", id)));
		}
		return entrees;
	}

	private static String texte(JsonNode node, String champ) {
		return node.path(champ).asText("");
	}

	private static String texteOu(JsonNode node, String champ, String repli) {
		String valeur = texte(node, champ);
		return valeur.isBlank() ? repli : valeur;
	}

	private static String premierTexte(JsonNode node, String premier, String second) {
		String valeur = texte(node, premier);
		return valeur.isBlank() ? texte(node, second) : valeur;
	}
}

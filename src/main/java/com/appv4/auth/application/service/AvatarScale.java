package com.appv4.auth.application.service;

import java.util.Map;
import java.util.Set;

/**
 * Seuils et noms français des zones d'avatar (BACK-48).
 *
 * <p><b>Business :</b> le même nom de zone coûte le même XP dans tous les styles.
 * <p><b>Technique :</b> table figée, pas une donnée que le professeur édite.
 * <p><b>Métier / flow :</b> la lecture d'avatar s'en sert pour ordonner et fermer les zones.
 */
public final class AvatarScale {

	private static final Map<String, Integer> SEUIL = Map.ofEntries(
			Map.entry("mouth", 20),
			Map.entry("eyes", 30),
			Map.entry("expression", 40),
			Map.entry("mood", 50),
			Map.entry("face", 60),
			Map.entry("eyebrows", 70),
			Map.entry("brows", 80),
			Map.entry("nose", 90),
			Map.entry("cheeks", 100),
			Map.entry("ears", 110),
			Map.entry("blush", 120),
			Map.entry("freckles", 130),
			Map.entry("hair", 140),
			Map.entry("frontHair", 150),
			Map.entry("rearHair", 160),
			Map.entry("head", 170),
			Map.entry("heads", 180),
			Map.entry("top", 190),
			Map.entry("tops", 200),
			Map.entry("glasses", 210),
			Map.entry("beard", 220),
			Map.entry("mustache", 230),
			Map.entry("sideburns", 240),
			Map.entry("facialHair", 250),
			Map.entry("earrings", 260),
			Map.entry("hat", 270),
			Map.entry("accessories", 280),
			Map.entry("hairAccessories", 290),
			Map.entry("mask", 300),
			Map.entry("details", 310),
			Map.entry("clothes", 320),
			Map.entry("outfit", 330),
			Map.entry("clothesGraphic", 340),
			Map.entry("body", 350),
			Map.entry("shoulders", 360),
			Map.entry("chest", 370),
			Map.entry("gesture", 380),
			Map.entry("animation", 390),
			Map.entry("pattern", 400),
			Map.entry("texture", 410),
			Map.entry("shape", 420),
			Map.entry("blend", 430),
			Map.entry("plant", 440),
			Map.entry("pot", 450),
			Map.entry("knot", 460),
			Map.entry("spacing", 470),
			Map.entry("sides", 480));

	private static final Map<String, String> LABEL = Map.ofEntries(
			Map.entry("mouth", "bouche"),
			Map.entry("eyes", "yeux"),
			Map.entry("expression", "expression"),
			Map.entry("mood", "humeur"),
			Map.entry("face", "visage"),
			Map.entry("eyebrows", "sourcils"),
			Map.entry("brows", "sourcils"),
			Map.entry("nose", "nez"),
			Map.entry("cheeks", "joues"),
			Map.entry("ears", "oreilles"),
			Map.entry("blush", "blush"),
			Map.entry("freckles", "taches de rousseur"),
			Map.entry("hair", "cheveux"),
			Map.entry("frontHair", "cheveux devant"),
			Map.entry("rearHair", "cheveux derrière"),
			Map.entry("head", "tête"),
			Map.entry("heads", "têtes"),
			Map.entry("top", "coiffure"),
			Map.entry("tops", "coiffures"),
			Map.entry("glasses", "lunettes"),
			Map.entry("beard", "barbe"),
			Map.entry("mustache", "moustache"),
			Map.entry("sideburns", "favoris"),
			Map.entry("facialHair", "pilosité"),
			Map.entry("earrings", "boucles d'oreilles"),
			Map.entry("hat", "chapeau"),
			Map.entry("accessories", "accessoires"),
			Map.entry("hairAccessories", "accessoire de cheveux"),
			Map.entry("mask", "masque"),
			Map.entry("details", "détails"),
			Map.entry("clothes", "vêtements"),
			Map.entry("outfit", "tenue"),
			Map.entry("clothesGraphic", "motif de vêtement"),
			Map.entry("body", "corps"),
			Map.entry("shoulders", "épaules"),
			Map.entry("chest", "torse"),
			Map.entry("gesture", "geste"),
			Map.entry("animation", "animation"),
			Map.entry("pattern", "motif"),
			Map.entry("texture", "texture"),
			Map.entry("shape", "forme"),
			Map.entry("blend", "fondu"),
			Map.entry("plant", "plante"),
			Map.entry("pot", "pot"),
			Map.entry("knot", "nœud"),
			Map.entry("spacing", "espacement"),
			Map.entry("sides", "côtés"));

	private static final Set<String> REMOVABLE = Set.of(
			"glasses",
			"beard",
			"mustache",
			"sideburns",
			"facialHair",
			"earrings",
			"hat",
			"accessories",
			"hairAccessories",
			"freckles",
			"mask");

	private AvatarScale() {
	}

	/** Âge de référence : la table brute, sans majoration. */
	public static final int REFERENCE_AGE = 6;

	/** Majoration ajoutée pour chaque année au-dessus de l'âge de référence. */
	public static final int PERCENT_PER_YEAR = 10;

	public static int seuil(String zoneCode) {
		return seuil(zoneCode, REFERENCE_AGE);
	}

	/**
	 * Seuil de la zone pour cet âge.
	 *
	 * <p>Jusqu'à 6 ans, la table brute. Ensuite, +10 % par année :
	 * 7 ans +10 %, 8 ans +20 %, 12 ans +60 %.
	 */
	public static int seuil(String zoneCode, int age) {
		Integer base = SEUIL.get(zoneCode);
		if (base == null) {
			throw new IllegalStateException("Zone sans seuil : " + zoneCode);
		}
		int percent = Math.max(0, age - REFERENCE_AGE) * PERCENT_PER_YEAR;
		return (base * (100 + percent) + 50) / 100;
	}

	public static String label(String zoneCode) {
		String label = LABEL.get(zoneCode);
		if (label == null) {
			throw new IllegalStateException("Zone sans nom : " + zoneCode);
		}
		return label;
	}

	public static boolean removable(String zoneCode) {
		return REMOVABLE.contains(zoneCode);
	}
}

package com.appv4.auth.application;

import java.util.List;

/**
 * Les cinq classements lus par l'enfant, plus la case Amitié qui n'est pas une liste.
 *
 * <p><b>Business :</b> les mots affichés viennent d'ici, pas de l'écran.
 * <p><b>Technique :</b> codes stables pour l'URL et les cases.
 * <p><b>Métier / flow :</b> fiche parent et lecture enfant (BACK-22).
 */
public final class RankingGroups {

	public static final String SCHOOL_PHRASE = "Il te faut une école pour ce classement.";

	public static final String HIDDEN_PHRASE = "Tu n'es pas dans ce classement.";

	public static final String UNKNOWN = "Ce classement n'existe pas.";

	private static final List<Group> GROUPS = List.of(
			new Group("classe", "Ma classe"),
			new Group("annee", "Mon année"),
			new Group("pays", "Mon pays"),
			new Group("tous", "Tout le monde"),
			new Group("age", "Mon âge"));

	private RankingGroups() {
	}

	public static List<Group> childGroups() {
		return GROUPS;
	}

	public static List<Group> parentCases() {
		return List.of(
				new Group("classe", "Ma classe"),
				new Group("annee", "Mon année"),
				new Group("pays", "Mon pays"),
				new Group("tous", "Tout le monde"),
				new Group("age", "Mon âge"),
				new Group("amitie", "Amitié"));
	}

	public static boolean childGroup(String code) {
		return GROUPS.stream().anyMatch(group -> group.code().equals(code));
	}

	public record Group(String code, String label) {
	}
}

package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.appv4.auth.application.result.TeacherProfileView.CurriculumChoice;

/**
 * Classes proposées à un professeur (SCRUM-159, affinées avec le pays et l'école).
 *
 * <p><b>Business :</b> le pays dit si les classes s'appellent CP ou 1re primaire.
 * L'école dit quels niveaux elle a (maternelle, primaire, secondaire).
 * Une école belge « maternelle et primaire » ne montre pas le secondaire, ni CP.
 * <p><b>Technique :</b> les libellés sont stockés tels quels. Le code enregistré
 * est {@code FRANCE} ou {@code BELGIUM_FR}, déduit du pays.
 * <p><b>Métier / flow :</b> le profil professeur ne demande plus la norme à part :
 * pays → écoles de ce pays → classes de cette école.
 */
public final class SchoolCurricula {

	public static final String FRANCE = "FRANCE";
	public static final String BELGIUM_FR = "BELGIUM_FR";
	public static final String COUNTRY_FRANCE = "France";
	public static final String COUNTRY_BELGIUM = "Belgique";

	private static final List<String> FRANCE_MATERNELLE = List.of(
			"Petite section (PS)",
			"Moyenne section (MS)",
			"Grande section (GS)");
	private static final List<String> FRANCE_PRIMAIRE = List.of("CP", "CE1", "CE2", "CM1", "CM2");
	private static final List<String> FRANCE_COLLEGE = List.of("6e", "5e", "4e", "3e");
	private static final List<String> FRANCE_LYCEE = List.of("2de", "1re", "Terminale");

	private static final List<String> BELGIUM_MATERNELLE = List.of(
			"1re maternelle",
			"2e maternelle",
			"3e maternelle");
	private static final List<String> BELGIUM_PRIMAIRE = List.of(
			"1re primaire",
			"2e primaire",
			"3e primaire",
			"4e primaire",
			"5e primaire",
			"6e primaire");
	private static final List<String> BELGIUM_SECONDAIRE = List.of(
			"1re secondaire",
			"2e secondaire",
			"3e secondaire",
			"4e secondaire",
			"5e secondaire",
			"6e secondaire",
			"7e année (enseignement professionnel)");

	private SchoolCurricula() {
	}

	public static List<CurriculumChoice> options() {
		return List.of(
				new CurriculumChoice(FRANCE, COUNTRY_FRANCE, grades(FRANCE)),
				new CurriculumChoice(
						BELGIUM_FR,
						"Belgique (francophone, Fédération Wallonie-Bruxelles)",
						grades(BELGIUM_FR)));
	}

	/**
	 * @return {@code FRANCE}, {@code BELGIUM_FR}, ou null si ce pays n'a pas de liste
	 */
	public static String forCountry(String country) {
		if (country == null) {
			return null;
		}
		String value = country.trim();
		if (COUNTRY_BELGIUM.equalsIgnoreCase(value)) {
			return BELGIUM_FR;
		}
		if (COUNTRY_FRANCE.equalsIgnoreCase(value)) {
			return FRANCE;
		}
		return null;
	}

	/**
	 * Classes de cette école : noms selon le pays, niveaux selon le type d'enseignement.
	 */
	public static List<String> offeredBy(String country, String teachingTypes) {
		String curriculum = forCountry(country);
		if (curriculum == null) {
			return List.of();
		}
		Set<String> allowed = new HashSet<>(levels(curriculum, teachingTypes));
		if (allowed.isEmpty()) {
			return List.of();
		}
		List<String> kept = new ArrayList<>();
		for (String grade : grades(curriculum)) {
			if (allowed.contains(grade)) {
				kept.add(grade);
			}
		}
		return List.copyOf(kept);
	}

	/** Toutes les classes d'une norme, sans filtre d'école. */
	public static List<String> allForCurriculum(String curriculum) {
		return grades(curriculum);
	}

	public static boolean known(String curriculum) {
		return FRANCE.equals(curriculum) || BELGIUM_FR.equals(curriculum);
	}

	/**
	 * Années connues de cette norme, dans l'ordre d'affichage, sans doublon.
	 */
	public static List<String> ordered(String curriculum, Collection<String> selected) {
		if (selected == null || selected.isEmpty()) {
			return List.of();
		}
		List<String> kept = new ArrayList<>();
		for (String grade : grades(curriculum)) {
			if (selected.contains(grade)) {
				kept.add(grade);
			}
		}
		return List.copyOf(kept);
	}

	private static List<String> levels(String curriculum, String teachingTypes) {
		String text = teachingTypes == null ? "" : teachingTypes.toLowerCase(Locale.ROOT);
		List<String> grades = new ArrayList<>();
		if (text.contains("maternelle")) {
			grades.addAll(FRANCE.equals(curriculum) ? FRANCE_MATERNELLE : BELGIUM_MATERNELLE);
		}
		if (text.contains("primaire") || text.contains("élémentaire") || text.contains("elementaire")) {
			grades.addAll(FRANCE.equals(curriculum) ? FRANCE_PRIMAIRE : BELGIUM_PRIMAIRE);
		}
		grades.addAll(secondary(curriculum, text));
		return grades;
	}

	private static List<String> secondary(String curriculum, String text) {
		boolean belgianSecondary = text.contains("secondaire");
		boolean college = text.contains("collège") || text.contains("college") || text.contains("secondaire");
		boolean lycee = text.contains("lycée") || text.contains("lycee") || text.contains("secondaire");
		if (BELGIUM_FR.equals(curriculum)) {
			if (belgianSecondary || text.contains("collège") || text.contains("college")
					|| text.contains("lycée") || text.contains("lycee")) {
				return BELGIUM_SECONDAIRE;
			}
			return List.of();
		}
		List<String> grades = new ArrayList<>();
		if (college) {
			grades.addAll(FRANCE_COLLEGE);
		}
		if (lycee) {
			grades.addAll(FRANCE_LYCEE);
		}
		return grades;
	}

	private static List<String> grades(String curriculum) {
		if (FRANCE.equals(curriculum)) {
			List<String> all = new ArrayList<>();
			all.addAll(FRANCE_MATERNELLE);
			all.addAll(FRANCE_PRIMAIRE);
			all.addAll(FRANCE_COLLEGE);
			all.addAll(FRANCE_LYCEE);
			return List.copyOf(all);
		}
		if (BELGIUM_FR.equals(curriculum)) {
			List<String> all = new ArrayList<>();
			all.addAll(BELGIUM_MATERNELLE);
			all.addAll(BELGIUM_PRIMAIRE);
			all.addAll(BELGIUM_SECONDAIRE);
			return List.copyOf(all);
		}
		return List.of();
	}
}

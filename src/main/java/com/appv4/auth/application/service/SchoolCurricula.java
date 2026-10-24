package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.appv4.auth.application.result.TeacherProfileView.CurriculumChoice;

/**
 * Années scolaires fermées, norme française ou belge francophone (SCRUM-159).
 *
 * <p><b>Business :</b> ce sont les classes (CP, 1re primaire), pas l'année civile.
 * Une école France ne propose pas « 1re primaire ». Une école Belgique ne propose pas « CP ».
 * La norme flamande n'est pas dans cette liste.
 * <p><b>Technique :</b> les libellés sont stockés tels quels. Le code de norme est
 * {@code FRANCE} ou {@code BELGIUM_FR}.
 * <p><b>Métier / flow :</b> le profil professeur valide les cases cochées ici,
 * puis les renvoie au front pour afficher les mêmes listes.
 */
public final class SchoolCurricula {

	public static final String FRANCE = "FRANCE";
	public static final String BELGIUM_FR = "BELGIUM_FR";

	private static final List<String> FRANCE_GRADES = List.of(
			"Petite section (PS)",
			"Moyenne section (MS)",
			"Grande section (GS)",
			"CP",
			"CE1",
			"CE2",
			"CM1",
			"CM2",
			"6e",
			"5e",
			"4e",
			"3e",
			"2de",
			"1re",
			"Terminale");

	private static final List<String> BELGIUM_GRADES = List.of(
			"1re maternelle",
			"2e maternelle",
			"3e maternelle",
			"1re primaire",
			"2e primaire",
			"3e primaire",
			"4e primaire",
			"5e primaire",
			"6e primaire",
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
				new CurriculumChoice(FRANCE, "France", FRANCE_GRADES),
				new CurriculumChoice(
						BELGIUM_FR,
						"Belgique (francophone, Fédération Wallonie-Bruxelles)",
						BELGIUM_GRADES));
	}

	public static boolean known(String curriculum) {
		return FRANCE.equals(curriculum) || BELGIUM_FR.equals(curriculum);
	}

	public static boolean accepts(String curriculum, String grade) {
		return grade != null && grades(curriculum).contains(grade);
	}

	/**
	 * Années connues de cette norme, dans l'ordre d'affichage, sans doublon.
	 */
	public static List<String> ordered(String curriculum, Collection<String> selected) {
		if (selected == null || selected.isEmpty()) {
			return List.of();
		}
		List<String> official = grades(curriculum);
		List<String> kept = new ArrayList<>();
		for (String grade : official) {
			if (selected.contains(grade)) {
				kept.add(grade);
			}
		}
		return List.copyOf(kept);
	}

	private static List<String> grades(String curriculum) {
		if (FRANCE.equals(curriculum)) {
			return FRANCE_GRADES;
		}
		if (BELGIUM_FR.equals(curriculum)) {
			return BELGIUM_GRADES;
		}
		return List.of();
	}
}

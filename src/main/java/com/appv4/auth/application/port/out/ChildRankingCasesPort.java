package com.appv4.auth.application.port.out;

import java.util.Collection;
import java.util.Map;

/**
 * Cases qui disent si un enfant apparaît dans un classement (BACK-22).
 *
 * <p><b>Business :</b> tant que personne n'a enregistré, l'enfant apparaît partout,
 * y compris dans Amitié qui n'a pas encore de liste.
 * <p><b>Technique :</b> une ligne par fiche. L'absence de ligne vaut six cases cochées.
 * <p><b>Métier / flow :</b> port out de la fiche parent, relu par le classement enfant.
 */
public interface ChildRankingCasesPort {

	/**
	 * Cases enregistrées, ou toutes cochées si la fiche n'a encore rien.
	 */
	RankingSelection selection(long childId);

	/**
	 * Une entrée par identifiant demandé. Une fiche sans ligne est toute cochée.
	 */
	Map<Long, RankingSelection> selection(Collection<Long> childIds);

	void save(long childId, RankingSelection selection);

	/**
	 * @param classe visible dans Ma classe
	 * @param annee visible dans Mon année
	 * @param pays visible dans Mon pays
	 * @param tous visible dans Tout le monde
	 * @param age visible dans Mon âge
	 * @param amitie case gardée pour plus tard
	 */
	record RankingSelection(
			boolean classe,
			boolean annee,
			boolean pays,
			boolean tous,
			boolean age,
			boolean amitie) {

		public static RankingSelection shown() {
			return new RankingSelection(true, true, true, true, true, true);
		}

		public boolean on(String code) {
			return switch (code) {
				case "classe" -> classe;
				case "annee" -> annee;
				case "pays" -> pays;
				case "tous" -> tous;
				case "age" -> age;
				case "amitie" -> amitie;
				default -> false;
			};
		}
	}
}

package com.appv4.auth.application.result;

import java.util.List;
import java.util.Map;

/**
 * Classement renvoyé à l'enfant (BACK-22).
 *
 * <p><b>Business :</b> dix cartes, puis la sienne si elle est au-delà.
 * La phrase, quand elle existe, est la seule.
 * <p><b>Technique :</b> le visage est un code, pas des octets.
 */
public record ChildRankingView(
		List<ScopeChip> scopes,
		String scope,
		String groupLabel,
		String phrase,
		List<RankingCard> cards) {

	public ChildRankingView {
		scopes = List.copyOf(scopes);
		cards = List.copyOf(cards);
	}

	public record ScopeChip(String code, String label, boolean selected) {
	}

	/**
	 * @param face personnage, photo, animal ou aucun
	 * @param pinned vrai seulement pour la carte de l'appelant ajoutée après les dix
	 */
	public record RankingCard(
			int place,
			String firstName,
			int xp,
			boolean mine,
			boolean pinned,
			String face,
			String avatarCode,
			String styleCode,
			Map<String, Object> options,
			List<Pin> pins,
			long childId) {

		public RankingCard {
			options = options == null ? Map.of() : Map.copyOf(options);
			pins = pins == null ? List.of() : List.copyOf(pins);
		}
	}

	public record Pin(String zoneCode, String variantCode) {
	}
}

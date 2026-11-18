package com.appv4.auth.application.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.appv4.auth.application.result.AvatarChoice;

/**
 * Librairie d'icônes des matières (BACK-15 / LEARN-29).
 *
 * <p><b>Business :</b> le professeur choisit un dessin pour sa matière ; l'enfant
 * le voit sur Learn. Les codes sont fermés : rien n'est inventé côté front.
 * <p><b>Technique :</b> même forme {@link AvatarChoice} que les avatars compte.
 * <p><b>Métier / flow :</b> create/update matière → {@code icon_code} → GET child /me subjects.
 */
public final class SubjectIcons {

	public static final List<AvatarChoice> CHOICES = List.of(
			new AvatarChoice("math", "Mathématiques"),
			new AvatarChoice("alphabet", "Alphabet"),
			new AvatarChoice("animals", "Animaux"),
			new AvatarChoice("words", "Mots"),
			new AvatarChoice("knowledge", "Connaissances"),
			new AvatarChoice("opposites", "Contraires"),
			new AvatarChoice("books", "Livres"),
			new AvatarChoice("globe", "Monde"),
			new AvatarChoice("music", "Musique"),
			new AvatarChoice("sport", "Sport"),
			new AvatarChoice("art", "Arts"),
			new AvatarChoice("science", "Sciences"));

	private SubjectIcons() {
	}

	/**
	 * Code normalisé, ou empty si la chaîne est vide (pas d'icône).
	 * Empty aussi si le code est inconnu — l'appelant refuse alors la requête.
	 */
	public static Optional<String> normalizeKnown(String raw) {
		if (raw == null || raw.isBlank()) {
			return Optional.empty();
		}
		String code = raw.trim().toLowerCase(Locale.ROOT);
		boolean known = CHOICES.stream().anyMatch(choice -> choice.code().equals(code));
		return known ? Optional.of(code) : Optional.empty();
	}

	/** Vrai si absent, vide, ou code de la librairie. */
	public static boolean isBlankOrKnown(String raw) {
		if (raw == null || raw.isBlank()) {
			return true;
		}
		return normalizeKnown(raw).isPresent();
	}
}

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
 * <p><b>Métier / flow :</b> create/update matière → {@code icon_code} → GET child subjects.
 */
public final class SubjectIcons {

	public static final List<AvatarChoice> CHOICES = List.of(
			new AvatarChoice("math", "Mathématiques"),
			new AvatarChoice("numbers", "Nombres"),
			new AvatarChoice("counting", "Comptage"),
			new AvatarChoice("geometry", "Géométrie"),
			new AvatarChoice("shapes", "Formes"),
			new AvatarChoice("fractions", "Fractions"),
			new AvatarChoice("algebra", "Algèbre"),
			new AvatarChoice("calculator", "Calculatrice"),
			new AvatarChoice("clock", "Horloge / temps"),
			new AvatarChoice("measure", "Mesures"),
			new AvatarChoice("money", "Argent"),
			new AvatarChoice("alphabet", "Alphabet"),
			new AvatarChoice("letters", "Lettres"),
			new AvatarChoice("reading", "Lecture"),
			new AvatarChoice("writing", "Écriture"),
			new AvatarChoice("words", "Mots"),
			new AvatarChoice("vocabulary", "Vocabulaire"),
			new AvatarChoice("grammar", "Grammaire"),
			new AvatarChoice("spelling", "Orthographe"),
			new AvatarChoice("poetry", "Poésie"),
			new AvatarChoice("story", "Histoire / conte"),
			new AvatarChoice("books", "Livres"),
			new AvatarChoice("library", "Bibliothèque"),
			new AvatarChoice("language", "Langues"),
			new AvatarChoice("english", "Anglais"),
			new AvatarChoice("dutch", "Néerlandais"),
			new AvatarChoice("spanish", "Espagnol"),
			new AvatarChoice("german", "Allemand"),
			new AvatarChoice("science", "Sciences"),
			new AvatarChoice("nature", "Nature"),
			new AvatarChoice("plants", "Plantes"),
			new AvatarChoice("animals", "Animaux"),
			new AvatarChoice("insects", "Insectes"),
			new AvatarChoice("ocean", "Océan"),
			new AvatarChoice("space", "Espace"),
			new AvatarChoice("weather", "Météo"),
			new AvatarChoice("earth", "Terre"),
			new AvatarChoice("globe", "Monde"),
			new AvatarChoice("map", "Carte"),
			new AvatarChoice("history", "Histoire"),
			new AvatarChoice("geography", "Géographie"),
			new AvatarChoice("civics", "Citoyenneté"),
			new AvatarChoice("knowledge", "Connaissances"),
			new AvatarChoice("discovery", "Découverte"),
			new AvatarChoice("experiment", "Expérience"),
			new AvatarChoice("lab", "Laboratoire"),
			new AvatarChoice("music", "Musique"),
			new AvatarChoice("singing", "Chant"),
			new AvatarChoice("instrument", "Instrument"),
			new AvatarChoice("art", "Arts"),
			new AvatarChoice("drawing", "Dessin"),
			new AvatarChoice("painting", "Peinture"),
			new AvatarChoice("craft", "Bricolage"),
			new AvatarChoice("colors", "Couleurs"),
			new AvatarChoice("sport", "Sport"),
			new AvatarChoice("football", "Football"),
			new AvatarChoice("basketball", "Basket"),
			new AvatarChoice("swim", "Natation"),
			new AvatarChoice("dance", "Danse"),
			new AvatarChoice("yoga", "Yoga"),
			new AvatarChoice("run", "Course"),
			new AvatarChoice("bike", "Vélo"),
			new AvatarChoice("body", "Corps humain"),
			new AvatarChoice("health", "Santé"),
			new AvatarChoice("food", "Alimentation"),
			new AvatarChoice("fruit", "Fruits"),
			new AvatarChoice("vegetables", "Légumes"),
			new AvatarChoice("kitchen", "Cuisine"),
			new AvatarChoice("home", "Maison"),
			new AvatarChoice("family", "Famille"),
			new AvatarChoice("friends", "Amis"),
			new AvatarChoice("emotions", "Émotions"),
			new AvatarChoice("opposites", "Contraires"),
			new AvatarChoice("seasons", "Saisons"),
			new AvatarChoice("calendar", "Calendrier"),
			new AvatarChoice("school", "École"),
			new AvatarChoice("pencil", "Crayon"),
			new AvatarChoice("backpack", "Cartable"),
			new AvatarChoice("computer", "Ordinateur"),
			new AvatarChoice("tablet", "Tablette"),
			new AvatarChoice("robot", "Robotique"),
			new AvatarChoice("code", "Code"),
			new AvatarChoice("puzzle", "Puzzle"),
			new AvatarChoice("memory", "Mémoire"),
			new AvatarChoice("logic", "Logique"),
			new AvatarChoice("chess", "Échecs"),
			new AvatarChoice("games", "Jeux"),
			new AvatarChoice("trophy", "Trophée"),
			new AvatarChoice("star", "Étoile"),
			new AvatarChoice("heart", "Cœur"),
			new AvatarChoice("flag", "Drapeau"),
			new AvatarChoice("transport", "Transports"),
			new AvatarChoice("car", "Voiture"),
			new AvatarChoice("train", "Train"),
			new AvatarChoice("plane", "Avion"),
			new AvatarChoice("boat", "Bateau"),
			new AvatarChoice("city", "Ville"),
			new AvatarChoice("farm", "Ferme"),
			new AvatarChoice("forest", "Forêt"),
			new AvatarChoice("mountain", "Montagne"),
			new AvatarChoice("beach", "Plage"),
			new AvatarChoice("fire", "Feu"),
			new AvatarChoice("water", "Eau"),
			new AvatarChoice("air", "Air"),
			new AvatarChoice("recycle", "Écologie"),
			new AvatarChoice("light", "Lumière"),
			new AvatarChoice("sound", "Sons"),
			new AvatarChoice("touch", "Toucher"),
			new AvatarChoice("sight", "Vue"),
			new AvatarChoice("smell", "Odorat"),
			new AvatarChoice("taste", "Goût"));

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

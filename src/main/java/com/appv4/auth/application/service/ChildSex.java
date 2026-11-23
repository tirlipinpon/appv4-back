package com.appv4.auth.application.service;

import java.util.List;

/**
 * Sexe d'une fiche enfant.
 *
 * <p><b>Business :</b> garçon, fille, ou non genré. Le mot du lien suit ce choix.
 * <p><b>Technique :</b> codes stables envoyés par le formulaire. {@code NON_GENRE} est le défaut des fiches déjà là.
 * <p><b>Métier / flow :</b> choisi à l'enregistrement, relu avec la liste, puis utilisé pour frère / sœur.
 */
public final class ChildSex {

	public static final String BOY = "GARCON";
	public static final String GIRL = "FILLE";
	public static final String UNSPECIFIED = "NON_GENRE";

	public static final List<String> CODES = List.of(BOY, GIRL, UNSPECIFIED);

	private ChildSex() {
	}

	public static boolean isKnown(String raw) {
		return raw != null && CODES.contains(raw.trim());
	}
}

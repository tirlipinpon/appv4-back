package com.appv4.ai.application;

/**
 * Échec d'un besoin IA avant toute réponse inventée (BACK-30).
 *
 * <p><b>Business :</b> un besoin inconnu, inactif ou sans fournisseur ne reçoit pas de texte de secours.
 * <p><b>Technique :</b> le message est celui que l'API pourra renvoyer plus tard. Il ne contient pas la clé.
 * <p><b>Métier / flow :</b> le port du besoin refuse, l'adaptateur n'est pas appelé.
 */
public class BesoinIaException extends RuntimeException {

	public enum Code {
		BAD_REQUEST,
		FORBIDDEN,
		NOT_FOUND
	}

	private final Code code;

	public BesoinIaException(String message) {
		this(message, Code.BAD_REQUEST);
	}

	public BesoinIaException(String message, Code code) {
		super(message);
		this.code = code;
	}

	public Code code() {
		return code;
	}
}

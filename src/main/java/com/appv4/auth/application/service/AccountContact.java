package com.appv4.auth.application.service;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.result.AvatarChoice;

/**
 * Règles du nom, du mail de contact, du téléphone et de l'avatar d'un compte.
 *
 * <p><b>Business :</b> le parent et le professeur corrigent la même fiche. Le mail de contact
 * ne sert ni à se connecter ni à réinitialiser le mot de passe.
 * <p><b>Technique :</b> une valeur absente du JSON ({@code null}) laisse la colonne ; une chaîne
 * vide l'efface. Une valeur présente est contrôlée avant l'écriture.
 * <p><b>Métier / flow :</b> appelé par les fiches parent et professeur, avant le port de persistance.
 */
public final class AccountContact {

	public static final String NAME_REQUIRED = "Le nom est obligatoire.";
	public static final String NAME_TOO_LONG = "Le nom est trop long (maximum 255).";
	public static final String EMAIL_INVALID = "Le mail de contact n'est pas valide.";
	public static final String PHONE_INVALID = "Le numéro de téléphone n'est pas valide.";
	public static final String AVATAR_UNKNOWN = "Cet avatar n'existe pas.";

	public static final List<AvatarChoice> CHOICES = List.of(
			new AvatarChoice("owl", "Hibou"),
			new AvatarChoice("fox", "Renard"),
			new AvatarChoice("bear", "Ours"),
			new AvatarChoice("cat", "Chat"),
			new AvatarChoice("panda", "Panda"),
			new AvatarChoice("frog", "Grenouille"));

	private static final Set<String> CODES = Set.of("owl", "fox", "bear", "cat", "panda", "frog");

	private AccountContact() {
	}

	public static String name(String raw) {
		String trimmed = raw == null ? "" : raw.trim();
		if (trimmed.isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, NAME_REQUIRED);
		}
		if (trimmed.length() > DisplayNames.MAX_LENGTH) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, NAME_TOO_LONG);
		}
		return trimmed;
	}

	/** Vide devient {@code null}. Sinon un mail avec un seul @, une partie locale et un domaine à point. */
	public static String email(String raw) {
		String trimmed = raw == null ? "" : raw.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		if (trimmed.length() > 320 || trimmed.chars().anyMatch(Character::isWhitespace)) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, EMAIL_INVALID);
		}
		int at = trimmed.indexOf('@');
		if (at <= 0 || at != trimmed.lastIndexOf('@') || at == trimmed.length() - 1) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, EMAIL_INVALID);
		}
		String domain = trimmed.substring(at + 1);
		int dot = domain.indexOf('.');
		if (dot <= 0 || dot == domain.length() - 1 || domain.indexOf("..") >= 0) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, EMAIL_INVALID);
		}
		return trimmed;
	}

	/** Vide devient {@code null}. Sinon + facultatif, chiffres et espaces, 8 à 15 chiffres, 20 caractères max. */
	public static String phone(String raw) {
		String trimmed = raw == null ? "" : raw.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		if (trimmed.length() > 20) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, PHONE_INVALID);
		}
		int start = 0;
		if (trimmed.charAt(0) == '+') {
			start = 1;
			if (trimmed.length() == 1) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, PHONE_INVALID);
			}
		}
		int digits = 0;
		for (int i = start; i < trimmed.length(); i++) {
			char c = trimmed.charAt(i);
			if (c == ' ') {
				continue;
			}
			if (c < '0' || c > '9') {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, PHONE_INVALID);
			}
			digits++;
		}
		if (digits < 8 || digits > 15) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, PHONE_INVALID);
		}
		return trimmed;
	}

	/** Vide devient {@code null}. Sinon un des six codes du catalogue. */
	public static String avatar(String raw) {
		String trimmed = raw == null ? "" : raw.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		if (!CODES.contains(trimmed)) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, AVATAR_UNKNOWN);
		}
		return trimmed;
	}

	/**
	 * {@code incoming == null} : la colonne ne change pas.
	 * Une chaîne, même vide, passe par le contrôle et peut effacer la colonne.
	 */
	public static String merge(String incoming, String current, Function<String, String> normalize) {
		if (incoming == null) {
			return current;
		}
		return normalize.apply(incoming);
	}
}

package com.appv4.auth.application.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.RecoverIdentifierUseCase;
import com.appv4.auth.application.port.out.RecoveryAccountPort;
import com.appv4.auth.application.port.out.RecoveryAccountPort.RecoveryCompte;

/**
 * Récupération de l'identifiant.
 *
 * <p><b>Business :</b> une petite erreur sur chaque info peut encore désigner le compte.
 * Un seul compte actif montre le masque. Un seul inactif est annoncé. Sinon, la même phrase.
 * <p><b>Technique :</b> chaque info vaut 100, 70 ou 0. La plus faible décide. Seuil 70.
 * <p><b>Métier / flow :</b> port in → lecture des comptes → masque ou phrase. Aucune écriture.
 */
public class RecoverIdentifierService implements RecoverIdentifierUseCase {

	public static final String MISS = "Aucun compte ne correspond à ces informations.";

	public static final String INACTIVE = "Ce compte est inactif.";

	public static final String WAIT = "Trop d'essais. Réessaie dans un moment.";

	private static final int BAR = 70;

	private final RecoveryAccountPort accounts;

	public RecoverIdentifierService(RecoveryAccountPort accounts) {
		this.accounts = accounts;
	}

	@Override
	public IdentifierRecovery recover(String displayName, String phone, String contactEmail) {
		String name = AccountContact.name(displayName);
		String validPhone = AccountContact.phone(phone);
		if (validPhone == null) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, AccountContact.PHONE_INVALID);
		}
		String validEmail = AccountContact.email(contactEmail);
		if (validEmail == null) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, AccountContact.EMAIL_INVALID);
		}
		List<RecoveryCompte> band = new ArrayList<>();
		for (RecoveryCompte compte : accounts.all()) {
			int ratio = Math.min(
					scoreName(name, compte.displayName()),
					Math.min(scorePhone(validPhone, compte.phone()), scoreEmail(validEmail, compte.contactEmail())));
			if (ratio >= BAR) {
				band.add(compte);
			}
		}
		if (band.size() != 1) {
			return new IdentifierRecovery(null, MISS);
		}
		RecoveryCompte only = band.get(0);
		if (!only.active()) {
			return new IdentifierRecovery(null, INACTIVE);
		}
		return new IdentifierRecovery(mask(only.loginEmail()), null);
	}

	/**
	 * Garde deux caractères quand la partie locale en a au moins trois.
	 * Le domaine reste entier, arobase comprise.
	 */
	static String mask(String loginEmail) {
		int at = loginEmail.indexOf('@');
		String local = at < 0 ? loginEmail : loginEmail.substring(0, at);
		String domain = at < 0 ? "" : loginEmail.substring(at);
		String shown = local.length() <= 2
				? "*".repeat(Math.max(local.length(), 1))
				: local.substring(0, 2) + "*".repeat(local.length() - 2);
		return shown + domain;
	}

	private static int scoreName(String submitted, String stored) {
		String left = prepareName(submitted);
		String right = prepareName(stored);
		if (left.isEmpty() || right.isEmpty()) {
			return 0;
		}
		if (left.equals(right)) {
			return 100;
		}
		if (words(left).equals(words(right))) {
			return BAR;
		}
		if (distance(left, right) == 1) {
			return BAR;
		}
		return 0;
	}

	private static int scorePhone(String submitted, String stored) {
		return scorePrepared(preparePhone(submitted), preparePhone(stored));
	}

	private static int scoreEmail(String submitted, String stored) {
		return scorePrepared(prepareEmail(submitted), prepareEmail(stored));
	}

	private static int scorePrepared(String left, String right) {
		if (left.isEmpty() || right.isEmpty()) {
			return 0;
		}
		if (left.equals(right)) {
			return 100;
		}
		if (distance(left, right) == 1) {
			return BAR;
		}
		return 0;
	}

	private static String prepareName(String raw) {
		if (raw == null) {
			return "";
		}
		String lowered = raw.trim().toLowerCase(Locale.ROOT).replace('-', ' ');
		String plain = Normalizer.normalize(lowered, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
		return plain.replaceAll("\\s+", " ").trim();
	}

	private static List<String> words(String prepared) {
		return Arrays.stream(prepared.split(" ")).sorted().toList();
	}

	/**
	 * Chiffres seuls. 0032, 0033, puis 32 ou 33 s'il reste au moins huit chiffres,
	 * puis un 0 de tête, sont le préfixe national.
	 */
	private static String preparePhone(String raw) {
		if (raw == null) {
			return "";
		}
		String digits = raw.replaceAll("\\D", "");
		if (digits.startsWith("0032") || digits.startsWith("0033")) {
			digits = digits.substring(4);
		} else if ((digits.startsWith("32") || digits.startsWith("33")) && digits.length() >= 10) {
			digits = digits.substring(2);
		}
		if (digits.startsWith("0")) {
			digits = digits.substring(1);
		}
		return digits;
	}

	private static String prepareEmail(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.trim().toLowerCase(Locale.ROOT);
	}

	private static int distance(String left, String right) {
		int[] previous = new int[right.length() + 1];
		int[] current = new int[right.length() + 1];
		for (int column = 0; column <= right.length(); column++) {
			previous[column] = column;
		}
		for (int row = 1; row <= left.length(); row++) {
			current[0] = row;
			for (int column = 1; column <= right.length(); column++) {
				int cost = left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1;
				current[column] = Math.min(
						Math.min(current[column - 1] + 1, previous[column] + 1),
						previous[column - 1] + cost);
			}
			int[] swap = previous;
			previous = current;
			current = swap;
		}
		return previous[right.length()];
	}
}

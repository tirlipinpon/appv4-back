package com.appv4.auth.application.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.AppHours;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.LoginChildUseCase;
import com.appv4.auth.application.port.out.AppHoursPort;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.result.ChildLoginResult;
import com.appv4.auth.application.result.ChildSessionView;
import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Login enfant par prénom et code (BACK-13).
 *
 * <p><b>Business :</b> l'enfant entre dans son app. Prénom inconnu, code faux,
 * fiche arrêtée, ou deux fiches avec le même prénom et le même code : le même refus.
 * Pendant la fermeture, l'entrée et la relecture de session sont fermées : pas de jeton, pas de séjour.
 * Les heures sont celles choisies par l'admin. Au départ, de 22 h à 6 h. On n'ouvre pas l'admin.
 * <p><b>Technique :</b> {@code matches} est toujours appelé, y compris sur un hash factice
 * quand aucune fiche ne correspond, pour ne pas rendre l'absence plus rapide.
 * La limite HTTP sur {@code POST /api/ext/auth/login} freine les essais des 10 000 codes.
 * L'horloge injectée est lue dans le fuseau de la progression.
 * <p><b>Métier / flow :</b> port in → si l'app est fermée, la phrase des horaires, sans jeton ;
 * sinon port out fiches actives → port out jeton enfant.
 */
public class LoginChildService implements LoginChildUseCase {

	public static final String INVALID_CREDENTIALS_MESSAGE =
			"Prénom ou code inconnu ou erroné.";

	public static final String INVALID_SESSION_MESSAGE = "Session enfant invalide.";

	private final ChildLoginPort childLoginPort;
	private final PasswordEncoder passwordEncoder;
	private final ChildSessionTokenPort childSessionTokenPort;
	private final Clock clock;
	private final AppHoursPort appHours;

	public LoginChildService(
			ChildLoginPort childLoginPort,
			PasswordEncoder passwordEncoder,
			ChildSessionTokenPort childSessionTokenPort,
			Clock clock) {
		this(childLoginPort, passwordEncoder, childSessionTokenPort, clock, AppHoursPort.fixed(AppHours.defaults()));
	}

	public LoginChildService(
			ChildLoginPort childLoginPort,
			PasswordEncoder passwordEncoder,
			ChildSessionTokenPort childSessionTokenPort,
			Clock clock,
			AppHoursPort appHours) {
		this.childLoginPort = childLoginPort;
		this.passwordEncoder = passwordEncoder;
		this.childSessionTokenPort = childSessionTokenPort;
		this.clock = clock;
		this.appHours = appHours;
	}

	@Override
	@Transactional(readOnly = true)
	public ChildLoginResult login(String firstName, String pin) {
		String normalized = normalizeFirstName(firstName);
		String attempt = pin == null ? "" : pin;
		if (normalized.isEmpty() || !isFourDigits(attempt)) {
			passwordEncoder.matches(attempt, LoginWithEmailService.DUMMY_BCRYPT_HASH);
			throw invalidCredentials();
		}

		List<ChildLoginCandidate> candidates = childLoginPort.findActiveByFirstName(normalized);
		if (candidates.isEmpty()) {
			passwordEncoder.matches(attempt, LoginWithEmailService.DUMMY_BCRYPT_HASH);
			throw invalidCredentials();
		}

		List<ChildLoginCandidate> matches = new ArrayList<>();
		for (ChildLoginCandidate candidate : candidates) {
			if (pinMatches(attempt, candidate.pinHash())) {
				matches.add(candidate);
			}
		}
		if (matches.size() != 1) {
			throw invalidCredentials();
		}

		ChildLoginCandidate child = matches.get(0);
		refuseWhenClosed();
		JwtTokenResult token = childSessionTokenPort.issue(child.id(), child.firstName());
		return new ChildLoginResult(token.token(), token.expiresAtEpochSeconds(), child.firstName());
	}

	@Override
	@Transactional(readOnly = true)
	public ChildSessionView session(String rawToken) {
		refuseWhenClosed();
		long childId = childSessionTokenPort.read(rawToken)
				.orElseThrow(this::invalidSession)
				.childId();
		ChildLoginCandidate child = childLoginPort.findActiveById(childId)
				.orElseThrow(this::invalidSession);
		return new ChildSessionView(child.id(), child.firstName());
	}

	/** Pendant la fermeture : la phrase des horaires, sans jeton. */
	private void refuseWhenClosed() {
		AppHours hours = appHours.current();
		if (hours.closedAt(clock.instant(), ReadChildProgressService.PROGRESS_ZONE)) {
			throw new AuthDomainException(AuthErrorCode.UNAUTHORIZED, hours.closedMessage());
		}
	}

	private boolean pinMatches(String pin, String hash) {
		if (hash == null || hash.isBlank()) {
			return false;
		}
		try {
			return passwordEncoder.matches(pin, hash);
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	static String normalizeFirstName(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.trim().toLowerCase(Locale.ROOT);
	}

	private static boolean isFourDigits(String value) {
		if (value.length() != 4) {
			return false;
		}
		for (int i = 0; i < value.length(); i++) {
			if (!Character.isDigit(value.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private AuthDomainException invalidCredentials() {
		return new AuthDomainException(AuthErrorCode.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
	}

	private AuthDomainException invalidSession() {
		return new AuthDomainException(AuthErrorCode.UNAUTHORIZED, INVALID_SESSION_MESSAGE);
	}
}

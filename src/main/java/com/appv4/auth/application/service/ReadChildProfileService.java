package com.appv4.auth.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ReadChildProfileUseCase;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildPhotoBytes;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildProfile;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.result.ChildProfileView;

/**
 * Profil enfant pour l'app learning (BACK-14).
 *
 * <p><b>Business :</b> après la connexion, l'accueil lit prénom et image sur la fiche.
 * Une fiche arrêtée ne donne plus de profil.
 * <p><b>Technique :</b> même jeton enfant que le login ; pas de claim inventé pour l'avatar.
 * <p><b>Métier / flow :</b> port in → jeton enfant → port out fiche active.
 */
public class ReadChildProfileService implements ReadChildProfileUseCase {

	public static final String INVALID_SESSION_MESSAGE = LoginChildService.INVALID_SESSION_MESSAGE;

	public static final String PHOTO_MISSING_MESSAGE = "Photo introuvable.";

	private final ChildLoginPort childLoginPort;
	private final ChildSessionTokenPort childSessionTokenPort;

	public ReadChildProfileService(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort) {
		this.childLoginPort = childLoginPort;
		this.childSessionTokenPort = childSessionTokenPort;
	}

	@Override
	@Transactional(readOnly = true)
	public ChildProfileView profile(String rawToken) {
		long childId = requireChildId(rawToken);
		ChildProfile profile = childLoginPort.findActiveProfileById(childId)
				.orElseThrow(this::invalidSession);
		return new ChildProfileView(
				profile.id(),
				profile.firstName(),
				profile.avatarCode(),
				profile.hasPhoto());
	}

	@Override
	@Transactional(readOnly = true)
	public ChildPhotoBytes photo(String rawToken) {
		long childId = requireChildId(rawToken);
		if (childLoginPort.findActiveProfileById(childId).isEmpty()) {
			throw invalidSession();
		}
		return childLoginPort.findActivePhotoById(childId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, PHOTO_MISSING_MESSAGE));
	}

	private long requireChildId(String rawToken) {
		return childSessionTokenPort.read(rawToken)
				.orElseThrow(this::invalidSession)
				.childId();
	}

	private AuthDomainException invalidSession() {
		return new AuthDomainException(AuthErrorCode.UNAUTHORIZED, INVALID_SESSION_MESSAGE);
	}
}

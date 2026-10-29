package com.appv4.auth.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ConnectedAccountUseCase;
import com.appv4.auth.application.port.in.ConnectedAccountUseCase.ConnectedBanner;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;

/**
 * Donne le nom du compte connecté pour l'en-tête.
 *
 * <p><b>Business :</b> parent et professeur partagent le même nom de compte.
 * Un compte absent ne reçoit pas un nom inventé.
 * <p><b>Technique :</b> lecture seule. Aucune écriture, aucun nouveau JWT.
 * <p><b>Métier / flow :</b> port in → {@link AuthUserAccountPort#findById}.
 */
public class ConnectedAccountService implements ConnectedAccountUseCase {

	public static final String ACCOUNT_MISSING = "Compte introuvable.";

	private final AuthUserAccountPort authUserAccountPort;

	public ConnectedAccountService(AuthUserAccountPort authUserAccountPort) {
		this.authUserAccountPort = authUserAccountPort;
	}

	@Override
	@Transactional(readOnly = true)
	public String displayName(long userId) {
		return account(userId).displayName();
	}

	@Override
	@Transactional(readOnly = true)
	public ConnectedBanner banner(long userId) {
		String name = account(userId).displayName();
		String avatar = authUserAccountPort.findAvatarCode(userId).orElse(null);
		return new ConnectedBanner(name, avatar);
	}

	private AuthUserAccount account(long userId) {
		return authUserAccountPort.findById(userId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, ACCOUNT_MISSING));
	}
}

package com.appv4.auth.application.service;

import java.util.Locale;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.in.SwitchActiveRoleUseCase;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Change l'espace actif et renvoie un nouveau jeton (SCRUM-124).
 *
 * <p><b>Business :</b> on ne fait pas confiance au rôle envoyé par le front
 * s'il n'est pas déjà accordé. Sinon la colonne et le JWT suivent.
 * <p><b>Technique :</b> 403, colonne inchangée, si le rôle n'est pas tenu.
 * <p><b>Métier / flow :</b> port in du {@code POST /api/int/auth/switch-role}
 * → port out rôles → {@link IssueJwtUseCase}.
 */
public class SwitchActiveRoleService implements SwitchActiveRoleUseCase {

	public static final String ROLE_NOT_GRANTED = "Rôle non accordé.";

	private final ActiveRolePort activeRolePort;
	private final IssueJwtUseCase issueJwtUseCase;

	public SwitchActiveRoleService(ActiveRolePort activeRolePort, IssueJwtUseCase issueJwtUseCase) {
		this.activeRolePort = activeRolePort;
		this.issueJwtUseCase = issueJwtUseCase;
	}

	@Override
	@Transactional
	public JwtTokenResult switchRole(long userId, String email, String role) {
		String requested = normalize(role);
		if (!activeRolePort.findSpaceRoles(userId).contains(requested)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, ROLE_NOT_GRANTED);
		}
		activeRolePort.saveLastActiveRole(userId, requested);
		return issueJwtUseCase.issue(userId, email);
	}

	private static String normalize(String role) {
		if (role == null || role.isBlank()) {
			throw denied();
		}
		String value = role.trim().toLowerCase(Locale.ROOT);
		if (!ActiveRoles.isSpace(value)) {
			throw denied();
		}
		return value;
	}

	private static AuthDomainException denied() {
		return new AuthDomainException(AuthErrorCode.FORBIDDEN, ROLE_NOT_GRANTED);
	}
}

package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.ActiveRolePort.AuthzSnapshot;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.SessionState;
import com.appv4.auth.application.port.out.JwtTokenEncoder;
import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Émet le JWT et y pose l'espace actif (SCRUM-124).
 *
 * <p><b>Business :</b> un seul rôle → on y va et on s'en souvient. Deux rôles
 * et un souvenir encore valide → on le reprend. Deux rôles sans souvenir
 * valide → le jeton ne choisit pas d'espace (le front montrera le choix).
 * Un compte inactif ne reçoit pas de nouveau badge.
 * <p><b>Technique :</b> {@link ActiveRolePort#loadAuthzSnapshot} une fois (issue #37).
 * Claim {@code sv} = {@code security_version} (issue #49).
 * <p><b>Métier / flow :</b> appelé après login, signup, switch-role, complete-roles.
 */
public class IssueJwtService implements IssueJwtUseCase {

	static final String ACCOUNT_INACTIVE = "Compte indisponible.";

	private final JwtTokenEncoder jwtTokenEncoder;
	private final ActiveRolePort activeRolePort;
	private final AuthUserAccountPort authUserAccountPort;

	public IssueJwtService(
			JwtTokenEncoder jwtTokenEncoder,
			ActiveRolePort activeRolePort,
			AuthUserAccountPort authUserAccountPort) {
		this.jwtTokenEncoder = jwtTokenEncoder;
		this.activeRolePort = activeRolePort;
		this.authUserAccountPort = authUserAccountPort;
	}

	@Override
	@Transactional
	public JwtTokenResult issue(long userId, String email) {
		SessionState session = authUserAccountPort.findSessionState(userId)
				.orElseThrow(() -> new AuthDomainException(
						HttpStatus.UNAUTHORIZED.value(), ACCOUNT_INACTIVE));
		if (!session.active()) {
			throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), ACCOUNT_INACTIVE);
		}
		AuthzSnapshot authz = activeRolePort.loadAuthzSnapshot(userId);
		String activeRole = resolveAndRemember(userId, authz.spaceRoles(), authz.lastActiveRole());
		List<String> roles = new ArrayList<>(authz.spaceRoles());
		if (authz.admin()) {
			roles.add(ActiveRoles.ADMIN);
		}
		return jwtTokenEncoder.encode(
				userId, email, activeRole, roles, authz.pendingTeacher(), session.securityVersion());
	}

	private String resolveAndRemember(long userId, List<String> spaces, String last) {
		if (spaces.size() == 1) {
			String only = spaces.get(0);
			if (!only.equals(last)) {
				activeRolePort.saveLastActiveRole(userId, only);
			}
			return only;
		}
		if (spaces.size() >= 2 && last != null && spaces.contains(last)) {
			return last;
		}
		if (last != null) {
			activeRolePort.saveLastActiveRole(userId, null);
		}
		return null;
	}
}

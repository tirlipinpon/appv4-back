package com.appv4.auth.application.service;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.JwtTokenEncoder;
import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Émet le JWT et y pose l'espace actif (SCRUM-124).
 *
 * <p><b>Business :</b> un seul rôle → on y va et on s'en souvient. Deux rôles
 * et un souvenir encore valide → on le reprend. Deux rôles sans souvenir
 * valide → le jeton ne choisit pas d'espace (le front montrera le choix).
 * <p><b>Technique :</b> claim {@code activeRole} omis quand il n'y a pas d'espace.
 * <p><b>Métier / flow :</b> appelé après login email, login social et inscription,
 * une fois les rôles écrits.
 */
public class IssueJwtService implements IssueJwtUseCase {

	private final JwtTokenEncoder jwtTokenEncoder;
	private final ActiveRolePort activeRolePort;

	public IssueJwtService(JwtTokenEncoder jwtTokenEncoder, ActiveRolePort activeRolePort) {
		this.jwtTokenEncoder = jwtTokenEncoder;
		this.activeRolePort = activeRolePort;
	}

	@Override
	@Transactional
	public JwtTokenResult issue(long userId, String email) {
		String activeRole = resolveAndRemember(userId);
		return jwtTokenEncoder.encode(userId, email, activeRole);
	}

	/**
	 * Choisit l'espace à partir des rôles accordés et du souvenir, puis aligne la colonne.
	 *
	 * @return {@code parent}, {@code teacher}, ou {@code null} si le front doit choisir
	 */
	private String resolveAndRemember(long userId) {
		List<String> spaces = activeRolePort.findSpaceRoles(userId);
		String last = activeRolePort.findLastActiveRole(userId).orElse(null);
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

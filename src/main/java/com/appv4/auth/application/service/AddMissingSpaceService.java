package com.appv4.auth.application.service;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.in.AddMissingSpaceUseCase;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.SignupRolePort;
import com.appv4.auth.application.result.JwtTokenResult;
import com.appv4.auth.application.result.SpaceAccess;

/**
 * Ajoute l'espace manquant sur le compte déjà ouvert (SCRUM-152).
 *
 * <p><b>Business :</b> parent tout de suite. Professeur = demande en attente,
 * comme à l'inscription. Le bouton « Passer en » ne crée jamais un rôle.
 * <p><b>Technique :</b> réutilise {@link SignupRolePort#grantParent} et
 * {@link SignupRolePort#requestTeacher}. Pas de nouvelle table.
 * <p><b>Métier / flow :</b> filtre JWT → controller {@code /api/int/auth/spaces}
 * → ce service → ports rôles / demande → JWT seulement pour le parent.
 */
public class AddMissingSpaceService implements AddMissingSpaceUseCase {

	private final SignupRolePort signupRolePort;
	private final ActiveRolePort activeRolePort;
	private final IssueJwtUseCase issueJwtUseCase;

	public AddMissingSpaceService(
			SignupRolePort signupRolePort,
			ActiveRolePort activeRolePort,
			IssueJwtUseCase issueJwtUseCase) {
		this.signupRolePort = signupRolePort;
		this.activeRolePort = activeRolePort;
		this.issueJwtUseCase = issueJwtUseCase;
	}

	@Override
	@Transactional(readOnly = true)
	public SpaceAccess view(long userId) {
		return snapshot(userId);
	}

	@Override
	@Transactional
	public JwtTokenResult openParent(long userId, String email) {
		signupRolePort.grantParent(userId);
		return issueJwtUseCase.issue(userId, email);
	}

	@Override
	@Transactional
	public SpaceAccess requestTeacher(long userId) {
		if (!activeRolePort.findSpaceRoles(userId).contains(ActiveRoles.TEACHER)) {
			signupRolePort.requestTeacher(userId);
		}
		return snapshot(userId);
	}

	private SpaceAccess snapshot(long userId) {
		List<String> roles = activeRolePort.findSpaceRoles(userId);
		return new SpaceAccess(roles, activeRolePort.teacherRequestStatus(userId));
	}
}

package com.appv4.auth.application.service;

import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.in.CompleteRolesUseCase;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.SignupRolePort;
import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Enregistre le premier choix de rôle (SCRUM-153).
 *
 * <p><b>Business :</b> parent tout de suite, professeur en demande. Un second passage
 * ne change pas un choix déjà posé.
 * <p><b>Technique :</b> réutilise {@link SignupRoles} et {@link SignupRolePort}.
 * <p><b>Métier / flow :</b> après signup email ou login social, avant d'entrer dans un espace.
 */
public class CompleteRolesService implements CompleteRolesUseCase {

	private final ActiveRolePort activeRolePort;
	private final SignupRolePort signupRolePort;
	private final IssueJwtUseCase issueJwtUseCase;

	public CompleteRolesService(
			ActiveRolePort activeRolePort,
			SignupRolePort signupRolePort,
			IssueJwtUseCase issueJwtUseCase) {
		this.activeRolePort = activeRolePort;
		this.signupRolePort = signupRolePort;
		this.issueJwtUseCase = issueJwtUseCase;
	}

	@Override
	@Transactional
	public JwtTokenResult complete(long userId, String email, List<String> roles) {
		if (alreadyChosen(userId)) {
			return issueJwtUseCase.issue(userId, email);
		}
		Set<String> chosen = SignupRoles.normalizeOrThrow(roles);
		if (chosen.contains(SignupRoles.PARENT)) {
			signupRolePort.grantParent(userId);
		}
		if (chosen.contains(SignupRoles.TEACHER)) {
			signupRolePort.requestTeacher(userId);
		}
		return issueJwtUseCase.issue(userId, email);
	}

	private boolean alreadyChosen(long userId) {
		return !activeRolePort.findSpaceRoles(userId).isEmpty()
				|| activeRolePort.hasPendingTeacherRequest(userId);
	}
}

package com.appv4.auth.application.service;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.TeacherModerationUseCase;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.TeacherApprovedMailPort;
import com.appv4.auth.application.port.out.TeacherModerationPort;
import com.appv4.auth.application.port.out.TeacherRequestView;

/**
 * Accepter une demande professeur (SCRUM-142).
 *
 * <p><b>Business :</b> dire oui donne l'espace professeur et prévient la personne.
 * Révoquer retire cet espace. Un oui déjà donné peut revenir, et l'inverse.
 * <p><b>Technique :</b> le contrôle admin lit la base, pas seulement le jeton.
 * <p><b>Métier / flow :</b> port in de la page de modération.
 */
public class TeacherModerationService implements TeacherModerationUseCase {

	static final String FORBIDDEN = "Réservé à l'admin.";
	static final String NOT_FOUND = "Demande introuvable.";
	static final String NOT_ACCEPTABLE = "Cette demande ne peut pas être acceptée.";
	static final String NOT_REVOKABLE = "Cette demande ne peut pas être révoquée.";
	private static final String PENDING = "pending";
	private static final String APPROVED = "approved";
	private static final String REVOKED = "revoked";

	private final ActiveRolePort activeRolePort;
	private final TeacherModerationPort teacherModerationPort;
	private final TeacherApprovedMailPort teacherApprovedMailPort;

	public TeacherModerationService(
			ActiveRolePort activeRolePort,
			TeacherModerationPort teacherModerationPort,
			TeacherApprovedMailPort teacherApprovedMailPort) {
		this.activeRolePort = activeRolePort;
		this.teacherModerationPort = teacherModerationPort;
		this.teacherApprovedMailPort = teacherApprovedMailPort;
	}

	@Override
	@Transactional(readOnly = true)
	public List<TeacherRequestView> list(long callerId) {
		requireAdmin(callerId);
		return teacherModerationPort.findPendingAndApproved();
	}

	@Override
	@Transactional
	public TeacherRequestView approve(long callerId, long requestId) {
		requireAdmin(callerId);
		TeacherRequestView current = teacherModerationPort.findById(requestId)
				.orElseThrow(() -> new AuthDomainException(HttpStatus.NOT_FOUND.value(), NOT_FOUND));
		if (APPROVED.equals(current.status())) {
			teacherModerationPort.grantTeacher(current.userId());
			return current;
		}
		if (!PENDING.equals(current.status()) && !REVOKED.equals(current.status())) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), NOT_ACCEPTABLE);
		}
		return markApproved(current, requestId);
	}

	@Override
	@Transactional
	public TeacherRequestView revoke(long callerId, long requestId) {
		requireAdmin(callerId);
		TeacherRequestView current = teacherModerationPort.findById(requestId)
				.orElseThrow(() -> new AuthDomainException(HttpStatus.NOT_FOUND.value(), NOT_FOUND));
		if (REVOKED.equals(current.status())) {
			teacherModerationPort.revokeTeacher(current.userId());
			forgetTeacherSpace(current.userId());
			return current;
		}
		if (!APPROVED.equals(current.status())) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), NOT_REVOKABLE);
		}
		Instant reviewedAt = Instant.now();
		teacherModerationPort.markRevoked(requestId, reviewedAt);
		teacherModerationPort.revokeTeacher(current.userId());
		forgetTeacherSpace(current.userId());
		return new TeacherRequestView(
				current.id(),
				current.userId(),
				current.email(),
				REVOKED,
				current.createdAt(),
				reviewedAt);
	}

	private TeacherRequestView markApproved(TeacherRequestView current, long requestId) {
		Instant reviewedAt = Instant.now();
		teacherModerationPort.markApproved(requestId, reviewedAt);
		teacherModerationPort.grantTeacher(current.userId());
		teacherApprovedMailPort.sendTeacherApproved(current.email());
		return new TeacherRequestView(
				current.id(),
				current.userId(),
				current.email(),
				APPROVED,
				current.createdAt(),
				reviewedAt);
	}

	/** Si l'espace ouvert était professeur, on l'oublie. Parent reste le souvenir s'il est seul. */
	private void forgetTeacherSpace(long userId) {
		String last = activeRolePort.findLastActiveRole(userId).orElse(null);
		if (!ActiveRoles.TEACHER.equals(last)) {
			return;
		}
		var spaces = activeRolePort.findSpaceRoles(userId);
		activeRolePort.saveLastActiveRole(userId, spaces.size() == 1 ? spaces.get(0) : null);
	}

	private void requireAdmin(long callerId) {
		if (!activeRolePort.hasAdminRole(callerId)) {
			throw new AuthDomainException(HttpStatus.FORBIDDEN.value(), FORBIDDEN);
		}
	}
}

package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.TeacherModerationPort;
import com.appv4.auth.application.port.out.TeacherRequestView;
import com.appv4.auth.application.service.ActiveRoles;

/**
 * Lit et accepte les demandes professeur (SCRUM-142).
 *
 * <p><b>Business :</b> pending et approved remontent dans la file.
 * Le rôle teacher n'est écrit qu'au oui, et une seule fois.
 * <p><b>Technique :</b> JPA sur les deux tables déjà créées en V5.
 * <p><b>Métier / flow :</b> adapter out du port {@link TeacherModerationPort}.
 */
@Component
public class TeacherModerationPersistenceAdapter implements TeacherModerationPort {

	private static final String PENDING = "pending";
	private static final String APPROVED = "approved";
	private static final String REVOKED = "revoked";

	private final BackTeacherRoleRequestJpaRepository requestRepository;
	private final BackUserJpaRepository userRepository;
	private final BackUserRoleJpaRepository roleRepository;

	public TeacherModerationPersistenceAdapter(
			BackTeacherRoleRequestJpaRepository requestRepository,
			BackUserJpaRepository userRepository,
			BackUserRoleJpaRepository roleRepository) {
		this.requestRepository = requestRepository;
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
	}

	@Override
	public List<TeacherRequestView> findPendingAndApproved() {
		return requestRepository.findByStatusInOrderByCreatedAtAsc(List.of(PENDING, APPROVED, REVOKED)).stream()
				.map(this::toView)
				.toList();
	}

	@Override
	public Optional<TeacherRequestView> findById(long requestId) {
		return requestRepository.findById(requestId).map(this::toView);
	}

	@Override
	public void markApproved(long requestId, Instant reviewedAt) {
		BackTeacherRoleRequestEntity entity = requestRepository.findById(requestId)
				.orElseThrow(() -> new AuthDomainException(HttpStatus.NOT_FOUND.value(), "Demande introuvable."));
		entity.setStatus(APPROVED);
		entity.setReviewedAt(reviewedAt);
		requestRepository.save(entity);
	}

	@Override
	public void markRevoked(long requestId, Instant reviewedAt) {
		BackTeacherRoleRequestEntity entity = requestRepository.findById(requestId)
				.orElseThrow(() -> new AuthDomainException(HttpStatus.NOT_FOUND.value(), "Demande introuvable."));
		entity.setStatus(REVOKED);
		entity.setReviewedAt(reviewedAt);
		requestRepository.save(entity);
	}

	@Override
	public void grantTeacher(long userId) {
		if (roleRepository.existsByUserIdAndRole(userId, ActiveRoles.TEACHER)) {
			return;
		}
		roleRepository.save(new BackUserRoleEntity(userId, ActiveRoles.TEACHER));
	}

	@Override
	public void revokeTeacher(long userId) {
		roleRepository.deleteByUserIdAndRole(userId, ActiveRoles.TEACHER);
	}

	private TeacherRequestView toView(BackTeacherRoleRequestEntity entity) {
		BackUserEntity user = userRepository.findById(entity.getUserId()).orElse(null);
		String email = user == null ? "" : user.getEmail();
		String displayName = user == null || user.getDisplayName() == null ? "" : user.getDisplayName();
		return new TeacherRequestView(
				entity.getId(),
				entity.getUserId(),
				email,
				displayName,
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getReviewedAt());
	}
}

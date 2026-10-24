package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.TeacherApprovalOutcome;
import com.appv4.auth.application.port.out.TeacherModerationPort;
import com.appv4.auth.application.port.out.TeacherRequestPage;
import com.appv4.auth.application.port.out.TeacherRequestView;
import com.appv4.auth.application.service.ActiveRoles;

/**
 * Lit et accepte les demandes professeur (SCRUM-142).
 *
 * <p><b>Business :</b> pending et approved remontent dans la file.
 * Le rôle teacher n'est écrit qu'au oui, et une seule fois.
 * <p><b>Technique :</b> JPA sur les deux tables déjà créées en V5.
 * Liste : un lot utilisateurs ({@code findAllById}) — pas de N+1 (issue #56) ;
 * lecture bornée en base (issue #55).
 * Absent du profil {@code test} : les tests web ne démarrent pas JPA.
 * <p><b>Métier / flow :</b> adapter out du port {@link TeacherModerationPort}.
 */
@Component
@Profile("!test")
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
	public TeacherRequestPage findModerationQueue(int limit) {
		var pageable = PageRequest.of(
				0,
				limit + 1,
				Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
		List<BackTeacherRoleRequestEntity> fetched = requestRepository
				.findByStatusIn(List.of(PENDING, APPROVED, REVOKED), pageable);
		boolean truncated = fetched.size() > limit;
		List<BackTeacherRoleRequestEntity> requests = truncated
				? new ArrayList<>(fetched.subList(0, limit))
				: fetched;
		if (requests.isEmpty()) {
			return new TeacherRequestPage(List.of(), false);
		}
		List<Long> userIds = requests.stream()
				.map(BackTeacherRoleRequestEntity::getUserId)
				.distinct()
				.toList();
		Map<Long, BackUserEntity> usersById = userRepository.findAllById(userIds).stream()
				.collect(Collectors.toMap(BackUserEntity::getId, Function.identity()));
		List<TeacherRequestView> items = requests.stream()
				.map(request -> toView(request, usersById.get(request.getUserId())))
				.toList();
		return new TeacherRequestPage(items, truncated);
	}

	@Override
	public Optional<TeacherRequestView> findById(long requestId) {
		return requestRepository.findById(requestId)
				.map(entity -> toView(entity, userRepository.findById(entity.getUserId()).orElse(null)));
	}

	@Override
	public TeacherApprovalOutcome markApproved(long requestId, Instant reviewedAt) {
		if (requestRepository.approveFromOpen(requestId, reviewedAt) == 1) {
			return TeacherApprovalOutcome.FIRST_APPROVAL;
		}
		if (requestRepository.refreshApproved(requestId, reviewedAt) == 1) {
			return TeacherApprovalOutcome.REVALIDATED;
		}
		throw new AuthDomainException(HttpStatus.NOT_FOUND.value(), "Demande introuvable.");
	}

	@Override
	public void markRevoked(long requestId, Instant revokedAt) {
		BackTeacherRoleRequestEntity entity = requestRepository.findById(requestId)
				.orElseThrow(() -> new AuthDomainException(HttpStatus.NOT_FOUND.value(), "Demande introuvable."));
		entity.setStatus(REVOKED);
		entity.setRevokedAt(revokedAt);
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

	private TeacherRequestView toView(BackTeacherRoleRequestEntity entity, BackUserEntity user) {
		String email = user == null ? "" : user.getEmail();
		String displayName = user == null || user.getDisplayName() == null ? "" : user.getDisplayName();
		return new TeacherRequestView(
				entity.getId(),
				entity.getUserId(),
				email,
				displayName,
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getReviewedAt(),
				entity.getRevokedAt());
	}
}

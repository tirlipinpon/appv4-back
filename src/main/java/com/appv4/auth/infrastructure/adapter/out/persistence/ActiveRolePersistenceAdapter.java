package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.service.ActiveRoles;

/**
 * Lit et écrit le rôle actif (SCRUM-124).
 *
 * <p><b>Business :</b> seuls parent et professeur comptent comme espaces.
 * Une demande professeur pending n'est pas un rôle.
 * <p><b>Technique :</b> {@code back_user_roles} + colonne {@code last_active_role}.
 * Absent du profil {@code test} : les tests web ne démarrent pas JPA.
 * <p><b>Métier / flow :</b> adapter out du port {@link ActiveRolePort}.
 */
@Component
@Profile("!test")
public class ActiveRolePersistenceAdapter implements ActiveRolePort {

	private final BackUserJpaRepository userRepository;
	private final BackUserRoleJpaRepository roleRepository;
	private final BackTeacherRoleRequestJpaRepository requestRepository;

	public ActiveRolePersistenceAdapter(
			BackUserJpaRepository userRepository,
			BackUserRoleJpaRepository roleRepository,
			BackTeacherRoleRequestJpaRepository requestRepository) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.requestRepository = requestRepository;
	}

	@Override
	public List<String> findSpaceRoles(long userId) {
		return roleRepository.findByUserId(userId).stream()
				.map(BackUserRoleEntity::getRole)
				.filter(ActiveRoles::isSpace)
				.distinct()
				.toList();
	}

	@Override
	public boolean hasPendingTeacherRequest(long userId) {
		return requestRepository.existsByUserIdAndStatus(userId, "pending");
	}

	/**
	 * Statut à montrer dans l'en-tête (SCRUM-152).
	 *
	 * <p><b>Business :</b> une demande en attente compte plus qu'un ancien oui ou un retrait.
	 * <p><b>Technique :</b> lit les statuts de {@code back_teacher_role_requests}.
	 * <p><b>Métier / flow :</b> {@code GET /api/int/auth/spaces} après connexion ou F5.
	 */
	@Override
	public String teacherRequestStatus(long userId) {
		List<String> statuses = requestRepository.findStatusesByUserId(userId);
		if (statuses.contains("pending")) {
			return "pending";
		}
		if (statuses.contains("approved")) {
			return "approved";
		}
		if (statuses.contains("revoked")) {
			return "revoked";
		}
		return "none";
	}

	@Override
	public boolean hasAdminRole(long userId) {
		return roleRepository.existsByUserIdAndRole(userId, ActiveRoles.ADMIN);
	}

	/**
	 * Pose le rôle admin (seed local ou futur grant explicite).
	 *
	 * <p><b>Business :</b> sans transaction, l'insert native échoue au boot local.
	 * <p><b>Technique :</b> {@code @Modifying} + flush → exige un EntityManager transactionnel.
	 * <p><b>Métier / flow :</b> {@link com.appv4.auth.application.service.LocalAdminSeed}
	 * → ce port → {@code insertIgnoreConflict}.
	 */
	@Override
	@Transactional
	public void grantAdmin(long userId) {
		roleRepository.insertIgnoreConflict(userId, ActiveRoles.ADMIN);
	}

	@Override
	public Optional<String> findLastActiveRole(long userId) {
		return userRepository.findById(userId)
				.map(BackUserEntity::getLastActiveRole)
				.filter(role -> !role.isBlank());
	}

	@Override
	public void saveLastActiveRole(long userId, String role) {
		BackUserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.UNAUTHORIZED,
						"Session invalide."));
		user.setLastActiveRole(role);
		userRepository.save(user);
	}

	@Override
	public AuthzSnapshot loadAuthzSnapshot(long userId) {
		List<String> allRoles = roleRepository.findByUserId(userId).stream()
				.map(BackUserRoleEntity::getRole)
				.distinct()
				.toList();
		List<String> spaces = allRoles.stream().filter(ActiveRoles::isSpace).toList();
		boolean admin = allRoles.contains(ActiveRoles.ADMIN);
		String last = userRepository.findById(userId)
				.map(BackUserEntity::getLastActiveRole)
				.filter(role -> role != null && !role.isBlank())
				.orElse(null);
		boolean pending = spaces.isEmpty() && requestRepository.existsByUserIdAndStatus(userId, "pending");
		return new AuthzSnapshot(spaces, admin, last, pending);
	}
}

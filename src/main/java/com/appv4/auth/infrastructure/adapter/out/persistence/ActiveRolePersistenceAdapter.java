package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.exception.AuthDomainException;
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

	@Override
	public boolean hasAdminRole(long userId) {
		return roleRepository.existsByUserIdAndRole(userId, ActiveRoles.ADMIN);
	}

	@Override
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
				.orElseThrow(() -> new AuthDomainException(
						HttpStatus.UNAUTHORIZED.value(),
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

package com.appv4.auth.infrastructure.adapter.out.persistence;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.SignupRolePort;

/**
 * Écrit le rôle parent et la demande professeur (SCRUM-121).
 *
 * <p><b>Business :</b> le professeur n'est pas accordé ici. Le flag
 * {@code initial_roles_completed} dit si le choix d'onboarding a déjà eu lieu
 * même après révocation des rôles (issue #35).
 * <p><b>Technique :</b> insert JPA, unicité (user, rôle), une demande
 * {@code pending} par compte ; colonne {@code back_users.initial_roles_completed}.
 * Absent du profil {@code test} : les tests web ne démarrent pas JPA.
 */
@Component
@Profile("!test")
public class SignupRolePersistenceAdapter implements SignupRolePort {

	static final String PARENT = "parent";

	private final BackUserJpaRepository userRepository;
	private final BackUserRoleJpaRepository roleRepository;
	private final BackTeacherRoleRequestJpaRepository requestRepository;

	public SignupRolePersistenceAdapter(
			BackUserJpaRepository userRepository,
			BackUserRoleJpaRepository roleRepository,
			BackTeacherRoleRequestJpaRepository requestRepository) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.requestRepository = requestRepository;
	}

	@Override
	public void grantParent(long userId) {
		roleRepository.insertIgnoreConflict(userId, PARENT);
	}

	@Override
	public void requestTeacher(long userId) {
		requestRepository.insertPendingIfAbsent(userId);
	}

	@Override
	public boolean hasCompletedInitialChoice(long userId) {
		return userRepository.findById(userId)
				.map(BackUserEntity::isInitialRolesCompleted)
				.orElse(false);
	}

	@Override
	public void markInitialChoiceCompleted(long userId) {
		BackUserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new AuthDomainException(
						HttpStatus.UNAUTHORIZED.value(),
						"Session invalide."));
		if (!user.isInitialRolesCompleted()) {
			user.setInitialRolesCompleted(true);
			userRepository.save(user);
		}
	}
}

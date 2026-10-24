package com.appv4.auth.infrastructure.adapter.out.persistence;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.SignupRolePort;

/**
 * Écrit le rôle parent et la demande professeur (SCRUM-121).
 *
 * <p><b>Business :</b> le professeur n'est pas accordé ici.
 * <p><b>Technique :</b> insert JPA, en respectant l'unicité (user, rôle)
 * et une seule demande {@code pending} par compte.
 * Absent du profil {@code test} : les tests web ne démarrent pas JPA.
 */
@Component
@Profile("!test")
public class SignupRolePersistenceAdapter implements SignupRolePort {

	static final String PARENT = "parent";

	private final BackUserRoleJpaRepository roleRepository;
	private final BackTeacherRoleRequestJpaRepository requestRepository;

	public SignupRolePersistenceAdapter(
			BackUserRoleJpaRepository roleRepository,
			BackTeacherRoleRequestJpaRepository requestRepository) {
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
}

package com.appv4.hello.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.hello.application.port.out.HelloMessageRepository;
import com.appv4.hello.domain.model.HelloAudience;
import com.appv4.hello.domain.model.HelloMessage;

/**
 * Adaptateur sortant : implémente le port métier via Spring Data JPA.
 * Absent du profil {@code test} : les tests web ne démarrent pas JPA.
 */
@Component
@Profile("!test")
class HelloMessagePersistenceAdapter implements HelloMessageRepository {

	private final FrontHelloMessageJpaRepository frontHelloMessageJpaRepository;
	private final BackHelloMessageJpaRepository backHelloMessageJpaRepository;
	private final HelloMessagePersistenceMapper mapper = new HelloMessagePersistenceMapper();

	HelloMessagePersistenceAdapter(
			FrontHelloMessageJpaRepository frontHelloMessageJpaRepository,
			BackHelloMessageJpaRepository backHelloMessageJpaRepository) {
		this.frontHelloMessageJpaRepository = frontHelloMessageJpaRepository;
		this.backHelloMessageJpaRepository = backHelloMessageJpaRepository;
	}

	@Override
	public Optional<HelloMessage> findLatest(HelloAudience audience) {
		return switch (audience) {
			case LEARNING -> frontHelloMessageJpaRepository.findFirstByOrderByIdDesc().map(mapper::toDomain);
			case ADMIN -> backHelloMessageJpaRepository.findFirstByOrderByIdDesc().map(mapper::toDomain);
		};
	}
}

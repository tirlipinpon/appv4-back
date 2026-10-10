package com.appv4.hello.application.port.out;

import java.util.Optional;

import com.appv4.hello.domain.model.HelloAudience;
import com.appv4.hello.domain.model.HelloMessage;

/**
 * Port sortant : lire un message hello selon l'audience.
 * Implémentation JPA dans {@code hello.infrastructure.adapter.out.persistence}.
 */
public interface HelloMessageRepository {

	Optional<HelloMessage> findLatest(HelloAudience audience);
}

package com.appv4.hello.infrastructure.adapter.out.persistence;

import com.appv4.hello.domain.model.HelloMessage;

/**
 * Mapping Entity JPA ↔ domaine. Mapping trivial — pas de MapStruct pour l'instant.
 */
final class HelloMessagePersistenceMapper {

	HelloMessage toDomain(HelloMessageEntityBase entity) {
		return new HelloMessage(entity.getText());
	}
}

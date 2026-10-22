package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Accès JPA à {@code back_user_identities}.
 *
 * <p>Lookup OAuth : « ce Google subject existe déjà ? » → retrouver le
 * {@code back_users} lié (login social / linking, tickets suivants).
 */
public interface BackUserIdentityJpaRepository extends JpaRepository<BackUserIdentityEntity, Long> {

	Optional<BackUserIdentityEntity> findByProviderAndProviderSubject(String provider, String providerSubject);
}

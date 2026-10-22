package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data — {@code back_user_identities}.
 */
public interface BackUserIdentityJpaRepository extends JpaRepository<BackUserIdentityEntity, Long> {

	Optional<BackUserIdentityEntity> findByProviderAndProviderSubject(String provider, String providerSubject);
}

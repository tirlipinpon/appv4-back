package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Accès JPA à {@code back_user_identities}.
 *
 * <p>Lookup OAuth : « ce Google subject existe déjà ? » → retrouver le
 * {@code back_users} lié (login social / linking, tickets suivants).
 */
public interface BackUserIdentityJpaRepository extends JpaRepository<BackUserIdentityEntity, Long> {

	Optional<BackUserIdentityEntity> findByProviderAndProviderSubject(String provider, String providerSubject);

	@Query("select i from BackUserIdentityEntity i where i.user.id = :userId")
	List<BackUserIdentityEntity> findByUserId(@Param("userId") Long userId);
}

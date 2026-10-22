package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.AuthUserAccountPort;

/**
 * Adaptateur JPA — {@code back_users} + {@code back_user_identities}.
 */
@Component
public class AuthUserAccountPersistenceAdapter implements AuthUserAccountPort {

	private final BackUserJpaRepository userRepository;
	private final BackUserIdentityJpaRepository identityRepository;

	public AuthUserAccountPersistenceAdapter(
			BackUserJpaRepository userRepository,
			BackUserIdentityJpaRepository identityRepository) {
		this.userRepository = userRepository;
		this.identityRepository = identityRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AuthUserAccount> findByProviderSubject(String provider, String providerSubject) {
		return identityRepository.findByProviderAndProviderSubject(provider, providerSubject)
				.map(identity -> toAccount(identity.getUser()));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AuthUserAccount> findByEmail(String email) {
		return userRepository.findByEmail(email).map(this::toAccount);
	}

	@Override
	@Transactional
	public AuthUserAccount createWithIdentity(
			String email,
			String displayName,
			String provider,
			String providerSubject,
			String emailAtProvider) {
		BackUserEntity user = new BackUserEntity();
		user.setEmail(email);
		user.setDisplayName(displayName);
		user.setActive(true);
		user.setPasswordHash(null);
		BackUserEntity saved = userRepository.save(user);

		BackUserIdentityEntity identity = new BackUserIdentityEntity();
		identity.setUser(saved);
		identity.setProvider(provider);
		identity.setProviderSubject(providerSubject);
		identity.setEmailAtProvider(emailAtProvider);
		identityRepository.save(identity);

		return toAccount(saved);
	}

	@Override
	@Transactional
	public void linkIdentity(long userId, String provider, String providerSubject, String emailAtProvider) {
		BackUserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new AuthDomainException(
						HttpStatus.INTERNAL_SERVER_ERROR.value(),
						"User introuvable pour link identity"));
		if (identityRepository.findByProviderAndProviderSubject(provider, providerSubject).isPresent()) {
			return;
		}
		BackUserIdentityEntity identity = new BackUserIdentityEntity();
		identity.setUser(user);
		identity.setProvider(provider);
		identity.setProviderSubject(providerSubject);
		identity.setEmailAtProvider(emailAtProvider);
		identityRepository.save(identity);
	}

	private AuthUserAccount toAccount(BackUserEntity user) {
		return new AuthUserAccount(user.getId(), user.getEmail(), user.getDisplayName(), user.isActive());
	}
}

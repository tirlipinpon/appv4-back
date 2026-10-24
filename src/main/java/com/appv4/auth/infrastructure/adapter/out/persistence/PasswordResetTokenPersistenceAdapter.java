package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.PasswordResetTokenPort;

/**
 * Adaptateur JPA — {@code back_password_reset_tokens} (SCRUM-108 / SCRUM-111).
 * Absent du profil {@code test} : les tests web ne démarrent pas JPA.
 */
@Component
@Profile("!test")
public class PasswordResetTokenPersistenceAdapter implements PasswordResetTokenPort {

	private final BackPasswordResetTokenJpaRepository tokenRepository;
	private final BackUserJpaRepository userRepository;

	public PasswordResetTokenPersistenceAdapter(
			BackPasswordResetTokenJpaRepository tokenRepository,
			BackUserJpaRepository userRepository) {
		this.tokenRepository = tokenRepository;
		this.userRepository = userRepository;
	}

	@Override
	@Transactional
	public void invalidateUnusedForUser(long userId) {
		tokenRepository.markUnusedAsUsedForUser(userId);
	}

	@Override
	@Transactional
	public void create(long userId, String tokenHash, Instant expiresAt) {
		BackUserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new AuthDomainException(
						HttpStatus.INTERNAL_SERVER_ERROR.value(),
						"User introuvable pour reset token"));
		BackPasswordResetTokenEntity entity = new BackPasswordResetTokenEntity();
		entity.setUser(user);
		entity.setTokenHash(tokenHash);
		entity.setExpiresAt(expiresAt);
		tokenRepository.save(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ValidResetToken> findValidByTokenHash(String tokenHash) {
		return tokenRepository.findValidByTokenHash(tokenHash, Instant.now())
				.map(entity -> new ValidResetToken(entity.getId(), entity.getUser().getId()));
	}

	@Override
	@Transactional
	public boolean tryMarkUsed(long tokenId) {
		return tokenRepository.markUsedIfStillValid(tokenId, Instant.now()) == 1;
	}
}

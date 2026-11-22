package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.RecoveryPromptPort;
import com.appv4.auth.application.service.RecoveryPromptService;

/**
 * Marque et contact de la sollicitation, sur le compte.
 *
 * <p><b>Business :</b> passer ne change pas le téléphone ni le mail de contact.
 * La fiche parent ou professeur écrit ces champs sans poser la marque.
 * <p><b>Technique :</b> {@code back_users.recovery_prompt_seen}. Absent du profil test.
 * <p><b>Métier / flow :</b> adapter out du port de sollicitation.
 */
@Component
@Profile("!test")
public class RecoveryPromptPersistenceAdapter implements RecoveryPromptPort {

	private final BackUserJpaRepository users;

	public RecoveryPromptPersistenceAdapter(BackUserJpaRepository users) {
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<RecoveryPromptState> find(long userId) {
		return users.findById(userId)
				.map(user -> new RecoveryPromptState(
						user.getDisplayName(),
						user.getPhone(),
						user.getContactEmail(),
						user.isRecoveryPromptSeen()));
	}

	@Override
	@Transactional
	public void markAsked(long userId) {
		require(userId).setRecoveryPromptSeen(true);
	}

	@Override
	@Transactional
	public void saveAndAsk(long userId, String displayName, String phone, String contactEmail) {
		BackUserEntity user = require(userId);
		user.setDisplayName(displayName);
		user.setPhone(phone);
		user.setContactEmail(contactEmail);
		user.setRecoveryPromptSeen(true);
	}

	private BackUserEntity require(long userId) {
		return users.findById(userId)
				.orElseThrow(() -> new AuthDomainException(
						AuthErrorCode.NOT_FOUND, RecoveryPromptService.ACCOUNT_MISSING));
	}
}

package com.appv4.auth.infrastructure.adapter.out.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appv4.auth.application.port.out.PasswordResetMailPort;

/**
 * Envoi reset local (SCRUM-108) — log du lien (pas encore de SMTP).
 *
 * <p><b>Business :</b> en local on peut cliquer le lien depuis les logs.
 * <p><b>Technique :</b> remplacé plus tard par un adaptateur SendGrid/SMTP.
 */
public class LoggingPasswordResetMailAdapter implements PasswordResetMailPort {

	private static final Logger LOG = LoggerFactory.getLogger(LoggingPasswordResetMailAdapter.class);

	@Override
	public void sendResetLink(String email, String resetUrl) {
		LOG.info("Password reset link for {} → {}", email, resetUrl);
	}
}

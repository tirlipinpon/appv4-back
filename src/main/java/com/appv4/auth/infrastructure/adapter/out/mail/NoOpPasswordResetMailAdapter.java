package com.appv4.auth.infrastructure.adapter.out.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appv4.auth.application.port.out.PasswordResetMailPort;

/**
 * Transport hors local sans SMTP : n'envoie rien et n'écrit pas le lien/jeton.
 *
 * <p><b>Business :</b> l'API forgot reste anti-énumération ; pas de secret en log (issue #58).
 * <p><b>Technique :</b> remplacé plus tard par SendGrid/SMTP.
 */
public class NoOpPasswordResetMailAdapter implements PasswordResetMailPort {

	private static final Logger LOG = LoggerFactory.getLogger(NoOpPasswordResetMailAdapter.class);

	@Override
	public void sendResetLink(String email, String resetUrl) {
		LOG.info("Password reset requested (mail transport=noop ; lien non journalisé)");
	}
}

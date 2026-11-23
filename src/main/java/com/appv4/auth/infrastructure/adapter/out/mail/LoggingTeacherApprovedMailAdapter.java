package com.appv4.auth.infrastructure.adapter.out.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appv4.auth.application.port.out.TeacherApprovedMailPort;

/**
 * Email d'acceptation en local (SCRUM-142) — une ligne de log, pas encore de SMTP.
 *
 * <p><b>Business :</b> la personne est prévenue. En local, le message est dans les logs du back.
 * <p><b>Technique :</b> même principe que le lien de mot de passe oublié.
 */
public class LoggingTeacherApprovedMailAdapter implements TeacherApprovedMailPort {

	private static final Logger LOG = LoggerFactory.getLogger(LoggingTeacherApprovedMailAdapter.class);

	@Override
	public void sendTeacherApproved(String email) {
		LOG.info("Rôle professeur accepté pour {}", email);
	}
}

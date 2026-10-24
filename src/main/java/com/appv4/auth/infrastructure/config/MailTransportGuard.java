package com.appv4.auth.infrastructure.config;

import java.util.Arrays;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Interdit le transport {@code logging} hors profils local/test.
 *
 * <p><b>Business :</b> le lien reset secret ne doit pas finir dans les logs d'un déploiement.
 * <p><b>Technique :</b> après bind de {@link MailTransportProperties}.
 * <p><b>Métier / flow :</b> boot → avant forgot-password.
 */
@Component
public class MailTransportGuard implements InitializingBean {

	private final MailTransportProperties mailTransportProperties;
	private final Environment environment;

	public MailTransportGuard(MailTransportProperties mailTransportProperties, Environment environment) {
		this.mailTransportProperties = mailTransportProperties;
		this.environment = environment;
	}

	@Override
	public void afterPropertiesSet() {
		String transport = mailTransportProperties.getTransport();
		if (transport == null || transport.isBlank()) {
			throw new IllegalStateException("appv4.mail.transport manquant (logging|noop)");
		}
		if (!mailTransportProperties.isLogging() && !mailTransportProperties.isNoop()) {
			throw new IllegalStateException(
					"appv4.mail.transport invalide : " + transport + " (attendu : logging|noop)");
		}
		if (mailTransportProperties.isLogging() && !allowsLoggingTransport()) {
			throw new IllegalStateException(
					"Transport mail « logging » interdit hors profils local/test"
							+ " (écrit le lien reset dans les logs) : utiliser noop ou un SMTP réel");
		}
	}

	boolean allowsLoggingTransport() {
		var profiles = Arrays.asList(environment.getActiveProfiles());
		return profiles.contains("local") || profiles.contains("test");
	}
}

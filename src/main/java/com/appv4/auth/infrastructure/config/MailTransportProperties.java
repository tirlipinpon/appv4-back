package com.appv4.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Choix du transport mail (issue #58).
 *
 * <p><b>Business :</b> hors local, ne jamais écrire le lien reset (jeton) dans les logs.
 * <p><b>Technique :</b> {@code logging} = adaptateurs de démo ; {@code noop} = silencieux
 * en attendant SMTP.
 */
@ConfigurationProperties(prefix = "appv4.mail")
public class MailTransportProperties {

	public static final String TRANSPORT_LOGGING = "logging";
	public static final String TRANSPORT_NOOP = "noop";

	/**
	 * {@code logging} (local / test) ou {@code noop} (défaut sûr hors local).
	 */
	private String transport = TRANSPORT_NOOP;

	public String getTransport() {
		return transport;
	}

	public void setTransport(String transport) {
		this.transport = transport;
	}

	public boolean isLogging() {
		return TRANSPORT_LOGGING.equalsIgnoreCase(transport);
	}

	public boolean isNoop() {
		return TRANSPORT_NOOP.equalsIgnoreCase(transport);
	}
}

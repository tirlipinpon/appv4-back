package com.appv4.auth.application.port.out;

/**
 * Port sortant — envoi du lien reset (SCRUM-108).
 *
 * <p>Localement : log du lien. Plus tard : SendGrid / SMTP réel.
 */
public interface PasswordResetMailPort {

	void sendResetLink(String email, String resetUrl);
}

package com.appv4.auth.application.port.out;

/**
 * Port sortant — prévenir la personne que son rôle professeur est accepté (SCRUM-142).
 *
 * <p><b>Business :</b> après un oui, elle doit l'apprendre sans rouvrir l'app au hasard.
 * <p><b>Technique :</b> en local, un log. Plus tard, un vrai envoi.
 */
public interface TeacherApprovedMailPort {

	void sendTeacherApproved(String email);
}

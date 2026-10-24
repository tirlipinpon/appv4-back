package com.appv4.auth.application.port.in;

import com.appv4.auth.application.port.out.TeacherRequestPage;
import com.appv4.auth.application.port.out.TeacherRequestView;

/**
 * Port entrant — modérer les demandes professeur (SCRUM-142).
 *
 * <p><b>Business :</b> seul un admin voit la file et peut dire oui.
 * <p><b>Technique :</b> 403 si le compte n'a pas le rôle admin.
 * <p><b>Métier / flow :</b> controller admin → ce port → persistance + email.
 */
public interface TeacherModerationUseCase {

	/** @param limit taille max demandée (bornée côté service) */
	TeacherRequestPage list(long callerId, int limit);

	TeacherRequestView approve(long callerId, long requestId);

	TeacherRequestView revoke(long callerId, long requestId);
}

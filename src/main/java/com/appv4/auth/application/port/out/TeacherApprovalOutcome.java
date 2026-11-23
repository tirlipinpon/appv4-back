package com.appv4.auth.application.port.out;

/**
 * Résultat d'une écriture d'approbation atomique (issue #46).
 *
 * <p>{@link #FIRST_APPROVAL} : la demande était pending/revoked → un seul appel
 * concurrent gagne et doit notifier. {@link #REVALIDATED} : déjà approved
 * (répétition ou course perdue) → pas d'email.
 */
public enum TeacherApprovalOutcome {
	FIRST_APPROVAL,
	REVALIDATED
}

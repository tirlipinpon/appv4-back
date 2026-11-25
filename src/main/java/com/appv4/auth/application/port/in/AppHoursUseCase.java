package com.appv4.auth.application.port.in;

/**
 * Lire et changer les horaires de l'app enfant.
 *
 * <p><b>Business :</b> seul l'admin choisit l'heure de début et l'heure de fin.
 * Un parent ou un professeur ne les change pas.
 * <p><b>Technique :</b> les heures voyagent en {@code HH:mm}. La phrase de fermeture
 * est calculée ici, le front l'affiche telle quelle.
 * <p><b>Métier / flow :</b> page admin → ce port → la ligne des horaires.
 */
public interface AppHoursUseCase {

	AppHoursView read(long callerId);

	AppHoursView update(long callerId, String opensAt, String closesAt);

	/**
	 * @param opensAt       heure de début, {@code HH:mm}
	 * @param closesAt      heure de fin, {@code HH:mm}
	 * @param closedMessage phrase que l'enfant lit pendant la fermeture
	 */
	record AppHoursView(String opensAt, String closesAt, String closedMessage) {
	}
}

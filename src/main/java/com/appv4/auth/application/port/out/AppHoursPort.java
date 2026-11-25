package com.appv4.auth.application.port.out;

import java.time.LocalTime;

import com.appv4.auth.application.AppHours;

/**
 * Horaires d'ouverture de l'app enfant.
 *
 * <p><b>Business :</b> une seule fenêtre pour tous les enfants. L'admin l'écrit.
 * <p><b>Technique :</b> {@link #current()} relit la ligne à chaque appel.
 * <p><b>Métier / flow :</b> port out du séjour, du login enfant, et de la page admin.
 */
public interface AppHoursPort {

	AppHours current();

	void save(LocalTime opensAt, LocalTime closesAt);

	/** Fenêtre figée, pour les tests qui ne passent pas par la base. */
	static AppHoursPort fixed(AppHours hours) {
		return new AppHoursPort() {
			@Override
			public AppHours current() {
				return hours;
			}

			@Override
			public void save(LocalTime opensAt, LocalTime closesAt) {
				throw new UnsupportedOperationException("Ces horaires sont figés.");
			}
		};
	}
}

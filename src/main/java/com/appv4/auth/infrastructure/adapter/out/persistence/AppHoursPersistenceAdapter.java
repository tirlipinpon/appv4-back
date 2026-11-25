package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.LocalTime;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.AppHours;
import com.appv4.auth.application.port.out.AppHoursPort;

/**
 * Ligne unique des horaires de l'app enfant.
 *
 * <p><b>Business :</b> tout le monde lit la même fenêtre. Seul l'admin l'écrit,
 * et le cas d'usage a déjà vérifié le rôle.
 * <p><b>Technique :</b> JDBC sur {@code back_app_hours}, identifiant 1.
 * Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out de {@link AppHoursPort}.
 */
@Component
@Profile("!test")
public class AppHoursPersistenceAdapter implements AppHoursPort {

	private final JdbcClient jdbc;

	public AppHoursPersistenceAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public AppHours current() {
		return jdbc.sql("select opens_at, closes_at from back_app_hours where id = 1")
				.query((rs, row) -> new AppHours(
						rs.getObject("opens_at", LocalTime.class),
						rs.getObject("closes_at", LocalTime.class)))
				.single();
	}

	@Override
	public void save(LocalTime opensAt, LocalTime closesAt) {
		jdbc.sql("""
				update back_app_hours
				set opens_at = :opensAt, closes_at = :closesAt
				where id = 1
				""")
				.param("opensAt", opensAt)
				.param("closesAt", closesAt)
				.update();
	}
}

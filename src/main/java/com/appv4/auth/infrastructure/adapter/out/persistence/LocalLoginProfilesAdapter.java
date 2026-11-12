package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.appv4.auth.application.port.out.LocalLoginProfilesPort;

/** Lecture groupée réservée au développement local. Aucun hash n'est lu. */
@Repository
@Profile("local & !prod & !production & !test")
public class LocalLoginProfilesAdapter implements LocalLoginProfilesPort {

	private final JdbcTemplate jdbc;

	public LocalLoginProfilesAdapter(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<LocalLoginProfilesPort.Profile> list() {
		return jdbc.query("""
				SELECT u.id, u.email, u.display_name, u.is_active,
				       coalesce(string_agg(r.role, ' + ' ORDER BY r.role), '') AS roles
				FROM back_users u LEFT JOIN back_user_roles r ON r.user_id = u.id
				GROUP BY u.id ORDER BY lower(u.display_name), u.email, u.id
				""", (row, index) -> new LocalLoginProfilesPort.Profile(
						row.getLong("id"), row.getString("email"), row.getString("display_name"),
						row.getString("roles"), row.getBoolean("is_active")));
	}
}

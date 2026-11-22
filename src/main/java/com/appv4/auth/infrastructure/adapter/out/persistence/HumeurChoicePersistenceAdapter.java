package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.HumeurChoicePort;

/**
 * Bibliothèque des humeurs (BACK-68).
 *
 * <p><b>Business :</b> une seule liste pour tous les enfants. L'ordre est celui
 * de la création. Modifier un choix ne change pas sa place.
 * <p><b>Technique :</b> JDBC sur {@code back_humeur_choices}. Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out de {@link HumeurChoicePort}.
 */
@Component
@Profile("!test")
public class HumeurChoicePersistenceAdapter implements HumeurChoicePort {

	private final JdbcClient jdbc;

	public HumeurChoicePersistenceAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<HumeurChoice> list() {
		return jdbc.sql("select id, icon, word from back_humeur_choices order by id")
				.query((rs, row) -> new HumeurChoice(rs.getLong("id"), rs.getString("icon"), rs.getString("word")))
				.list();
	}

	@Override
	public HumeurChoice insert(String icon, String word) {
		long id = jdbc.sql("""
				insert into back_humeur_choices (icon, word)
				values (:icon, :word)
				returning id
				""")
				.param("icon", icon)
				.param("word", word)
				.query(Long.class)
				.single();
		return new HumeurChoice(id, icon, word);
	}

	@Override
	public Optional<HumeurChoice> find(long id) {
		return jdbc.sql("select id, icon, word from back_humeur_choices where id = :id")
				.param("id", id)
				.query((rs, row) -> new HumeurChoice(rs.getLong("id"), rs.getString("icon"), rs.getString("word")))
				.optional();
	}

	@Override
	public HumeurChoice update(long id, String icon, String word) {
		jdbc.sql("update back_humeur_choices set icon = :icon, word = :word where id = :id")
				.param("icon", icon)
				.param("word", word)
				.param("id", id)
				.update();
		return new HumeurChoice(id, icon, word);
	}

	@Override
	public void delete(long id) {
		jdbc.sql("delete from back_humeur_choices where id = :id")
				.param("id", id)
				.update();
	}
}

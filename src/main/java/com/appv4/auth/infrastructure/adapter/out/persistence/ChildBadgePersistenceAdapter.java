package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.ChildBadgePort;

/**
 * Niveaux de badges déjà gardés (BACK-19).
 *
 * <p><b>Business :</b> la collection relit ces lignes. Une date vide n'est pas « aujourd'hui ».
 * L'instant du dernier niveau sert à classer les gains du jour.
 * <p><b>Technique :</b> JDBC sur {@code back_child_badges} et {@code back_child_badge_inits}.
 * Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out du port badges.
 */
@Component
@Profile("!test")
public class ChildBadgePersistenceAdapter implements ChildBadgePort {

	private final JdbcClient jdbc;

	public ChildBadgePersistenceAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public boolean initialized(long childId) {
		return jdbc.sql("select 1 from back_child_badge_inits where child_id = :childId")
				.param("childId", childId)
				.query(Integer.class)
				.optional()
				.isPresent();
	}

	@Override
	public void markInitialized(long childId) {
		jdbc.sql("insert into back_child_badge_inits (child_id) values (:childId) on conflict do nothing")
				.param("childId", childId)
				.update();
	}

	@Override
	public Map<String, StoredBadge> load(long childId) {
		Map<String, StoredBadge> found = new HashMap<>();
		jdbc.sql("""
				select code, level, retained_value, unlocked_on, subject_id, gained_at
				from back_child_badges where child_id = :childId
				""")
				.param("childId", childId)
				.query((rs, row) -> {
					String code = rs.getString("code");
					LocalDate unlocked = rs.getObject("unlocked_on", LocalDate.class);
					Long subjectId = rs.getObject("subject_id", Long.class);
					Timestamp gained = rs.getTimestamp("gained_at");
					Instant gainedAt = gained == null ? null : gained.toInstant();
					found.put(code, new StoredBadge(
							code,
							rs.getInt("level"),
							rs.getInt("retained_value"),
							unlocked,
							subjectId,
							gainedAt));
					return null;
				})
				.list();
		return found;
	}

	@Override
	public void save(long childId, StoredBadge badge) {
		jdbc.sql("""
				insert into back_child_badges (child_id, code, level, retained_value, unlocked_on, subject_id, gained_at)
				values (:childId, :code, :level, :retained, :unlocked, :subjectId, :gainedAt)
				on conflict (child_id, code) do update
				set level = excluded.level,
				    retained_value = excluded.retained_value,
				    unlocked_on = excluded.unlocked_on,
				    subject_id = excluded.subject_id,
				    gained_at = excluded.gained_at
				""")
				.param("childId", childId)
				.param("code", badge.code())
				.param("level", badge.level())
				.param("retained", badge.retainedValue())
				.param("unlocked", badge.unlockedOn())
				.param("subjectId", badge.subjectId())
				.param("gainedAt", badge.gainedAt() == null ? null : Timestamp.from(badge.gainedAt()))
				.update();
	}
}

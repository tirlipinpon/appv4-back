package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.ChildRankingCasesPort;
import com.appv4.auth.application.port.out.ChildRankingRosterPort;

/**
 * Cases et enfants visibles du classement (BACK-22).
 *
 * <p><b>Business :</b> une fiche inactive ou masquée ne sort pas. Sans ligne de cases, tout est coché.
 * <p><b>Technique :</b> JDBC. Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out des deux ports du classement.
 */
@Component
@Profile("!test")
public class ChildRankingPersistenceAdapter implements ChildRankingCasesPort, ChildRankingRosterPort {

	private final JdbcTemplate jdbc;

	public ChildRankingPersistenceAdapter(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public RankingSelection selection(long childId) {
		return selection(List.of(childId)).getOrDefault(childId, RankingSelection.shown());
	}

	@Override
	public Map<Long, RankingSelection> selection(Collection<Long> childIds) {
		Map<Long, RankingSelection> found = new HashMap<>();
		if (childIds == null || childIds.isEmpty()) {
			return found;
		}
		String marks = String.join(",", childIds.stream().map(id -> "?").toList());
		jdbc.query(
				"select child_id, show_classe, show_annee, show_pays, show_tous, show_age, show_amitie "
						+ "from back_child_ranking_cases where child_id in (" + marks + ")",
				rs -> {
					found.put(rs.getLong("child_id"), new RankingSelection(
							rs.getBoolean("show_classe"),
							rs.getBoolean("show_annee"),
							rs.getBoolean("show_pays"),
							rs.getBoolean("show_tous"),
							rs.getBoolean("show_age"),
							rs.getBoolean("show_amitie")));
				},
				childIds.toArray());
		for (Long childId : childIds) {
			found.putIfAbsent(childId, RankingSelection.shown());
		}
		return found;
	}

	@Override
	public void save(long childId, RankingSelection selection) {
		jdbc.update(
				"""
				insert into back_child_ranking_cases
				(child_id, show_classe, show_annee, show_pays, show_tous, show_age, show_amitie)
				values (?, ?, ?, ?, ?, ?, ?)
				on conflict (child_id) do update set
				show_classe = excluded.show_classe,
				show_annee = excluded.show_annee,
				show_pays = excluded.show_pays,
				show_tous = excluded.show_tous,
				show_age = excluded.show_age,
				show_amitie = excluded.show_amitie
				""",
				childId,
				selection.classe(),
				selection.annee(),
				selection.pays(),
				selection.tous(),
				selection.age(),
				selection.amitie());
	}

	@Override
	public List<VisibleChild> visibleChildren() {
		return jdbc.query(
				"""
				select c.id, c.first_name, c.age, c.grade_label, c.school_id, s.name as school_name,
				       s.country, coalesce(p.xp_total, 0) as xp_total, c.avatar_code,
				       c.photo is not null as has_photo, c.created_at
				from back_children c
				left join back_schools s on s.id = c.school_id
				left join back_child_progress p on p.child_id = c.id
				where c.active and not c.hidden
				""",
				(rs, row) -> new VisibleChild(
						rs.getLong("id"),
						rs.getString("first_name"),
						rs.getInt("age"),
						rs.getString("grade_label"),
						rs.getObject("school_id") == null ? null : rs.getLong("school_id"),
						rs.getString("school_name"),
						rs.getString("country"),
						rs.getInt("xp_total"),
						rs.getString("avatar_code"),
						rs.getBoolean("has_photo"),
						toInstant(rs.getTimestamp("created_at"))));
	}

	@Override
	public Optional<ChildPhoto> photo(long childId) {
		List<ChildPhoto> rows = jdbc.query(
				"""
				select photo, photo_content_type
				from back_children
				where id = ? and active and not hidden and photo is not null
				""",
				(rs, row) -> new ChildPhoto(rs.getBytes("photo"), rs.getString("photo_content_type")),
				childId);
		return rows.stream().findFirst();
	}

	private static Instant toInstant(Timestamp timestamp) {
		return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
	}
}

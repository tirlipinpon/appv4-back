package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.HumeurAnswerPort;

/**
 * Réponses d'humeur (BACK-69).
 *
 * <p><b>Business :</b> la dernière réponse, par instant puis identifiant, fixe le prochain délai.
 * <p><b>Technique :</b> JDBC sur {@code back_humeur_answers}. Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out de {@link HumeurAnswerPort}.
 */
@Component
@Profile("!test")
public class HumeurAnswerPersistenceAdapter implements HumeurAnswerPort {

	private final JdbcClient jdbc;

	public HumeurAnswerPersistenceAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<StoredAnswer> history(long childId) {
		return jdbc.sql("""
				select id, child_id, choice_id, icon, word, closed, answered_at
				from back_humeur_answers
				where child_id = :childId
				order by answered_at desc, id desc
				""")
				.param("childId", childId)
				.query((rs, row) -> toAnswer(rs.getLong("id"), rs.getLong("child_id"),
						(Long) rs.getObject("choice_id"), rs.getString("icon"), rs.getString("word"),
						rs.getBoolean("closed"), rs.getTimestamp("answered_at").toInstant()))
				.list();
	}

	@Override
	public Optional<StoredAnswer> latest(long childId) {
		return jdbc.sql("""
				select id, child_id, choice_id, icon, word, closed, answered_at
				from back_humeur_answers
				where child_id = :childId
				order by answered_at desc, id desc
				limit 1
				""")
				.param("childId", childId)
				.query((rs, row) -> toAnswer(rs.getLong("id"), rs.getLong("child_id"),
						(Long) rs.getObject("choice_id"), rs.getString("icon"), rs.getString("word"),
						rs.getBoolean("closed"), rs.getTimestamp("answered_at").toInstant()))
				.optional();
	}

	@Override
	public StoredAnswer insert(
			long childId, Long choiceId, String icon, String word, boolean closed, Instant answeredAt) {
		long id = jdbc.sql("""
				insert into back_humeur_answers (child_id, choice_id, icon, word, closed, answered_at)
				values (:childId, :choiceId, :icon, :word, :closed, :answeredAt)
				returning id
				""")
				.param("childId", childId)
				.param("choiceId", choiceId)
				.param("icon", icon)
				.param("word", word)
				.param("closed", closed)
				.param("answeredAt", Timestamp.from(answeredAt))
				.query(Long.class)
				.single();
		return new StoredAnswer(id, childId, choiceId, icon, word, closed, answeredAt);
	}

	private static StoredAnswer toAnswer(
			long id, long childId, Long choiceId, String icon, String word, boolean closed, Instant answeredAt) {
		return new StoredAnswer(id, childId, choiceId, icon, word, closed, answeredAt);
	}
}

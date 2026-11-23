package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.ChildSimonChallengePort;

/** Persistance des suites Simon, consommées dans la transaction du verdict. */
@Component
@Profile("!test")
public class ChildSimonChallengeAdapter implements ChildSimonChallengePort {

	private static final String TTL = "interval '30 minutes'";

	private final JdbcClient jdbc;

	public ChildSimonChallengeAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public UUID issue(long childId, long questionId, List<Integer> sequence, boolean legacy) {
		jdbc.sql("""
				delete from back_child_simon_challenges where challenge_id in (
				    select challenge_id from back_child_simon_challenges
				    where created_at < current_timestamp - %s
				    order by created_at limit 100 for update skip locked
				)
				""".formatted(TTL)).update();
		UUID id = UUID.randomUUID();
		String insert = """
				insert into back_child_simon_challenges (challenge_id, child_id, question_id, sequence, legacy)
				values (:id, :child, :question, :sequence, :legacy)
				""";
		if (legacy) {
			insert += """
					on conflict (child_id, question_id) where legacy
					do update set challenge_id = excluded.challenge_id, sequence = excluded.sequence,
					              created_at = excluded.created_at
					""";
		}
		jdbc.sql(insert)
				.param("id", id)
				.param("child", childId)
				.param("question", questionId)
				.param("sequence", sequence.toArray(Integer[]::new))
				.param("legacy", legacy)
				.update();
		return id;
	}

	@Override
	public Optional<List<Integer>> take(long childId, long questionId, UUID challengeId) {
		String match = challengeId == null ? "legacy" : "challenge_id = :id";
		JdbcClient.StatementSpec statement = jdbc.sql("""
				delete from back_child_simon_challenges
				where child_id = :child and question_id = :question and %s
				  and created_at >= current_timestamp - %s
				returning sequence
				""".formatted(match, TTL))
				.param("child", childId)
				.param("question", questionId);
		if (challengeId != null) {
			statement.param("id", challengeId);
		}
		return statement.query((row, index) -> List.of((Integer[]) row.getArray("sequence").getArray())).optional();
	}
}

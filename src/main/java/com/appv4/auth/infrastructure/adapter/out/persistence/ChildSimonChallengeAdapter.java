package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.ChildSimonChallengePort;

/**
 * Garde en base la suite Simon tirée pour un enfant (BACK-55).
 *
 * <p><b>Business :</b> une nouvelle ouverture remplace la suite ; la note la retire.
 * <p><b>Technique :</b> une ligne {@code back_child_simon_challenges}.
 * <p><b>Métier / flow :</b> appelé par le jeu enfant à l'ouverture et à la tentative.
 */
@Component
@Profile("!test")
public class ChildSimonChallengeAdapter implements ChildSimonChallengePort {

	private final BackChildSimonChallengeJpaRepository repository;

	public ChildSimonChallengeAdapter(BackChildSimonChallengeJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public void save(long childId, long questionId, List<Integer> sequence) {
		Integer[] values = sequence.toArray(Integer[]::new);
		ChildSimonChallengeKey key = new ChildSimonChallengeKey(childId, questionId);
		repository.findById(key).ifPresentOrElse(
				row -> row.replace(values),
				() -> repository.save(new BackChildSimonChallengeEntity(childId, questionId, values)));
		repository.flush();
	}

	@Override
	public Optional<List<Integer>> find(long childId, long questionId) {
		return repository.findById(new ChildSimonChallengeKey(childId, questionId))
				.map(row -> List.copyOf(Arrays.asList(row.getSequence())));
	}

	@Override
	public void delete(long childId, long questionId) {
		repository.deleteById(new ChildSimonChallengeKey(childId, questionId));
	}
}

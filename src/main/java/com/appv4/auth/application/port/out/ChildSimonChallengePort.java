package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Suites Simon émises par le serveur. Une suite ne peut servir qu'à une tentative enfant.
 */
public interface ChildSimonChallengePort {

	UUID issue(long childId, long questionId, List<Integer> sequence, boolean legacy);

	/** Consomme atomiquement le défi dans la transaction de la tentative. */
	Optional<List<Integer>> take(long childId, long questionId, UUID challengeId);
}

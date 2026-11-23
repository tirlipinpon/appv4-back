package com.appv4.auth.application.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.ChildProgressPort;
import com.appv4.auth.application.port.out.ChildProgressPort.StoredProgress;

/**
 * Met à jour la progression après une tentative enfant (BACK-17).
 *
 * <p><b>Business :</b> XP et streak bougent uniquement quand le back accepte la partie.
 * <p><b>Technique :</b> upsert sur {@code back_child_progress}.
 * <p><b>Métier / flow :</b> appelé depuis {@link ChildQuestionPlayService#submit} après insert.
 */
public class UpdateChildProgressAfterAttemptService {

	private final ChildProgressPort childProgressPort;
	private final Clock clock;

	public UpdateChildProgressAfterAttemptService(ChildProgressPort childProgressPort, Clock clock) {
		this.childProgressPort = childProgressPort;
		this.clock = clock;
	}

	@Transactional
	public StoredProgress apply(long childId, int scorePercent) {
		StoredProgress current = childProgressPort.findForUpdate(childId)
				.orElseGet(() -> ChildProgressRules.empty(childId));
		LocalDate today = LocalDate.now(clock.withZone(ReadChildProgressService.PROGRESS_ZONE));
		StoredProgress next = ChildProgressRules.afterAttempt(current, scorePercent, today);
		return childProgressPort.save(next);
	}
}

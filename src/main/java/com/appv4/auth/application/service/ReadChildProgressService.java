package com.appv4.auth.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ReadChildProgressUseCase;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildProgressPort;
import com.appv4.auth.application.port.out.ChildProgressPort.StoredProgress;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.FirstYellow;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.service.ChildProgressRules.YellowGain;
import com.appv4.auth.application.result.ChildProgressView;
import com.appv4.auth.application.service.ChildProgressRules.ChildProgressViewSnapshot;

/**
 * Lecture de la progression enfant (BACK-17).
 *
 * <p><b>Business :</b> home affiche XP, streak, palier et semaine sans recalcul.
 * <p><b>Technique :</b> jeton enfant → fiche active → ligne progress (ou zéros).
 * <p><b>Métier / flow :</b> port in → ports out session / login / progress.
 */
public class ReadChildProgressService implements ReadChildProgressUseCase {

	public static final String INVALID_SESSION_MESSAGE = LoginChildService.INVALID_SESSION_MESSAGE;

	/** Fuseau métier des « jours » de streak / semaine. */
	public static final ZoneId PROGRESS_ZONE = ZoneId.of("Europe/Paris");

	private final ChildLoginPort childLoginPort;
	private final ChildSessionTokenPort childSessionTokenPort;
	private final ChildProgressPort childProgressPort;
	private final ChildQuestionAttemptPort childQuestionAttemptPort;
	private final Clock clock;

	public ReadChildProgressService(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildProgressPort childProgressPort,
			ChildQuestionAttemptPort childQuestionAttemptPort,
			Clock clock) {
		this.childLoginPort = childLoginPort;
		this.childSessionTokenPort = childSessionTokenPort;
		this.childProgressPort = childProgressPort;
		this.childQuestionAttemptPort = childQuestionAttemptPort;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public ChildProgressView progress(String rawToken) {
		long childId = requireActiveChild(rawToken);
		LocalDate today = LocalDate.now(clock.withZone(PROGRESS_ZONE));
		StoredProgress stored = childProgressPort.findByChildId(childId)
				.orElseGet(() -> ChildProgressRules.empty(childId));
		ChildProgressViewSnapshot snap = ChildProgressRules.toView(stored, today);
		return new ChildProgressView(
				snap.xpTotal(),
				snap.currentStreak(),
				snap.leagueLabel(),
				snap.weekDays(),
				ChildProgressRules.weekYellowCounts(yellowGains(childId), today));
	}

	private List<YellowGain> yellowGains(long childId) {
		List<FirstYellow> rows = childQuestionAttemptPort.firstYellows(childId);
		if (rows == null) {
			return List.of();
		}
		List<YellowGain> gains = new ArrayList<>();
		for (FirstYellow row : rows) {
			if (row == null || row.at() == null) {
				continue;
			}
			gains.add(new YellowGain(row.questionId(), row.at().atZone(PROGRESS_ZONE).toLocalDate()));
		}
		return gains;
	}

	private long requireActiveChild(String rawToken) {
		long childId = childSessionTokenPort.read(rawToken)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.UNAUTHORIZED, INVALID_SESSION_MESSAGE))
				.childId();
		if (childLoginPort.findActiveById(childId).isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.UNAUTHORIZED, INVALID_SESSION_MESSAGE);
		}
		return childId;
	}
}

package com.appv4.auth.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.AskChildHumeurUseCase;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AttemptTrace;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.HumeurAnswerPort;
import com.appv4.auth.application.port.out.HumeurAnswerPort.StoredAnswer;
import com.appv4.auth.application.port.out.HumeurChoicePort;
import com.appv4.auth.application.port.out.HumeurChoicePort.HumeurChoice;

/**
 * Demande d'humeur selon la tendance (BACK-69).
 *
 * <p><b>Business :</b> la première demande part tout de suite. Ensuite 24 h en baisse
 * ou sans tendance, 72 h en hausse, 100 h si c'est stable. La fermeture compte.
 * <p><b>Technique :</b> deux fenêtres de 7×24 h. L'écart est en millièmes de point
 * pour qu'un −7,9 reste stable et qu'un −8 soit une baisse.
 * <p><b>Métier / flow :</b> port in de {@code /api/ext/humeur}. Ne publie pas la tendance.
 */
public class AskChildHumeurService implements AskChildHumeurUseCase {

	static final String INVALID_SESSION_MESSAGE = LoginChildService.INVALID_SESSION_MESSAGE;
	static final String NOT_A_CHOICE = "Ce choix n'est plus proposé.";
	static final String ONE_SHAPE = "Un envoi est un choix ou une fermeture.";

	private static final Duration WEEK = Duration.ofHours(7 * 24);
	private static final Duration SOON = Duration.ofHours(24);
	private static final Duration RISE = Duration.ofHours(72);
	private static final Duration STEADY = Duration.ofHours(100);
	private static final long THOUSANDTHS = 100_000L;
	private static final long EIGHT_POINTS = 8_000L;

	private final ChildSessionTokenPort sessions;
	private final ChildLoginPort children;
	private final ChildQuestionAttemptPort attempts;
	private final HumeurChoicePort choices;
	private final HumeurAnswerPort answers;
	private final Clock clock;

	public AskChildHumeurService(
			ChildSessionTokenPort sessions,
			ChildLoginPort children,
			ChildQuestionAttemptPort attempts,
			HumeurChoicePort choices,
			HumeurAnswerPort answers,
			Clock clock) {
		this.sessions = sessions;
		this.children = children;
		this.attempts = attempts;
		this.choices = choices;
		this.answers = answers;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public Prompt ask(String rawToken) {
		long childId = requireActiveChild(rawToken);
		List<HumeurChoice> library = choices.list();
		if (library.isEmpty() || !due(childId, clock.instant())) {
			return new Prompt(false, List.of());
		}
		return new Prompt(true, library);
	}

	@Override
	@Transactional
	public StoredAnswer answer(String rawToken, Long choiceId, Boolean closed) {
		long childId = requireActiveChild(rawToken);
		boolean closure = Boolean.TRUE.equals(closed);
		boolean picked = choiceId != null;
		if (closure == picked) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, ONE_SHAPE);
		}
		if (closure) {
			return answers.insert(childId, null, null, null, true, clock.instant());
		}
		HumeurChoice choice = choices.find(choiceId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.BAD_REQUEST, NOT_A_CHOICE));
		return answers.insert(childId, choice.id(), choice.icon(), choice.word(), false, clock.instant());
	}

	private boolean due(long childId, Instant now) {
		Optional<StoredAnswer> latest = answers.latest(childId);
		if (latest.isEmpty()) {
			return true;
		}
		Duration wait = switch (trend(childId, now)) {
			case RISE -> RISE;
			case STEADY -> STEADY;
			case DROP, NONE -> SOON;
		};
		return !now.isBefore(latest.get().answeredAt().plus(wait));
	}

	private Trend trend(long childId, Instant now) {
		Instant cut = now.minus(WEEK);
		Instant older = cut.minus(WEEK);
		int recentPassed = 0;
		int recentTotal = 0;
		int previousPassed = 0;
		int previousTotal = 0;
		List<AttemptTrace> rows = attempts.traces(childId);
		if (rows == null) {
			rows = List.of();
		}
		for (AttemptTrace trace : rows) {
			Instant at = trace.createdAt();
			if (at == null || at.isAfter(now)) {
				continue;
			}
			if (at.isAfter(cut)) {
				recentTotal++;
				if (trace.passed()) {
					recentPassed++;
				}
			} else if (at.isAfter(older)) {
				previousTotal++;
				if (trace.passed()) {
					previousPassed++;
				}
			}
		}
		if (recentTotal == 0 || previousTotal == 0) {
			return Trend.NONE;
		}
		long delta = points(recentPassed, recentTotal) - points(previousPassed, previousTotal);
		if (delta <= -EIGHT_POINTS) {
			return Trend.DROP;
		}
		if (delta >= EIGHT_POINTS) {
			return Trend.RISE;
		}
		return Trend.STEADY;
	}

	private static long points(int passed, int total) {
		return passed * THOUSANDTHS / total;
	}

	private long requireActiveChild(String rawToken) {
		long childId = sessions.read(rawToken)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.UNAUTHORIZED, INVALID_SESSION_MESSAGE))
				.childId();
		if (children.findActiveById(childId).isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.UNAUTHORIZED, INVALID_SESSION_MESSAGE);
		}
		return childId;
	}

	private enum Trend {
		DROP, RISE, STEADY, NONE
	}
}

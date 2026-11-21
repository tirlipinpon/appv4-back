package com.appv4.auth.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.PageAttempt;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredMark;
import com.appv4.auth.application.port.out.QuestionMark;
import com.appv4.auth.application.port.out.TeacherQuestionPort.QuestionSummary;
import com.appv4.auth.application.result.ChildLessonView;
import com.appv4.auth.application.result.ChildLessonView.Question;
import com.appv4.auth.application.service.question.play.PlayableQuestionForm;

/**
 * Décide quelles questions d'une matière restent dans la leçon.
 *
 * <p><b>Business :</b> une page acceptée sort une question jamais ratée jusqu'au bout.
 * Après une page ratée jusqu'au bout, il en faut deux d'affilée. Une nouvelle page ratée la remet.
 * <p><b>Technique :</b> les essais d'une même page se suivent par rang. Une page encore ouverte ne compte pas.
 * <p><b>Métier / flow :</b> lecture du board, et plus tard le badge qui voit la leçon se vider.
 */
public final class ChildLessonMembership {

	private ChildLessonMembership() {
	}

	public static final String FINISHED_LABEL = "Leçon finie.";

	public static String finishedLabel(int before, int after) {
		if (before > 0 && after == 0) {
			return FINISHED_LABEL;
		}
		return null;
	}

	public static ChildLessonView of(
			List<QuestionSummary> questions, Map<Long, StoredMark> marks, List<PageAttempt> attempts) {
		List<QuestionSummary> ordered = questions == null ? List.of() : questions;
		Map<Long, StoredMark> known = marks == null ? Map.of() : marks;
		List<PageAttempt> played = attempts == null ? List.of() : attempts;
		List<Question> kept = new ArrayList<>();
		for (QuestionSummary question : ordered) {
			if (question == null || !PlayableQuestionForm.codes().contains(question.formCode())) {
				continue;
			}
			List<PageAttempt> ofQuestion = played.stream()
					.filter(attempt -> attempt.questionId() == question.id())
					.toList();
			if (!stillIn(ofQuestion)) {
				continue;
			}
			StoredMark mark = known.get(question.id());
			kept.add(new Question(
					question.id(),
					question.prompt() == null ? "" : question.prompt(),
					mark == null || mark.code() == null ? QuestionMark.NONE : mark.code(),
					mark == null ? 0 : mark.yellowCount()));
		}
		return ChildLessonView.of(kept);
	}

	static boolean stillIn(List<PageAttempt> attempts) {
		List<PageAttempt> ordered = attempts.stream()
				.sorted(Comparator.comparing(PageAttempt::createdAt).thenComparingLong(PageAttempt::id))
				.toList();
		List<ClosedPage> closed = new ArrayList<>();
		boolean pageOpen = false;
		for (PageAttempt attempt : ordered) {
			boolean newPage = !pageOpen || attempt.attemptRank() == null || attempt.attemptRank() == 1;
			if (newPage) {
				pageOpen = true;
			}
			if (attempt.passed()) {
				closed.add(new ClosedPage(true, attempt.createdAt(), attempt.id()));
				pageOpen = false;
				continue;
			}
			boolean exhausted = attempt.attemptsRemaining() == null || attempt.attemptsRemaining() == 0;
			if (exhausted) {
				closed.add(new ClosedPage(false, attempt.createdAt(), attempt.id()));
				pageOpen = false;
			}
		}
		boolean anyAccepted = false;
		ClosedPage lastFailed = null;
		for (ClosedPage page : closed) {
			if (page.accepted) {
				anyAccepted = true;
			} else {
				lastFailed = page;
			}
		}
		if (lastFailed == null) {
			return !anyAccepted;
		}
		int acceptedAfter = 0;
		for (ClosedPage page : closed) {
			if (page.accepted && page.isAfter(lastFailed)) {
				acceptedAfter++;
			}
		}
		return acceptedAfter < 2;
	}

	private record ClosedPage(boolean accepted, Instant at, long id) {
		boolean isAfter(ClosedPage other) {
			int byTime = at.compareTo(other.at);
			return byTime > 0 || (byTime == 0 && id > other.id);
		}
	}
}

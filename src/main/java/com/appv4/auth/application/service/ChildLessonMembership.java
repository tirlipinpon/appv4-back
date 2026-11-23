package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.PageAttempt;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredMark;
import com.appv4.auth.application.port.out.QuestionMark;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.QuestionSummary;
import com.appv4.auth.application.result.ChildLessonView;
import com.appv4.auth.application.result.ChildLessonView.Question;
import com.appv4.auth.application.service.question.QuestionFormLooks;
import com.appv4.auth.application.service.question.QuestionFormLooks.Look;
import com.appv4.auth.application.service.question.QuestionFormRegistry;
import com.appv4.auth.application.service.question.play.PlayableQuestionForm;

/**
 * Décide quelles questions d'une matière restent dans la leçon.
 *
 * <p><b>Business :</b> une page acceptée sort une question jamais ratée jusqu'au bout.
 * Après une page ratée jusqu'au bout, il en faut deux d'affilée. Une nouvelle page ratée la remet.
 * <p><b>Technique :</b> seul le premier verdict qui clôt une page compte. Les essais historiques
 * sans identifiant de page gardent la lecture précédente de leurs verdicts.
 * <p><b>Métier / flow :</b> tableau de la matière et verdict de l'essai qui vide la leçon.
 * Chaque question emporte la même identité de forme que la liste de la matière.
 */
public final class ChildLessonMembership {

	public static final String FINISHED_LABEL = "Leçon finie.";
	private static final int QUESTION_PAGE = 100;
	private final TeacherQuestionPort questions;
	private final ChildQuestionAttemptPort attempts;
	private final QuestionFormRegistry forms;

	public ChildLessonMembership(
			TeacherQuestionPort questions, ChildQuestionAttemptPort attempts, QuestionFormRegistry forms) {
		this.questions = questions;
		this.attempts = attempts;
		this.forms = forms;
	}

	public ChildLessonView view(long childId, long subjectId) {
		Map<Long, List<PageAttempt>> histories = histories(childId, subjectId);
		List<QuestionSummary> remaining = playableQuestions(subjectId).stream()
				.filter(question -> progressOf(histories.getOrDefault(question.id(), List.of())).remaining())
				.toList();
		Map<Long, StoredMark> marks = attempts.marksFor(childId, remaining.stream().map(QuestionSummary::id).toList());
		Map<Long, StoredMark> known = marks == null ? Map.of() : marks;
		return ChildLessonView.of(remaining.stream().map(question -> toQuestion(question, known.get(question.id()))).toList());
	}

	/** Décide la transition sur le même historique, avant d'enregistrer le nouvel essai accepté. */
	public boolean finishesOnAcceptance(long childId, long subjectId, long questionId, UUID playId) {
		Map<Long, List<PageAttempt>> histories = histories(childId, subjectId);
		Progress submitted = progressOf(histories.getOrDefault(questionId, List.of()));
		if (!submitted.leavesOnAcceptance(playId)) {
			return false;
		}
		boolean submittedIsPlayable = false;
		for (QuestionSummary question : playableQuestions(subjectId)) {
			if (question.id() == questionId) {
				submittedIsPlayable = true;
			} else if (progressOf(histories.getOrDefault(question.id(), List.of())).remaining()) {
				return false;
			}
		}
		return submittedIsPlayable;
	}

	private List<QuestionSummary> playableQuestions(long subjectId) {
		List<QuestionSummary> playable = new ArrayList<>();
		long afterId = 0;
		while (true) {
			List<QuestionSummary> page = questions.findSummariesBySubject(subjectId, afterId, QUESTION_PAGE);
			if (page == null || page.isEmpty()) {
				return playable;
			}
			page.stream()
					.filter(question -> question != null && PlayableQuestionForm.codes().contains(question.formCode()))
					.forEach(playable::add);
			afterId = page.getLast().id();
			if (page.size() < QUESTION_PAGE) {
				return playable;
			}
		}
	}

	private Map<Long, List<PageAttempt>> histories(long childId, long subjectId) {
		List<PageAttempt> played = attempts.pageAttempts(childId, subjectId);
		return played == null ? Map.of() : played.stream().collect(Collectors.groupingBy(PageAttempt::questionId));
	}

	private Question toQuestion(QuestionSummary question, StoredMark mark) {
		Look look = QuestionFormLooks.of(question.formCode());
		return new Question(
				question.id(),
				question.formCode(),
				question.prompt() == null ? "" : question.prompt(),
				forms.labelOf(question.formCode()),
				look == null ? null : look.iconCode(),
				look == null ? null : look.color(),
				mark == null || mark.code() == null ? QuestionMark.NONE : mark.code(),
				mark == null ? 0 : mark.yellowCount());
	}

	private static Progress progressOf(List<PageAttempt> ordered) {
		Set<UUID> closedPages = new HashSet<>();
		int required = 1;
		int accepted = 0;
		for (PageAttempt attempt : ordered) {
			if (!attempt.passed() && attempt.attemptsRemaining() != null && attempt.attemptsRemaining() > 0) {
				continue;
			}
			if (attempt.playId() != null && !closedPages.add(attempt.playId())) {
				continue;
			}
			if (attempt.passed()) {
				accepted++;
			} else {
				required = 2;
				accepted = 0;
			}
		}
		return new Progress(required, accepted, closedPages);
	}

	private record Progress(int required, int accepted, Set<UUID> closedPages) {
		boolean remaining() {
			return accepted < required;
		}

		boolean leavesOnAcceptance(UUID playId) {
			return accepted == required - 1 && !closedPages.contains(playId);
		}
	}
}

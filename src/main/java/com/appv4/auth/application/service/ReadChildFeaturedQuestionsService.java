package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ReadChildFeaturedQuestionsUseCase;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredMark;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.QuestionMark;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.QuestionSummary;
import com.appv4.auth.application.result.ChildFeaturedQuestionsView;
import com.appv4.auth.application.result.ChildFeaturedQuestionsView.FeaturedQuestion;
import com.appv4.auth.application.service.question.play.PlayableQuestionForm;

/**
 * Questions proposées sur l'accueil de l'enfant (BACK-20).
 *
 * <p><b>Business :</b> au plus trois énoncés, matière liée et visible, forme déjà jouable,
 * étoile pas jaune.
 * <p><b>Technique :</b> jeton enfant, liens, résumés de questions, marques. Aucune écriture.
 * <p><b>Métier / flow :</b> port in → session enfant → questions triées → trois cartes.
 */
public class ReadChildFeaturedQuestionsService implements ReadChildFeaturedQuestionsUseCase {

	public static final String INVALID_SESSION_MESSAGE = LoginChildService.INVALID_SESSION_MESSAGE;

	private static final int MAX_FEATURED = 3;
	private static final int PAGE = 100;

	private final ChildLoginPort childLoginPort;
	private final ChildSessionTokenPort childSessionTokenPort;
	private final ChildSubjectPort childSubjectPort;
	private final TeacherQuestionPort teacherQuestionPort;
	private final ChildQuestionAttemptPort childQuestionAttemptPort;

	public ReadChildFeaturedQuestionsService(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildSubjectPort childSubjectPort,
			TeacherQuestionPort teacherQuestionPort,
			ChildQuestionAttemptPort childQuestionAttemptPort) {
		this.childLoginPort = childLoginPort;
		this.childSessionTokenPort = childSessionTokenPort;
		this.childSubjectPort = childSubjectPort;
		this.teacherQuestionPort = teacherQuestionPort;
		this.childQuestionAttemptPort = childQuestionAttemptPort;
	}

	@Override
	@Transactional(readOnly = true)
	public ChildFeaturedQuestionsView read(String rawToken) {
		long childId = requireActiveChild(rawToken);
		List<Long> chosenIds = chosenSubjectIds(childId);
		List<FeaturedQuestion> eligible = new ArrayList<>();
		if (!chosenIds.isEmpty()) {
			for (CatalogSubject subject : childSubjectPort.visibleByIds(chosenIds)) {
				addEligible(eligible, childId, subject);
			}
		}
		eligible.sort(Comparator.comparingLong(FeaturedQuestion::subjectId)
				.thenComparingLong(FeaturedQuestion::questionId));
		return new ChildFeaturedQuestionsView(eligible.stream().limit(MAX_FEATURED).toList());
	}

	private void addEligible(List<FeaturedQuestion> eligible, long childId, CatalogSubject subject) {
		List<QuestionSummary> questions = summaries(subject.id());
		Map<Long, StoredMark> marks = marksOf(childId, questions);
		for (QuestionSummary question : questions) {
			if (!PlayableQuestionForm.codes().contains(question.formCode())) {
				continue;
			}
			StoredMark mark = marks.get(question.id());
			if (mark != null && mark.code() == QuestionMark.YELLOW) {
				continue;
			}
			eligible.add(new FeaturedQuestion(
					subject.id(), subject.name(), question.id(), promptOf(question)));
		}
	}

	private List<Long> chosenSubjectIds(long childId) {
		List<Long> chosen = new ArrayList<>();
		for (StoredLink link : childSubjectPort.linksOf(childId)) {
			if (!link.excluded()) {
				chosen.add(link.subjectId());
			}
		}
		return chosen;
	}

	private List<QuestionSummary> summaries(long subjectId) {
		List<QuestionSummary> all = new ArrayList<>();
		long afterId = 0;
		while (true) {
			List<QuestionSummary> page = teacherQuestionPort.findSummariesBySubject(subjectId, afterId, PAGE);
			if (page == null || page.isEmpty()) {
				return all;
			}
			all.addAll(page);
			afterId = page.get(page.size() - 1).id();
			if (page.size() < PAGE) {
				return all;
			}
		}
	}

	private Map<Long, StoredMark> marksOf(long childId, List<QuestionSummary> questions) {
		Map<Long, StoredMark> loaded = childQuestionAttemptPort.marksFor(
				childId, questions.stream().map(QuestionSummary::id).toList());
		return loaded == null ? Map.of() : loaded;
	}

	private static String promptOf(QuestionSummary question) {
		return question.prompt() == null ? "" : question.prompt();
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

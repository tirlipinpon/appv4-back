package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ReadChildLearningSubjectsUseCase;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.QuestionSummary;
import com.appv4.auth.application.result.ChildLearningQuestionsView;
import com.appv4.auth.application.result.ChildLearningQuestionsView.QuestionCard;
import com.appv4.auth.application.result.ChildLearningSubjectsView;
import com.appv4.auth.application.result.ChildLearningSubjectsView.SubjectCard;
import com.appv4.auth.application.service.question.QuestionFormLooks;
import com.appv4.auth.application.service.question.QuestionFormLooks.Look;
import com.appv4.auth.application.service.question.QuestionFormRegistry;

/**
 * Liste learning des matières liées à l'enfant (BACK-15).
 *
 * <p><b>Business :</b> une matière retirée ou inactive n'apparaît pas.
 * <p><b>Technique :</b> jeton enfant → liens non exclus → catalogue visible + counts.
 * <p><b>Métier / flow :</b> port in → session enfant → ports out liens / questions.
 */
public class ReadChildLearningSubjectsService implements ReadChildLearningSubjectsUseCase {

	public static final String INVALID_SESSION_MESSAGE = LoginChildService.INVALID_SESSION_MESSAGE;

	public static final String SUBJECT_MISSING = "Matière introuvable.";
	private static final int MAX_QUESTIONS_PER_PAGE = 200;

	private final ChildLoginPort childLoginPort;
	private final ChildSessionTokenPort childSessionTokenPort;
	private final ChildSubjectPort childSubjectPort;
	private final TeacherQuestionPort teacherQuestionPort;
	private final QuestionFormRegistry questionFormRegistry;

	public ReadChildLearningSubjectsService(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildSubjectPort childSubjectPort,
			TeacherQuestionPort teacherQuestionPort,
			QuestionFormRegistry questionFormRegistry) {
		this.childLoginPort = childLoginPort;
		this.childSessionTokenPort = childSessionTokenPort;
		this.childSubjectPort = childSubjectPort;
		this.teacherQuestionPort = teacherQuestionPort;
		this.questionFormRegistry = questionFormRegistry;
	}

	@Override
	@Transactional(readOnly = true)
	public ChildLearningSubjectsView list(String rawToken) {
		long childId = requireActiveChild(rawToken);
		List<Long> chosenIds = chosenSubjectIds(childId);
		if (chosenIds.isEmpty()) {
			return new ChildLearningSubjectsView(List.of());
		}
		List<CatalogSubject> catalog = childSubjectPort.visibleByIds(chosenIds);
		Map<Long, Long> counts = teacherQuestionPort.countBySubjectIds(chosenIds);
		List<SubjectCard> cards = new ArrayList<>();
		for (CatalogSubject subject : catalog) {
			cards.add(new SubjectCard(
					subject.id(),
					subject.name(),
					subject.grade(),
					counts.getOrDefault(subject.id(), 0L),
					subject.iconCode()));
		}
		return new ChildLearningSubjectsView(cards);
	}

	@Override
	@Transactional(readOnly = true)
	public ChildLearningQuestionsView questions(String rawToken, long subjectId, long afterId, int limit) {
		long childId = requireActiveChild(rawToken);
		if (!chosenSubjectIds(childId).contains(subjectId)) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, SUBJECT_MISSING);
		}
		List<CatalogSubject> catalog = childSubjectPort.visibleByIds(List.of(subjectId));
		if (catalog.isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, SUBJECT_MISSING);
		}
		CatalogSubject subject = catalog.get(0);
		int pageSize = Math.clamp(limit, 1, MAX_QUESTIONS_PER_PAGE);
		List<QuestionSummary> rows = teacherQuestionPort.findSummariesBySubject(
				subjectId, Math.max(0, afterId), pageSize + 1);
		Long nextCursor = rows.size() > pageSize ? rows.get(pageSize - 1).id() : null;
		List<QuestionCard> cards = rows.stream().limit(pageSize)
				.map(q -> {
					Look look = QuestionFormLooks.of(q.formCode());
					return new QuestionCard(
							q.id(),
							q.formCode(),
							q.prompt() == null ? "" : q.prompt(),
							questionFormRegistry.labelOf(q.formCode()),
							look == null ? null : look.iconCode(),
							look == null ? null : look.color());
				})
				.toList();
		return new ChildLearningQuestionsView(subject.id(), subject.name(), cards, nextCursor);
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

	private List<Long> chosenSubjectIds(long childId) {
		return childSubjectPort.linksOf(childId).stream()
				.filter(link -> !link.excluded())
				.map(StoredLink::subjectId)
				.toList();
	}
}

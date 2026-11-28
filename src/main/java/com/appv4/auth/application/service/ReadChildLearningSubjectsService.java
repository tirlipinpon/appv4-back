package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ReadChildLearningSubjectsUseCase;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredMark;
import com.appv4.auth.application.port.out.QuestionMark;
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
 * <p><b>Business :</b> une matière retirée ou inactive n'apparaît pas. Chaque question
 * montre l'étoile déjà enregistrée pour cet enfant, ou aucune. Les cours de classe
 * manquants sont liés ici si le parent n'a jamais ouvert la page matières.
 * <p><b>Technique :</b> jeton enfant → filet liens classe → liens non exclus → catalogue + marques.
 * <p><b>Métier / flow :</b> port in → session enfant → ensure links → questions → étoiles.
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
	private final ChildQuestionAttemptPort childQuestionAttemptPort;

	public ReadChildLearningSubjectsService(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildSubjectPort childSubjectPort,
			TeacherQuestionPort teacherQuestionPort,
			QuestionFormRegistry questionFormRegistry,
			ChildQuestionAttemptPort childQuestionAttemptPort) {
		this.childLoginPort = childLoginPort;
		this.childSessionTokenPort = childSessionTokenPort;
		this.childSubjectPort = childSubjectPort;
		this.teacherQuestionPort = teacherQuestionPort;
		this.questionFormRegistry = questionFormRegistry;
		this.childQuestionAttemptPort = childQuestionAttemptPort;
	}

	@Override
	@Transactional
	public ChildLearningSubjectsView list(String rawToken) {
		long childId = requireActiveChild(rawToken);
		childSubjectPort.ensureMissingClassLinksForChild(childId);
		List<Long> chosenIds = chosenSubjectIds(childId);
		if (chosenIds.isEmpty()) {
			return new ChildLearningSubjectsView(List.of());
		}
		List<CatalogSubject> catalog = childSubjectPort.visibleByIds(chosenIds);
		Map<Long, Long> counts = teacherQuestionPort.countBySubjectIds(chosenIds);
		Map<Long, StoredMark> marks = childQuestionAttemptPort.allMarks(childId);
		if (marks == null) {
			marks = Map.of();
		}
		List<SubjectCard> cards = new ArrayList<>();
		for (CatalogSubject subject : catalog) {
			long total = counts.getOrDefault(subject.id(), 0L);
			long yellow = yellowQuestions(marks, teacherQuestionPort.questionIds(subject.id()));
			cards.add(new SubjectCard(
					subject.id(),
					subject.name(),
					subject.grade(),
					total,
					subject.iconCode(),
					yellow,
					Math.max(0, total - yellow)));
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
		List<QuestionSummary> page = rows.stream().limit(pageSize).toList();
		Map<Long, StoredMark> loaded = childQuestionAttemptPort.marksFor(
				childId, page.stream().map(QuestionSummary::id).toList());
		Map<Long, StoredMark> marks = loaded == null ? Map.of() : loaded;
		List<QuestionCard> cards = page.stream()
				.map(q -> {
					Look look = QuestionFormLooks.of(q.formCode());
					StoredMark mark = marks.get(q.id());
					return new QuestionCard(
							q.id(),
							q.formCode(),
							q.prompt() == null ? "" : q.prompt(),
							questionFormRegistry.labelOf(q.formCode()),
							look == null ? null : look.iconCode(),
							look == null ? null : look.color(),
							mark == null ? QuestionMark.NONE : mark.code(),
							mark == null ? 0 : mark.yellowCount());
				})
				.toList();
		return new ChildLearningQuestionsView(subject.id(), subject.name(), cards, nextCursor);
	}

	private static long yellowQuestions(Map<Long, StoredMark> marks, List<Long> questionIds) {
		if (questionIds == null) {
			return 0;
		}
		long yellow = 0;
		for (Long questionId : questionIds) {
			StoredMark mark = marks.get(questionId);
			if (mark != null && mark.code() == QuestionMark.YELLOW) {
				yellow++;
			}
		}
		return yellow;
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

package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ReadChildSubjectBoardUseCase;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort.BoardMeta;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort.LessonQuestion;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort.StoredLesson;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort.StoredTopic;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.result.ChildSubjectBoardView;
import com.appv4.auth.application.result.ChildSubjectBoardView.ContinueLesson;
import com.appv4.auth.application.result.ChildSubjectBoardView.TopicCard;

/**
 * Board matière de l'enfant (BACK-33).
 *
 * <p><b>Business :</b> progression et Continuer viennent des leçons et des tentatives.
 * <p><b>Technique :</b> matière liée → meta + thèmes → statut par questions réussies.
 * <p><b>Métier / flow :</b> Learn board → ce service → ports out board / sujets.
 */
public class ReadChildSubjectBoardService implements ReadChildSubjectBoardUseCase {

	public static final String INVALID_SESSION_MESSAGE = LoginChildService.INVALID_SESSION_MESSAGE;
	public static final String SUBJECT_MISSING = ReadChildLearningSubjectsService.SUBJECT_MISSING;

	private final ChildLoginPort childLoginPort;
	private final ChildSessionTokenPort childSessionTokenPort;
	private final ChildSubjectPort childSubjectPort;
	private final ChildSubjectBoardPort childSubjectBoardPort;

	public ReadChildSubjectBoardService(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildSubjectPort childSubjectPort,
			ChildSubjectBoardPort childSubjectBoardPort) {
		this.childLoginPort = childLoginPort;
		this.childSessionTokenPort = childSessionTokenPort;
		this.childSubjectPort = childSubjectPort;
		this.childSubjectBoardPort = childSubjectBoardPort;
	}

	@Override
	@Transactional(readOnly = true)
	public ChildSubjectBoardView board(String rawToken, long subjectId) {
		long childId = requireActiveChild(rawToken);
		CatalogSubject subject = requireLinkedSubject(childId, subjectId);
		BoardMeta meta = childSubjectBoardPort.findMeta(subjectId).orElse(null);
		List<StoredTopic> topics = childSubjectBoardPort.topicsOf(subjectId);
		List<Long> topicIds = topics.stream().map(StoredTopic::id).toList();
		List<StoredLesson> lessons = childSubjectBoardPort.lessonsOfTopics(topicIds);
		List<Long> lessonIds = lessons.stream().map(StoredLesson::id).toList();
		Map<Long, List<Long>> questionsByLesson = groupQuestions(
				childSubjectBoardPort.questionsOfLessons(lessonIds));
		Set<Long> passed = childSubjectBoardPort.passedQuestionIds(childId, subjectId);
		int xpEarned = childSubjectBoardPort.xpEarned(childId, subjectId);

		int completed = 0;
		int minutesLeft = 0;
		ContinueLesson continueLesson = null;
		Map<Long, int[]> topicProgress = new HashMap<>();
		Map<Long, Integer> topicRanks = new HashMap<>();
		for (int rank = 0; rank < topics.size(); rank++) {
			StoredTopic topic = topics.get(rank);
			topicRanks.put(topic.id(), rank);
			topicProgress.put(topic.id(), new int[] {0, 0});
		}
		List<StoredLesson> ordered = lessons.stream()
				.sorted(Comparator
						.comparingInt((StoredLesson lesson) ->
								topicRanks.getOrDefault(lesson.topicId(), Integer.MAX_VALUE))
						.thenComparingInt(StoredLesson::sortOrder)
						.thenComparingLong(StoredLesson::id))
				.toList();
		for (StoredLesson lesson : ordered) {
			List<Long> questionIds = questionsByLesson.getOrDefault(lesson.id(), List.of());
			LessonState state = stateOf(questionIds, passed);
			int[] counters = topicProgress.computeIfAbsent(lesson.topicId(), id -> new int[] {0, 0});
			counters[1]++;
			if (state == LessonState.COMPLETED) {
				completed++;
				counters[0]++;
			} else {
				minutesLeft += lesson.estimatedMinutes();
				if (continueLesson == null && !questionIds.isEmpty()) {
					continueLesson = toContinue(lesson, questionIds, passed, state);
				}
			}
		}

		List<TopicCard> topicCards = new ArrayList<>();
		for (StoredTopic topic : topics) {
			int[] counters = topicProgress.getOrDefault(topic.id(), new int[] {0, 0});
			topicCards.add(new TopicCard(
					topic.id(),
					topic.name(),
					topic.iconCode(),
					counters[0],
					counters[1]));
		}

		return new ChildSubjectBoardView(
				subject.id(),
				subject.name(),
				subject.iconCode(),
				subject.grade(),
				meta == null ? null : meta.description(),
				meta == null ? null : meta.tagline(),
				meta == null ? null : meta.worldLabel(),
				meta == null ? null : meta.ageLabel(),
				meta == null ? null : meta.levelLabel(),
				xpEarned,
				0,
				completed,
				lessons.size(),
				minutesLeft == 0 ? null : minutesLeft,
				continueLesson,
				topicCards);
	}

	private static ContinueLesson toContinue(
			StoredLesson lesson, List<Long> questionIds, Set<Long> passed, LessonState state) {
		int done = 0;
		Long playQuestionId = null;
		for (Long questionId : questionIds) {
			if (passed.contains(questionId)) {
				done++;
			} else if (playQuestionId == null) {
				playQuestionId = questionId;
			}
		}
		if (playQuestionId == null) {
			playQuestionId = questionIds.get(0);
		}
		int percent = questionIds.isEmpty() ? 0 : (int) Math.round(100.0 * done / questionIds.size());
		if (state == LessonState.TODO) {
			percent = 0;
		}
		return new ContinueLesson(
				lesson.id(),
				lesson.title(),
				percent,
				lesson.estimatedMinutes(),
				lesson.rewardXp(),
				playQuestionId);
	}

	private static LessonState stateOf(List<Long> questionIds, Set<Long> passed) {
		if (questionIds.isEmpty()) {
			return LessonState.TODO;
		}
		int done = 0;
		for (Long questionId : questionIds) {
			if (passed.contains(questionId)) {
				done++;
			}
		}
		if (done == questionIds.size()) {
			return LessonState.COMPLETED;
		}
		if (done > 0) {
			return LessonState.IN_PROGRESS;
		}
		return LessonState.TODO;
	}

	private static Map<Long, List<Long>> groupQuestions(List<LessonQuestion> rows) {
		Map<Long, List<Long>> byLesson = new HashMap<>();
		List<LessonQuestion> ordered = rows.stream()
				.sorted(Comparator.comparingInt(LessonQuestion::sortOrder).thenComparingLong(LessonQuestion::questionId))
				.toList();
		for (LessonQuestion row : ordered) {
			byLesson.computeIfAbsent(row.lessonId(), id -> new ArrayList<>()).add(row.questionId());
		}
		return byLesson;
	}

	private CatalogSubject requireLinkedSubject(long childId, long subjectId) {
		boolean linked = childSubjectPort.linksOf(childId).stream()
				.filter(link -> !link.excluded())
				.map(StoredLink::subjectId)
				.anyMatch(id -> id == subjectId);
		if (!linked) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, SUBJECT_MISSING);
		}
		List<CatalogSubject> catalog = childSubjectPort.visibleByIds(List.of(subjectId));
		if (catalog.isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, SUBJECT_MISSING);
		}
		return catalog.get(0);
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

	private enum LessonState {
		TODO,
		IN_PROGRESS,
		COMPLETED
	}
}

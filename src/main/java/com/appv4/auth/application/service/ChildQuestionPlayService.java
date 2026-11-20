package com.appv4.auth.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.NewAttempt;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredAttempt;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSimonChallengePort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.result.ChildAttemptResultView;
import com.appv4.auth.application.result.ChildLetterLocksView;
import com.appv4.auth.application.result.ChildPlayQuestionView;
import com.appv4.auth.application.service.question.play.ChildAnswerGrade;
import com.appv4.auth.application.service.question.play.PlayableQuestionForm;
import com.appv4.auth.application.service.question.play.QuestionPlayEngine;

/** Orchestration des parties enfant, de la correction à la progression. */
public class ChildQuestionPlayService implements ChildQuestionPlayUseCase {
	public static final String INVALID_SESSION_MESSAGE = LoginChildService.INVALID_SESSION_MESSAGE;
	public static final String SUBJECT_MISSING = ReadChildLearningSubjectsService.SUBJECT_MISSING;
	public static final String QUESTION_MISSING = "Question introuvable.";
	public static final String FORM_UNSUPPORTED = PlayableQuestionForm.UNSUPPORTED;

	private final ChildLoginPort childLoginPort;
	private final ChildSessionTokenPort childSessionTokenPort;
	private final ChildSubjectPort childSubjectPort;
	private final TeacherQuestionPort teacherQuestionPort;
	private final ChildQuestionAttemptPort childQuestionAttemptPort;
	private final ChildSimonChallengePort childSimonChallengePort;
	private final UpdateChildProgressAfterAttemptService updateChildProgressAfterAttemptService;
	private final ChildDailyMissionService childDailyMissionService;

	public ChildQuestionPlayService(
			ChildLoginPort childLoginPort, ChildSessionTokenPort childSessionTokenPort, ChildSubjectPort childSubjectPort,
			TeacherQuestionPort teacherQuestionPort, ChildQuestionAttemptPort childQuestionAttemptPort,
			ChildSimonChallengePort childSimonChallengePort,
			UpdateChildProgressAfterAttemptService updateChildProgressAfterAttemptService,
			ChildDailyMissionService childDailyMissionService) {
		this.childLoginPort = childLoginPort;
		this.childSessionTokenPort = childSessionTokenPort;
		this.childSubjectPort = childSubjectPort;
		this.teacherQuestionPort = teacherQuestionPort;
		this.childQuestionAttemptPort = childQuestionAttemptPort;
		this.childSimonChallengePort = childSimonChallengePort;
		this.updateChildProgressAfterAttemptService = updateChildProgressAfterAttemptService;
		this.childDailyMissionService = childDailyMissionService;
	}

	@Override
	@Transactional
	public ChildPlayQuestionView question(String rawToken, long subjectId, long questionId, boolean legacy) {
		long childId = requireLinkedChild(rawToken, subjectId);
		StoredQuestion question = requireQuestion(subjectId, questionId);
		if (isSimon(question)) {
			List<Integer> sequence = QuestionPlayEngine.drawSimon(question);
			UUID id = childSimonChallengePort.issue(childId, questionId, sequence, legacy);
			return QuestionPlayEngine.project(question, id, sequence, legacy);
		}
		return QuestionPlayEngine.project(question, null, List.of(), legacy);
	}

	@Override
	@Transactional
	public ChildAttemptResultView submit(String rawToken, long subjectId, long questionId, Submission submission) {
		long childId = requireLinkedChild(rawToken, subjectId);
		Optional<StoredAttempt> previous = submission.submissionId() == null ? Optional.empty()
				: childQuestionAttemptPort.lockAndFind(childId, submission.submissionId());
		if (previous.isPresent()) {
			StoredAttempt stored = previous.get();
			if (stored.questionId() != questionId || stored.subjectId() != subjectId) {
				throw new AuthDomainException(AuthErrorCode.CONFLICT, "Cette soumission appartient à une autre question.");
			}
			return result(stored);
		}
		StoredQuestion question = requireQuestion(subjectId, questionId);
		List<Integer> sequence = sequenceFor(childId, question, submission);
		ChildAnswerGrade grade = QuestionPlayEngine.grade(question, submission, sequence);
		StoredAttempt saved = childQuestionAttemptPort.insert(new NewAttempt(
				childId, subjectId, questionId, question.formCode(), grade.scorePercent(), grade.passed(), grade.answers(),
				submission.submissionId()));
		updateChildProgressAfterAttemptService.apply(childId, saved.scorePercent());
		childDailyMissionService.recordUsefulAttempt(childId, subjectId, saved.scorePercent(), saved.passed());
		return result(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public ChildLetterLocksView letterLocks(String rawToken, long subjectId, long questionId, String attempt) {
		requireLinkedChild(rawToken, subjectId);
		return QuestionPlayEngine.letterLocks(requireQuestion(subjectId, questionId), attempt);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean memoryPair(String rawToken, long subjectId, long questionId, MemoryMatch match) {
		requireLinkedChild(rawToken, subjectId);
		return QuestionPlayEngine.memoryPair(requireQuestion(subjectId, questionId), match);
	}

	private List<Integer> sequenceFor(long childId, StoredQuestion question, Submission submission) {
		UUID challengeId = submission.challengeId();
		if (!isSimon(question)) {
			if (challengeId != null) {
				throw QuestionPlayEngine.invalidAnswer();
			}
			return List.of();
		}
		if (challengeId == null && submission.declaredForm() != null) {
			throw QuestionPlayEngine.invalidAnswer();
		}
		return childSimonChallengePort.take(childId, question.id(), challengeId)
				.orElseThrow(() -> new AuthDomainException(
						challengeId == null ? AuthErrorCode.BAD_REQUEST : AuthErrorCode.CONFLICT,
						"Cette partie Simon est terminée ou a expiré. Rejoue pour obtenir une nouvelle suite."));
	}

	private static boolean isSimon(StoredQuestion question) {
		return PlayableQuestionForm.SIMON.code().equals(question.formCode());
	}

	private static ChildAttemptResultView result(StoredAttempt attempt) {
		return new ChildAttemptResultView(attempt.id(), attempt.scorePercent(), attempt.passed());
	}

	private StoredQuestion requireQuestion(long subjectId, long questionId) {
		return teacherQuestionPort.findInSubject(subjectId, questionId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, QUESTION_MISSING));
	}

	private long requireLinkedChild(String rawToken, long subjectId) {
		long childId = childSessionTokenPort.read(rawToken)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.UNAUTHORIZED, INVALID_SESSION_MESSAGE))
				.childId();
		if (childLoginPort.findActiveById(childId).isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.UNAUTHORIZED, INVALID_SESSION_MESSAGE);
		}
		boolean linked = childSubjectPort.linksOf(childId).stream()
				.anyMatch(link -> !link.excluded() && link.subjectId() == subjectId);
		if (!linked || childSubjectPort.visibleByIds(List.of(subjectId)).isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, SUBJECT_MISSING);
		}
		return childId;
	}
}

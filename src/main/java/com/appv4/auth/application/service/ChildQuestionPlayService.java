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
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.PageClaim;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredAttempt;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSimonChallengePort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.result.ChildAttemptResultView;
import com.appv4.auth.application.result.ChildLetterLocksView;
import com.appv4.auth.application.result.ChildPlayQuestionView;
import com.appv4.auth.application.service.question.QuestionGamification;
import com.appv4.auth.application.service.question.play.ChildAnswerGrade;
import com.appv4.auth.application.service.question.play.PlayableQuestionForm;
import com.appv4.auth.application.service.question.play.QuestionPlayEngine;

/** Orchestration des parties enfant, de la correction à la progression. */
public class ChildQuestionPlayService implements ChildQuestionPlayUseCase {
	public static final String INVALID_SESSION_MESSAGE = LoginChildService.INVALID_SESSION_MESSAGE;
	public static final String SUBJECT_MISSING = ReadChildLearningSubjectsService.SUBJECT_MISSING;
	public static final String QUESTION_MISSING = "Question introuvable.";
	public static final String FORM_UNSUPPORTED = PlayableQuestionForm.UNSUPPORTED;
	public static final String ATTEMPTS_EXHAUSTED = "Cette partie n'a plus d'essai.";

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
	@Transactional(readOnly = true)
	public PlayAllowance allowance(String rawToken, long subjectId, long questionId, UUID playId) {
		long childId = requireLinkedChild(rawToken, subjectId);
		requireQuestion(subjectId, questionId);
		QuestionGamification.Resolved scale = scaleOf(questionId);
		int used = playId == null ? 0 : childQuestionAttemptPort.attemptsUsed(playId, childId, subjectId, questionId);
		return new PlayAllowance(scale.maxAttempts(), Math.max(0, scale.maxAttempts() - used));
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
		QuestionGamification.Resolved scale = scaleOf(questionId);
		List<Integer> sequence = sequenceFor(childId, question, submission);
		ChildAnswerGrade grade = QuestionPlayEngine.grade(question, submission, sequence);
		PageClaim seat = seat(childId, subjectId, questionId, submission, scale);
		int points = grade.passed() ? scale.points().get(seat.rank() - 1) : 0;
		StoredAttempt saved = childQuestionAttemptPort.insert(new NewAttempt(
				childId, subjectId, questionId, question.formCode(), grade.scorePercent(), grade.passed(), grade.answers(),
				submission.submissionId(), seat.rank(), seat.attemptsRemaining(), points));
		updateChildProgressAfterAttemptService.apply(childId, saved.scorePercent());
		childDailyMissionService.recordUsefulAttempt(childId, subjectId, saved.scorePercent(), saved.passed());
		return new ChildAttemptResultView(
				saved.id(), grade.scorePercent(), grade.passed(), seat.rank(), seat.attemptsRemaining(), points,
				seat.attemptsRemaining() > 0);
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

	/**
	 * Le barème enregistré, ou le défaut (1 essai, Facile, 10 points) si la question n'en a pas.
	 * Le drapeau « gamification coupée » n'est pas lu ici.
	 */
	private QuestionGamification.Resolved scaleOf(long questionId) {
		var stored = teacherQuestionPort.gamificationOf(questionId);
		return stored == null
				? QuestionGamification.defaults()
				: QuestionGamification.resolve(
						stored.maxAttempts(), stored.difficulty(), stored.customizePoints(),
						stored.gamificationDisabled(), stored.customPoints());
	}

	private PageClaim seat(
			long childId, long subjectId, long questionId, Submission submission, QuestionGamification.Resolved scale) {
		return submission.playId() == null
				? new PageClaim(1, Math.max(0, scale.maxAttempts() - 1))
				: childQuestionAttemptPort
						.claim(submission.playId(), childId, subjectId, questionId, scale.maxAttempts())
						.orElseThrow(() -> new AuthDomainException(AuthErrorCode.CONFLICT, ATTEMPTS_EXHAUSTED));
	}

	/**
	 * Une tentative écrite avant le budget de page n'a pas ces colonnes.
	 * On la relit alors comme un premier essai déjà clos, sans points de barème.
	 */
	private static ChildAttemptResultView result(StoredAttempt attempt) {
		int rank = attempt.attemptRank() == null ? 1 : attempt.attemptRank();
		int remaining = attempt.attemptsRemaining() == null ? 0 : attempt.attemptsRemaining();
		int points = attempt.pointsAwarded() == null ? 0 : attempt.pointsAwarded();
		return new ChildAttemptResultView(
				attempt.id(), attempt.scorePercent(), attempt.passed(), rank, remaining, points, remaining > 0);
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

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
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredMark;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSimonChallengePort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.QuestionMark;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.result.ChildAttemptResultView;
import com.appv4.auth.application.result.ChildLetterLocksView;
import com.appv4.auth.application.result.ChildPlayQuestionView;
import com.appv4.auth.application.service.question.QuestionGamification;
import com.appv4.auth.application.service.question.play.ChildAnswerGrade;
import com.appv4.auth.application.service.question.play.PlayableQuestionForm;
import com.appv4.auth.application.service.question.play.QuestionPlayEngine;

/**
 * Orchestration d'une page de jeu : budget, correction, étoile, paiement.
 *
 * <p><b>Business :</b> une page ratée jusqu'au bout pose une étoile noire. Une réussite au
 * premier essai pose ou remplace par une étoile jaune. Une question déjà jaune ne rapporte
 * plus de points, sauf si ce premier essai peut encore faire avancer la mission du jour.
 * <p><b>Technique :</b> une seule transaction. La marque est verrouillée puis relue avant
 * d'ajouter une étoile, puis réécrite. Le verdict fige l'étoile de cet essai pour qu'une
 * relance renvoie la même.
 * <p><b>Métier / flow :</b> jeton enfant → réserve de la page → correction → étoile →
 * progression et mission seulement si l'essai est payé.
 */
public class ChildQuestionPlayService implements ChildQuestionPlayUseCase {
	public static final String INVALID_SESSION_MESSAGE = LoginChildService.INVALID_SESSION_MESSAGE;
	public static final String SUBJECT_MISSING = ReadChildLearningSubjectsService.SUBJECT_MISSING;
	public static final String QUESTION_MISSING = "Question introuvable.";
	public static final String FORM_UNSUPPORTED = PlayableQuestionForm.UNSUPPORTED;
	public static final String ATTEMPTS_EXHAUSTED = "Cette partie n'a plus d'essai.";
	public static final String PAGE_REQUIRED = "Cette validation doit indiquer la page ouverte.";

	private final ChildLoginPort childLoginPort;
	private final ChildSessionTokenPort childSessionTokenPort;
	private final ChildSubjectPort childSubjectPort;
	private final TeacherQuestionPort teacherQuestionPort;
	private final ChildQuestionAttemptPort childQuestionAttemptPort;
	private final ChildSimonChallengePort childSimonChallengePort;
	private final UpdateChildProgressAfterAttemptService updateChildProgressAfterAttemptService;
	private final ChildDailyMissionService childDailyMissionService;
	private final ChildBadgeProgress childBadgeProgress;

	public ChildQuestionPlayService(
			ChildLoginPort childLoginPort, ChildSessionTokenPort childSessionTokenPort, ChildSubjectPort childSubjectPort,
			TeacherQuestionPort teacherQuestionPort, ChildQuestionAttemptPort childQuestionAttemptPort,
			ChildSimonChallengePort childSimonChallengePort,
			UpdateChildProgressAfterAttemptService updateChildProgressAfterAttemptService,
			ChildDailyMissionService childDailyMissionService,
			ChildBadgeProgress childBadgeProgress) {
		this.childLoginPort = childLoginPort;
		this.childSessionTokenPort = childSessionTokenPort;
		this.childSubjectPort = childSubjectPort;
		this.teacherQuestionPort = teacherQuestionPort;
		this.childQuestionAttemptPort = childQuestionAttemptPort;
		this.childSimonChallengePort = childSimonChallengePort;
		this.updateChildProgressAfterAttemptService = updateChildProgressAfterAttemptService;
		this.childDailyMissionService = childDailyMissionService;
		this.childBadgeProgress = childBadgeProgress;
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
		if (submission.playId() == null) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, PAGE_REQUIRED);
		}
		StoredQuestion question = requireQuestion(subjectId, questionId);
		QuestionGamification.Resolved scale = scaleOf(questionId);
		List<Integer> sequence = sequenceFor(childId, question, submission);
		ChildAnswerGrade grade = QuestionPlayEngine.grade(question, submission, sequence);
		PageClaim seat = seat(childId, subjectId, questionId, submission, scale);
		StoredMark before = childQuestionAttemptPort.lockMark(childId, questionId)
				.orElse(new StoredMark(QuestionMark.NONE, 0));
		boolean alreadyYellow = before.code() == QuestionMark.YELLOW;
		QuestionMark mark = before.code();
		int yellowCount = before.yellowCount();
		if (grade.passed() && seat.rank() == 1) {
			mark = QuestionMark.YELLOW;
			yellowCount = alreadyYellow ? before.yellowCount() + 1 : 1;
		} else if (grade.passed() && alreadyYellow) {
			mark = QuestionMark.YELLOW;
			yellowCount = before.yellowCount() + 1;
		} else if (!grade.passed() && seat.attemptsRemaining() == 0 && !alreadyYellow) {
			mark = QuestionMark.BLACK;
			yellowCount = 0;
		}
		boolean missionPaysYellow = alreadyYellow && grade.passed() && seat.rank() == 1
				&& childDailyMissionService.subjectTaskHasRoom(childId, subjectId);
		boolean pay = grade.passed() && (!alreadyYellow || missionPaysYellow);
		int points = pay ? scale.points().get(seat.rank() - 1) : 0;
		childBadgeProgress.beforeAttempt(childId);
		if (mark != QuestionMark.NONE && (yellowCount != before.yellowCount() || mark != before.code())) {
			childQuestionAttemptPort.saveMark(childId, questionId, mark, yellowCount);
		}
		StoredAttempt saved = childQuestionAttemptPort.insert(new NewAttempt(
				childId, subjectId, questionId, question.formCode(), grade.scorePercent(), grade.passed(), grade.answers(),
				submission.submissionId(), seat.rank(), seat.attemptsRemaining(), points, mark, yellowCount, pay));
		childBadgeProgress.afterMarksChanged(childId);
		if (pay) {
			updateChildProgressAfterAttemptService.apply(childId, saved.scorePercent());
		}
		if (pay && seat.rank() == 1) {
			childDailyMissionService.recordUsefulAttempt(childId, subjectId, saved.scorePercent(), saved.passed());
		}
		return new ChildAttemptResultView(
				saved.id(), grade.scorePercent(), grade.passed(), seat.rank(), seat.attemptsRemaining(), points,
				seat.attemptsRemaining() > 0, mark, yellowCount);
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
		return childQuestionAttemptPort
				.claim(submission.playId(), childId, subjectId, questionId, scale.maxAttempts())
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.ATTEMPTS_EXHAUSTED, ATTEMPTS_EXHAUSTED));
	}

	/**
	 * Une tentative écrite avant le budget de page n'a pas ces colonnes.
	 * On la relit alors comme un premier essai déjà clos, sans points de barème et sans étoile.
	 */
	private static ChildAttemptResultView result(StoredAttempt attempt) {
		int rank = attempt.attemptRank() == null ? 1 : attempt.attemptRank();
		int remaining = attempt.attemptsRemaining() == null ? 0 : attempt.attemptsRemaining();
		int points = attempt.pointsAwarded() == null ? 0 : attempt.pointsAwarded();
		QuestionMark mark = attempt.markCode() == null ? QuestionMark.NONE : attempt.markCode();
		int yellowCount = attempt.yellowCount() == null ? 0 : attempt.yellowCount();
		return new ChildAttemptResultView(
				attempt.id(), attempt.scorePercent(), attempt.passed(), rank, remaining, points, remaining > 0,
				mark, yellowCount);
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

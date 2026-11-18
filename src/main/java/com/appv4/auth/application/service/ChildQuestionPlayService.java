package com.appv4.auth.application.service;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.NewAttempt;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredAttempt;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.result.ChildAttemptResultView;
import com.appv4.auth.application.result.ChildPlayQuestionView;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayChoice;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayStatement;
import com.appv4.auth.application.service.question.play.ChildAnswerGrade;
import com.appv4.auth.application.service.question.play.QcmChildAnswerGrader;
import com.appv4.auth.application.service.question.play.TrueFalseChildAnswerGrader;

/**
 * Lecture jouable et correction d'une tentative enfant (BACK-16).
 *
 * <p><b>Business :</b> seule une matière liée est jouable ; le front ne recalcule pas le score.
 * <p><b>Technique :</b> jeton enfant → question → grader qcm / vrai_faux → insert attempt.
 * <p><b>Métier / flow :</b> Learn play → ce service → ports out.
 */
public class ChildQuestionPlayService implements ChildQuestionPlayUseCase {

	public static final String INVALID_SESSION_MESSAGE = LoginChildService.INVALID_SESSION_MESSAGE;
	public static final String SUBJECT_MISSING = ReadChildLearningSubjectsService.SUBJECT_MISSING;
	public static final String QUESTION_MISSING = "Question introuvable.";
	public static final String FORM_UNSUPPORTED = "Cette forme de question n'est pas encore jouable.";

	private final ChildLoginPort childLoginPort;
	private final ChildSessionTokenPort childSessionTokenPort;
	private final ChildSubjectPort childSubjectPort;
	private final TeacherQuestionPort teacherQuestionPort;
	private final ChildQuestionAttemptPort childQuestionAttemptPort;

	public ChildQuestionPlayService(
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
	public ChildPlayQuestionView question(String rawToken, long subjectId, long questionId) {
		requireActiveChild(rawToken);
		requireLinkedSubject(rawToken, subjectId);
		StoredQuestion stored = requireQuestion(subjectId, questionId);
		return toPlayView(stored);
	}

	@Override
	@Transactional
	public ChildAttemptResultView submit(
			String rawToken, long subjectId, long questionId, AttemptAnswers answers) {
		long childId = requireActiveChild(rawToken);
		requireLinkedSubject(rawToken, subjectId);
		StoredQuestion stored = requireQuestion(subjectId, questionId);
		ChildAnswerGrade grade = grade(stored, answers);
		StoredAttempt saved = childQuestionAttemptPort.insert(new NewAttempt(
				childId,
				subjectId,
				questionId,
				stored.formCode(),
				grade.scorePercent(),
				grade.passed(),
				grade.answers()));
		return new ChildAttemptResultView(saved.id(), saved.scorePercent(), saved.passed());
	}

	private ChildAnswerGrade grade(StoredQuestion stored, AttemptAnswers answers) {
		return switch (stored.formCode()) {
			case QcmChildAnswerGrader.FORM -> QcmChildAnswerGrader.grade(stored, answers.selectedChoiceIds());
			case TrueFalseChildAnswerGrader.FORM -> TrueFalseChildAnswerGrader.grade(stored, answers.statements());
			default -> throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, FORM_UNSUPPORTED);
		};
	}

	private ChildPlayQuestionView toPlayView(StoredQuestion stored) {
		String form = stored.formCode();
		if (QcmChildAnswerGrader.FORM.equals(form)) {
			List<PlayChoice> choices = stored.choices().stream()
					.map(c -> new PlayChoice(c.id(), c.label()))
					.toList();
			return new ChildPlayQuestionView(
					stored.id(), stored.subjectId(), form, stored.prompt(), choices, List.of());
		}
		if (TrueFalseChildAnswerGrader.FORM.equals(form)) {
			List<PlayStatement> statements = stored.statements().stream()
					.map(s -> new PlayStatement(s.id(), s.label()))
					.toList();
			return new ChildPlayQuestionView(
					stored.id(), stored.subjectId(), form, stored.prompt(), List.of(), statements);
		}
		throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, FORM_UNSUPPORTED);
	}

	private StoredQuestion requireQuestion(long subjectId, long questionId) {
		return teacherQuestionPort.findInSubject(subjectId, questionId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, QUESTION_MISSING));
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

	private void requireLinkedSubject(String rawToken, long subjectId) {
		long childId = requireActiveChild(rawToken);
		boolean linked = childSubjectPort.linksOf(childId).stream()
				.filter(link -> !link.excluded())
				.map(StoredLink::subjectId)
				.anyMatch(id -> id == subjectId);
		if (!linked || childSubjectPort.visibleByIds(List.of(subjectId)).isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, SUBJECT_MISSING);
		}
	}
}

package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
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
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickZone;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.result.ChildAttemptResultView;
import com.appv4.auth.application.result.ChildPlayQuestionView;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayCard;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayChoice;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayClickPoint;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayClickStep;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayClickZone;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayColumn;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayGapWord;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayItem;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayLink;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayPair;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayPuzzlePiece;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlaySimonElement;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayStatement;
import com.appv4.auth.application.result.ChildLetterLocksView;
import com.appv4.auth.application.service.question.LetterByLetterMatch;
import com.appv4.auth.application.service.question.play.ChildAnswerGrade;
import com.appv4.auth.application.service.question.play.ClassificationChildAnswerGrader;
import com.appv4.auth.application.service.question.play.ClickChildAnswerGrader;
import com.appv4.auth.application.service.question.play.GapChildAnswerGrader;
import com.appv4.auth.application.service.question.play.LetterChildAnswerGrader;
import com.appv4.auth.application.service.question.play.LinkChildAnswerGrader;
import com.appv4.auth.application.service.question.play.MemoryChildAnswerGrader;
import com.appv4.auth.application.service.question.play.OrderChildAnswerGrader;
import com.appv4.auth.application.service.question.play.PuzzleChildAnswerGrader;
import com.appv4.auth.application.service.question.play.QcmChildAnswerGrader;
import com.appv4.auth.application.service.question.play.SimonChildAnswerGrader;
import com.appv4.auth.application.service.question.play.TrueFalseChildAnswerGrader;

/**
 * Lecture jouable et correction d'une tentative enfant (BACK-16 / BACK-17 / BACK-18 / BACK-49 / BACK-54).
 *
 * <p><b>Business :</b> seule une matière liée est jouable ; le front ne recalcule pas le score.
 * <p><b>Technique :</b> jeton enfant → question → grader → insert attempt → progress puis mission du jour.
 * <p><b>Métier / flow :</b> Learn play → ce service → ports out (+ progression et mission).
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
	private final UpdateChildProgressAfterAttemptService updateChildProgressAfterAttemptService;
	private final ChildDailyMissionService childDailyMissionService;

	public ChildQuestionPlayService(
			ChildLoginPort childLoginPort,
			ChildSessionTokenPort childSessionTokenPort,
			ChildSubjectPort childSubjectPort,
			TeacherQuestionPort teacherQuestionPort,
			ChildQuestionAttemptPort childQuestionAttemptPort,
			UpdateChildProgressAfterAttemptService updateChildProgressAfterAttemptService,
			ChildDailyMissionService childDailyMissionService) {
		this.childLoginPort = childLoginPort;
		this.childSessionTokenPort = childSessionTokenPort;
		this.childSubjectPort = childSubjectPort;
		this.teacherQuestionPort = teacherQuestionPort;
		this.childQuestionAttemptPort = childQuestionAttemptPort;
		this.updateChildProgressAfterAttemptService = updateChildProgressAfterAttemptService;
		this.childDailyMissionService = childDailyMissionService;
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
		updateChildProgressAfterAttemptService.apply(childId, saved.scorePercent());
		childDailyMissionService.recordUsefulAttempt(
				childId, subjectId, saved.scorePercent(), saved.passed());
		return new ChildAttemptResultView(saved.id(), saved.scorePercent(), saved.passed());
	}

	/**
	 * Verrous case par case pendant la saisie (sans enregistrer de tentative).
	 */
	@Override
	@Transactional(readOnly = true)
	public ChildLetterLocksView letterLocks(String rawToken, long subjectId, long questionId, String attempt) {
		requireActiveChild(rawToken);
		requireLinkedSubject(rawToken, subjectId);
		StoredQuestion stored = requireQuestion(subjectId, questionId);
		if (!LetterChildAnswerGrader.FORM.equals(stored.formCode())) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, FORM_UNSUPPORTED);
		}
		String expected = stored.expectedAnswer();
		if (expected == null || expected.isBlank()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, FORM_UNSUPPORTED);
		}
		return new ChildLetterLocksView(LetterByLetterMatch.positionLocks(expected, attempt));
	}

	private ChildAnswerGrade grade(StoredQuestion stored, AttemptAnswers answers) {
		return switch (stored.formCode()) {
			case QcmChildAnswerGrader.FORM -> QcmChildAnswerGrader.grade(stored, answers.selectedChoiceIds());
			case TrueFalseChildAnswerGrader.FORM -> TrueFalseChildAnswerGrader.grade(stored, answers.statements());
			case MemoryChildAnswerGrader.FORM -> MemoryChildAnswerGrader.grade(stored, answers.matchedPairIds());
			case OrderChildAnswerGrader.FORM -> OrderChildAnswerGrader.grade(stored, answers.orderedItemIds());
			case ClassificationChildAnswerGrader.FORM ->
					ClassificationChildAnswerGrader.grade(stored, answers.placements());
			case LinkChildAnswerGrader.FORM -> LinkChildAnswerGrader.grade(stored, answers.linkMatches());
			case GapChildAnswerGrader.FORM -> GapChildAnswerGrader.grade(stored, answers.gapFills());
			case ClickChildAnswerGrader.FORM -> ClickChildAnswerGrader.grade(stored, answers.zoneClicks());
			case LetterChildAnswerGrader.FORM -> LetterChildAnswerGrader.grade(stored, answers.attempt());
			case PuzzleChildAnswerGrader.FORM -> PuzzleChildAnswerGrader.grade(stored, answers.puzzlePlacements());
			case SimonChildAnswerGrader.FORM -> SimonChildAnswerGrader.grade(stored, answers.simonSequence());
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
					stored.id(),
					stored.subjectId(),
					form,
					stored.prompt(),
					choices,
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					null,
					List.of(),
					"",
					List.of());
		}
		if (TrueFalseChildAnswerGrader.FORM.equals(form)) {
			List<PlayStatement> statements = stored.statements().stream()
					.map(s -> new PlayStatement(s.id(), s.label()))
					.toList();
			return new ChildPlayQuestionView(
					stored.id(),
					stored.subjectId(),
					form,
					stored.prompt(),
					List.of(),
					statements,
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					null,
					List.of(),
					"",
					List.of());
		}
		if (MemoryChildAnswerGrader.FORM.equals(form)) {
			List<PlayPair> pairs = stored.pairs().stream()
					.map(p -> new PlayPair(p.id(), p.questionSide(), p.answerSide()))
					.toList();
			return new ChildPlayQuestionView(
					stored.id(),
					stored.subjectId(),
					form,
					stored.prompt(),
					List.of(),
					List.of(),
					pairs,
					List.of(),
					List.of(),
					List.of(),
					null,
					List.of(),
					"",
					List.of());
		}
		if (OrderChildAnswerGrader.FORM.equals(form)) {
			boolean timeline = Boolean.TRUE.equals(stored.timelineEnabled());
			List<PlayItem> items = new ArrayList<>(stored.items().stream()
					.map(i -> new PlayItem(i.id(), i.label(), timeline ? i.axisLabel() : null))
					.toList());
			Collections.shuffle(items);
			return new ChildPlayQuestionView(
					stored.id(),
					stored.subjectId(),
					form,
					stored.prompt(),
					List.of(),
					List.of(),
					List.of(),
					items,
					List.of(),
					List.of(),
					timeline,
					List.of(),
					"",
					List.of());
		}
		if (ClassificationChildAnswerGrader.FORM.equals(form)) {
			List<PlayColumn> columns = stored.columns().stream()
					.map(c -> new PlayColumn(c.id(), c.label()))
					.toList();
			List<PlayCard> cards = new ArrayList<>(stored.cards().stream()
					.map(c -> new PlayCard(c.id(), c.label()))
					.toList());
			Collections.shuffle(cards);
			return new ChildPlayQuestionView(
					stored.id(),
					stored.subjectId(),
					form,
					stored.prompt(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					columns,
					cards,
					null,
					List.of(),
					"",
					List.of());
		}
		if (LinkChildAnswerGrader.FORM.equals(form)) {
			List<PlayLink> links = stored.links().stream()
					.map(link -> new PlayLink(link.id(), link.leftLabel(), link.rightLabel()))
					.toList();
			return new ChildPlayQuestionView(
					stored.id(),
					stored.subjectId(),
					form,
					stored.prompt(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					null,
					links,
					"",
					List.of());
		}
		if (GapChildAnswerGrader.FORM.equals(form)) {
			List<PlayGapWord> words = new ArrayList<>();
			stored.answers().forEach(word -> words.add(new PlayGapWord(word.id(), word.label())));
			stored.decoys().forEach(word -> words.add(new PlayGapWord(word.id(), word.label())));
			Collections.shuffle(words);
			return new ChildPlayQuestionView(
					stored.id(),
					stored.subjectId(),
					form,
					stored.prompt(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					null,
					List.of(),
					stored.sentence(),
					words);
		}
		if (ClickChildAnswerGrader.FORM.equals(form)) {
			byte[] bytes = stored.imageWebp();
			String image = bytes.length == 0 ? "" : Base64.getEncoder().encodeToString(bytes);
			List<PlayClickZone> zones = stored.clickZones().stream()
					.map(zone -> new PlayClickZone(
							zone.id(),
							zone.shape(),
							zone.points().stream()
									.map(point -> new PlayClickPoint(point.x(), point.y()))
									.toList()))
					.toList();
			boolean ordered = stored.clicksOrdered() == null || stored.clicksOrdered();
			// Les noms des zones justes disent quoi chercher. Avec ordre : une consigne à la fois.
			// Sans ordre : la liste complète, sans révéler où elles sont sur l'image.
			List<PlayClickStep> steps = stored.clickZones().stream()
					.filter(StoredClickZone::correct)
					.sorted(Comparator.comparing(zone -> {
						if (ordered) {
							return zone.clickOrder() == null ? Integer.MAX_VALUE : zone.clickOrder();
						}
						return (int) Math.min(zone.id(), Integer.MAX_VALUE);
					}))
					.map(zone -> new PlayClickStep(zone.id(), zone.label()))
					.toList();
			return new ChildPlayQuestionView(
					stored.id(),
					stored.subjectId(),
					form,
					stored.prompt(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					null,
					List.of(),
					"",
					List.of(),
					image,
					ordered,
					zones,
					steps);
		}
		if (LetterChildAnswerGrader.FORM.equals(form)) {
			String expected = stored.expectedAnswer();
			if (expected == null || expected.isBlank()) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, FORM_UNSUPPORTED);
			}
			return new ChildPlayQuestionView(
					stored.id(),
					stored.subjectId(),
					form,
					stored.prompt(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					null,
					List.of(),
					"",
					List.of(),
					"",
					null,
					List.of(),
					List.of(),
					expected.length(),
					LetterByLetterMatch.mask(expected));
		}
		if (PuzzleChildAnswerGrader.FORM.equals(form)) {
			byte[] cutBytes = stored.imageWebp();
			if (cutBytes.length == 0 || stored.puzzlePieces().isEmpty() || stored.snapDistance() == null) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, FORM_UNSUPPORTED);
			}
			String cutImage = Base64.getEncoder().encodeToString(cutBytes);
			byte[] boardBytes = stored.boardImageWebp();
			String boardImage = boardBytes.length == 0 ? "" : Base64.getEncoder().encodeToString(boardBytes);
			List<PlayPuzzlePiece> pieces = new ArrayList<>(stored.puzzlePieces().stream()
					.map(piece -> new PlayPuzzlePiece(
							piece.id(),
							piece.label(),
							piece.shape(),
							piece.points().stream()
									.map(point -> new PlayClickPoint(point.x(), point.y()))
									.toList(),
							piece.slotX(),
							piece.slotY(),
							piece.slotWidth()))
					.toList());
			Collections.shuffle(pieces);
			return new ChildPlayQuestionView(
					stored.id(),
					stored.subjectId(),
					form,
					stored.prompt(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					null,
					List.of(),
					"",
					List.of(),
					cutImage,
					null,
					List.of(),
					List.of(),
					null,
					null,
					stored.boardMode(),
					boardImage,
					stored.snapDistance(),
					pieces,
					null,
					List.of(),
					List.of());
		}
		if (SimonChildAnswerGrader.FORM.equals(form)) {
			if (stored.simonElements().isEmpty() || stored.simonSequence().isEmpty()) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, FORM_UNSUPPORTED);
			}
			List<PlaySimonElement> elements = stored.simonElements().stream()
					.map(element -> new PlaySimonElement(element.label(), element.color()))
					.toList();
			return new ChildPlayQuestionView(
					stored.id(),
					stored.subjectId(),
					form,
					stored.prompt(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					null,
					List.of(),
					"",
					List.of(),
					"",
					null,
					List.of(),
					List.of(),
					null,
					null,
					null,
					"",
					null,
					List.of(),
					stored.simonElementType(),
					elements,
					stored.simonSequence());
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

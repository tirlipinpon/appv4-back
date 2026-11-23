package com.appv4.auth.application.service.question.play;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.Answer;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.MemoryMatch;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.Submission;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickZone;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.result.ChildLetterLocksView;
import com.appv4.auth.application.result.ChildPlayQuestionView;
import com.appv4.auth.application.result.ChildPlayQuestionView.Payload;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayCard;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayChoice;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayClassification;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayClickPoint;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayClickStep;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayClickZone;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayColumn;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayGapWord;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayGaps;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayImage;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayItem;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayLegacyLinks;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayLegacyMemory;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayLetters;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayLink;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayLinkOption;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayLinks;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayMemory;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayMemoryCard;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayOrder;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayPair;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayPuzzle;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayPuzzlePiece;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayQcm;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlaySimon;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlaySimonElement;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayStatement;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayText;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayTrueFalse;
import com.appv4.auth.application.service.question.LetterByLetterMatch;

/** Rendu et correction communs. La lecture des droits et la persistance restent à l'appelant. */
public final class QuestionPlayEngine {
	private QuestionPlayEngine() {
	}

	public static ChildPlayQuestionView project(
			StoredQuestion question, UUID challengeId, List<Integer> sequence, boolean legacy) {
		Payload content = switch (PlayableQuestionForm.require(question.formCode())) {
			case QCM -> new PlayQcm(question.choices().stream()
					.map(c -> new PlayChoice(c.id(), c.label(), encoded(c.imageWebp()))).toList());
			case TRUE_FALSE -> new PlayTrueFalse(question.statements().stream()
					.map(row -> new PlayStatement(row.id(), row.label(), encoded(row.imageWebp()))).toList());
			case MEMORY -> memory(question, legacy);
			case ORDER -> order(question);
			case CLASSIFICATION -> new PlayClassification(
					question.columns().stream().map(c -> new PlayColumn(c.id(), c.label())).toList(),
					shuffled(question.cards().stream()
							.map(c -> new PlayCard(c.id(), c.label(), encoded(c.imageWebp())))));
			case LINKS -> links(question, legacy);
			case GAPS -> new PlayGaps(question.sentence(), shuffled(
					Stream.concat(question.answers().stream(), question.decoys().stream())
							.map(word -> new PlayGapWord(word.id(), word.label()))));
			case CLICK_IMAGE -> image(question);
			case LETTERS -> {
				String expected = expectedText(question);
				yield new PlayLetters(expected.length(), LetterByLetterMatch.mask(expected));
			}
			case TEXT -> {
				expectedText(question);
				yield new PlayText();
			}
			case PUZZLE -> puzzle(question);
			case SIMON -> new PlaySimon(question.simonElementType(), question.simonElements().stream()
					.map(element -> new PlaySimonElement(
							element.label(), element.color(), encoded(element.imageWebp())))
					.toList(), sequence);
		};
		String prompt = question.prompt() == null ? "" : question.prompt();
		return new ChildPlayQuestionView(
				question.id(), question.subjectId(), question.formCode(), prompt, challengeId, content);
	}

	public static List<Integer> drawSimon(StoredQuestion question) {
		int count = question.simonElements().size();
		if (count == 0) {
			throw unsupported();
		}
		return SimonSequenceDraw.draw(count, ThreadLocalRandom.current());
	}

	public static ChildAnswerGrade grade(StoredQuestion question, Submission submission, List<Integer> issuedSequence) {
		if (submission.declaredForm() != null && !question.formCode().equals(submission.declaredForm())) {
			throw invalidAnswer();
		}
		Answer answer = submission.answer();
		return switch (PlayableQuestionForm.require(question.formCode())) {
			case QCM -> QcmChildAnswerGrader.grade(question, expect(answer, ChildQuestionPlayUseCase.Choices.class).ids());
			case TRUE_FALSE -> TrueFalseChildAnswerGrader.grade(
					question, expect(answer, ChildQuestionPlayUseCase.Statements.class).values());
			case MEMORY -> answer instanceof ChildQuestionPlayUseCase.MemoryMatches matches
					? MemoryChildAnswerGrader.gradeMatches(question, matches.matches())
					: MemoryChildAnswerGrader.grade(
							question, expect(answer, ChildQuestionPlayUseCase.LegacyMemory.class).pairIds());
			case ORDER -> OrderChildAnswerGrader.grade(question, expect(answer, ChildQuestionPlayUseCase.Order.class).ids());
			case CLASSIFICATION -> ClassificationChildAnswerGrader.grade(
					question, expect(answer, ChildQuestionPlayUseCase.Placements.class).values());
			case LINKS -> answer instanceof ChildQuestionPlayUseCase.LinkSelections selections
					? LinkChildAnswerGrader.gradeSelections(question, selections.matches())
					: LinkChildAnswerGrader.grade(
							question, expect(answer, ChildQuestionPlayUseCase.LegacyLinks.class).matches());
			case GAPS -> GapChildAnswerGrader.grade(question, expect(answer, ChildQuestionPlayUseCase.GapFills.class).values());
			case CLICK_IMAGE -> ClickChildAnswerGrader.grade(
					question, expect(answer, ChildQuestionPlayUseCase.ImageClicks.class).values());
			case LETTERS -> LetterChildAnswerGrader.grade(
					question, expect(answer, ChildQuestionPlayUseCase.Text.class).attempt());
			case TEXT -> FreeTextChildAnswerGrader.grade(
					question, expect(answer, ChildQuestionPlayUseCase.Text.class).attempt());
			case PUZZLE -> PuzzleChildAnswerGrader.grade(
					question, expect(answer, ChildQuestionPlayUseCase.Puzzle.class).values());
			case SIMON -> SimonChildAnswerGrader.grade(
					question, issuedSequence, expect(answer, ChildQuestionPlayUseCase.Simon.class).sequence());
		};
	}

	public static ChildLetterLocksView letterLocks(StoredQuestion question, String attempt) {
		if (!PlayableQuestionForm.LETTERS.code().equals(question.formCode())) {
			throw unsupported();
		}
		return new ChildLetterLocksView(LetterByLetterMatch.positionLocks(expectedText(question), attempt));
	}

	public static boolean memoryPair(StoredQuestion question, MemoryMatch match) {
		if (!PlayableQuestionForm.MEMORY.code().equals(question.formCode())) {
			throw unsupported();
		}
		return MemoryChildAnswerGrader.matches(question, match);
	}

	private static Payload memory(StoredQuestion question, boolean legacy) {
		if (legacy) {
			return new PlayLegacyMemory(question.pairs().stream()
					.map(pair -> new PlayPair(pair.id(), pair.questionSide(), pair.answerSide())).toList());
		}
		return new PlayMemory(shuffled(question.pairs().stream().flatMap(pair -> Stream.of(
				new PlayMemoryCard(
						token(question, pair.id(), true),
						pair.questionSide(),
						encoded(pair.questionImageWebp())),
				new PlayMemoryCard(
						token(question, pair.id(), false),
						pair.answerSide(),
						encoded(pair.answerImageWebp()))))));
	}

	private static PlayOrder order(StoredQuestion question) {
		boolean timeline = Boolean.TRUE.equals(question.timelineEnabled());
		return new PlayOrder(shuffled(question.items().stream()
				.map(item -> new PlayItem(
						item.id(),
						item.label(),
						timeline ? item.axisLabel() : null,
						encoded(item.imageWebp())))),
				timeline);
	}

	private static Payload links(StoredQuestion question, boolean legacy) {
		if (legacy) {
			return new PlayLegacyLinks(question.links().stream()
					.map(link -> new PlayLink(link.id(), link.leftLabel(), link.rightLabel())).toList());
		}
		return new PlayLinks(
				shuffled(question.links().stream()
						.map(link -> new PlayLinkOption(
								token(question, link.id(), true),
								link.leftLabel(),
								encoded(link.leftImageWebp())))),
				shuffled(question.links().stream()
						.map(link -> new PlayLinkOption(
								token(question, link.id(), false),
								link.rightLabel(),
								encoded(link.rightImageWebp())))));
	}

	private static PlayImage image(StoredQuestion question) {
		List<PlayClickZone> zones = question.clickZones().stream()
				.map(zone -> new PlayClickZone(zone.id(), zone.shape(), zone.points().stream()
						.map(point -> new PlayClickPoint(point.x(), point.y())).toList()))
				.toList();
		boolean ordered = !Boolean.FALSE.equals(question.clicksOrdered());
		Comparator<StoredClickZone> stepOrder = ordered
				? Comparator.comparing(StoredClickZone::clickOrder, Comparator.nullsLast(Comparator.naturalOrder()))
				: Comparator.comparingLong(StoredClickZone::id);
		List<PlayClickStep> steps = question.clickZones().stream().filter(StoredClickZone::correct)
				.sorted(stepOrder).map(zone -> new PlayClickStep(zone.id(), zone.label())).toList();
		return new PlayImage(encoded(question.imageWebp()), ordered, zones, steps);
	}

	private static PlayPuzzle puzzle(StoredQuestion question) {
		String image = encoded(question.imageWebp());
		if (image.isEmpty() || question.puzzlePieces().isEmpty() || question.snapDistance() == null) {
			throw unsupported();
		}
		List<PlayPuzzlePiece> pieces = shuffled(question.puzzlePieces().stream().map(piece -> new PlayPuzzlePiece(
				piece.id(), piece.label(), piece.shape(), piece.points().stream()
						.map(point -> new PlayClickPoint(point.x(), point.y())).toList(),
				piece.slotX(), piece.slotY(), piece.slotWidth())));
		return new PlayPuzzle(image, question.boardMode(), encoded(question.boardImageWebp()),
				question.snapDistance(), pieces);
	}

	private static String expectedText(StoredQuestion question) {
		String expected = question.expectedAnswer();
		if (expected == null || expected.isBlank()) {
			throw unsupported();
		}
		return expected;
	}

	private static <T extends Answer> T expect(Answer answer, Class<T> kind) {
		if (!kind.isInstance(answer)) {
			throw invalidAnswer();
		}
		return kind.cast(answer);
	}

	private static UUID token(StoredQuestion question, long id, boolean first) {
		return AssociationTokens.forSide(question.formCode(), id, first);
	}

	private static <T> List<T> shuffled(Stream<T> values) {
		List<T> rows = new ArrayList<>(values.toList());
		Collections.shuffle(rows);
		return List.copyOf(rows);
	}

	private static String encoded(byte[] image) {
		if (image == null || image.length == 0) {
			return "";
		}
		return Base64.getEncoder().encodeToString(image);
	}

	private static AuthDomainException unsupported() {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST, PlayableQuestionForm.UNSUPPORTED);
	}

	public static AuthDomainException invalidAnswer() {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST, "Réponse invalide pour cette question.");
	}
}

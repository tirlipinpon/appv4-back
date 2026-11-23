package com.appv4.auth.application.service.question.play;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.PuzzlePiecePlacementAnswer;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickPoint;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPuzzle;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPuzzlePiece;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

class PuzzleChildAnswerGraderTest {

	private static StoredQuestion trousQuestion() {
		return new StoredQuestion(
				1L,
				2L,
				"puzzle",
				"Replace.",
				new StoredPuzzle(
						new byte[]{1, 2},
						"trous",
						null,
						0.08,
						List.of(
								piece(10L, "A", 0.1, 0.2, 0.3, 0.4, null, null),
								piece(11L, "B", 0.5, 0.5, 0.7, 0.7, null, null))));
	}

	private static StoredQuestion blancQuestion() {
		return new StoredQuestion(
				1L,
				2L,
				"puzzle",
				"Replace.",
				new StoredPuzzle(
						new byte[]{1, 2},
						"blanc",
						null,
						0.08,
						List.of(piece(10L, "A", 0.1, 0.2, 0.3, 0.4, 0.55, 0.6))));
	}

	/** Planche d'organes + poupée : la découpe (en haut à gauche) n'est pas la place sur le fond. */
	private static StoredQuestion fondQuestion(Double slotX, Double slotY) {
		return new StoredQuestion(
				1L,
				2L,
				"puzzle",
				"Replace.",
				new StoredPuzzle(
						new byte[]{1, 2},
						"fond",
						new byte[]{3, 4},
						0.08,
						List.of(piece(10L, "cerveau", 0.08, 0.01, 0.64, 0.16, slotX, slotY))));
	}

	private static StoredPuzzlePiece piece(
			long id,
			String label,
			double x1,
			double y1,
			double x2,
			double y2,
			Double slotX,
			Double slotY) {
		return new StoredPuzzlePiece(
				id,
				label,
				"rect",
				List.of(new ClickPoint(x1, y1), new ClickPoint(x2, y2)),
				slotX,
				slotY,
				null);
	}

	@Test
	void allPiecesOnHomePass() {
		ChildAnswerGrade grade = PuzzleChildAnswerGrader.grade(
				trousQuestion(),
				List.of(
						new PuzzlePiecePlacementAnswer(10L, 0.1, 0.2),
						new PuzzlePiecePlacementAnswer(11L, 0.5, 0.5)));
		assertThat(grade.scorePercent()).isEqualTo(100);
		assertThat(grade.passed()).isTrue();
	}

	@Test
	void onePieceFarScoresHalf() {
		ChildAnswerGrade grade = PuzzleChildAnswerGrader.grade(
				trousQuestion(),
				List.of(
						new PuzzlePiecePlacementAnswer(10L, 0.1, 0.2),
						new PuzzlePiecePlacementAnswer(11L, 0.9, 0.9)));
		assertThat(grade.scorePercent()).isEqualTo(50);
		assertThat(grade.passed()).isFalse();
	}

	@Test
	void blancUsesSlotNotCutFrame() {
		ChildAnswerGrade grade = PuzzleChildAnswerGrader.grade(
				blancQuestion(),
				List.of(new PuzzlePiecePlacementAnswer(10L, 0.55, 0.6)));
		assertThat(grade.scorePercent()).isEqualTo(100);
		assertThat(grade.passed()).isTrue();
	}

	@Test
	void blancWrongIfPlacedOnCutInsteadOfSlot() {
		ChildAnswerGrade grade = PuzzleChildAnswerGrader.grade(
				blancQuestion(),
				List.of(new PuzzlePiecePlacementAnswer(10L, 0.1, 0.2)));
		assertThat(grade.scorePercent()).isEqualTo(0);
		assertThat(grade.passed()).isFalse();
	}

	@Test
	void fondUsesTeacherPlaceNotCutFrame() {
		ChildAnswerGrade grade = PuzzleChildAnswerGrader.grade(
				fondQuestion(0.33, 0.05),
				List.of(new PuzzlePiecePlacementAnswer(10L, 0.33, 0.05)));
		assertThat(grade.scorePercent()).isEqualTo(100);
		assertThat(grade.passed()).isTrue();
	}

	@Test
	void fondWrongIfPlacedOnCutInsteadOfPlace() {
		ChildAnswerGrade grade = PuzzleChildAnswerGrader.grade(
				fondQuestion(0.33, 0.05),
				List.of(new PuzzlePiecePlacementAnswer(10L, 0.08, 0.01)));
		assertThat(grade.scorePercent()).isEqualTo(0);
		assertThat(grade.passed()).isFalse();
	}

	@Test
	void fondWithoutPlaceKeepsTheCutFrame() {
		ChildAnswerGrade grade = PuzzleChildAnswerGrader.grade(
				fondQuestion(null, null),
				List.of(new PuzzlePiecePlacementAnswer(10L, 0.08, 0.01)));
		assertThat(grade.scorePercent()).isEqualTo(100);
		assertThat(grade.passed()).isTrue();
	}

	@Test
	void missingPieceRejected() {
		assertThatThrownBy(() -> PuzzleChildAnswerGrader.grade(
						trousQuestion(), List.of(new PuzzlePiecePlacementAnswer(10L, 0.1, 0.2))))
				.isInstanceOf(AuthDomainException.class);
	}

	@Test
	void duplicatePieceRejected() {
		assertThatThrownBy(() -> PuzzleChildAnswerGrader.grade(
						trousQuestion(),
						List.of(
								new PuzzlePiecePlacementAnswer(10L, 0.1, 0.2),
								new PuzzlePiecePlacementAnswer(10L, 0.1, 0.2))))
				.isInstanceOf(AuthDomainException.class);
	}
}

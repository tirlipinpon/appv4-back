package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.BOARD_IMAGE_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.BOARD_MODE_BLANC;
import static com.appv4.auth.application.service.TeacherQuestionService.BOARD_MODE_DEFAULT;
import static com.appv4.auth.application.service.TeacherQuestionService.BOARD_MODE_FOND;
import static com.appv4.auth.application.service.TeacherQuestionService.BOARD_MODE_INVALID;
import static com.appv4.auth.application.service.TeacherQuestionService.BOARD_MODE_TROUS;
import static com.appv4.auth.application.service.TeacherQuestionService.PIECE_NAME_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.PIECE_NAME_TOO_LONG;
import static com.appv4.auth.application.service.TeacherQuestionService.PIECE_POINTS;
import static com.appv4.auth.application.service.TeacherQuestionService.PIECE_SHAPE;
import static com.appv4.auth.application.service.TeacherQuestionService.PIECES_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.PUZZLE;
import static com.appv4.auth.application.service.TeacherQuestionService.PUZZLE_LABEL;
import static com.appv4.auth.application.service.TeacherQuestionService.PUZZLE_ORDER;
import static com.appv4.auth.application.service.TeacherQuestionService.SNAP_DEFAULT;
import static com.appv4.auth.application.service.TeacherQuestionService.SNAP_MAX;
import static com.appv4.auth.application.service.TeacherQuestionService.SNAP_RANGE;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

import java.util.ArrayList;
import java.util.List;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.PuzzleInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.PuzzlePieceInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PuzzleDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PuzzlePieceDraft;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;

/**
 * Règles de saisie d'un puzzle (SCRUM-183).
 *
 * <p><b>Business :</b> le professeur découpe une image en pièces nommées, puis choisit
 * le plateau : fond dédié, cadre blanc, ou image de découpe trouée. Pas de vert ni de rouge.
 * L'énoncé peut rester vide. Au moins deux pièces. Le magnétisme dit à quelle distance
 * une pièce se colle sur sa place.
 * <p><b>Technique :</b> code {@code puzzle}, ordre 10. Images et points via {@link DrawnImageRules}.
 * <p><b>Métier / flow :</b> le registre appelle {@link #prepare} avant l'enregistrement.
 */
public final class PuzzleQuestionForm implements QuestionFormHandler {

	private static final int NAME_MAX = 200;

	@Override
	public FormLine form() {
		return new FormLine(PUZZLE, PUZZLE_LABEL, PUZZLE_ORDER);
	}

	@Override
	public Prepared prepare(String prompt, ContentInput input) {
		if (!(input instanceof PuzzleInput puzzle)) {
			throw new IllegalArgumentException("Entrée incompatible avec le puzzle");
		}
		String mode = requireMode(puzzle.boardMode());
		byte[] cut = DrawnImageRules.requireWebp(puzzle.imageWebp());
		byte[] board = BOARD_MODE_FOND.equals(mode)
				? DrawnImageRules.requireWebp(puzzle.boardImageWebp(), BOARD_IMAGE_REQUIRED)
				: null;
		return new Prepared(
				QuestionValidation.optionalPrompt(prompt),
				new PuzzleDraft(cut, mode, board, requireSnap(puzzle.snapDistance()), requirePieces(puzzle.pieces())));
	}

	/** Absent : trous. Autre valeur : refus. */
	private static String requireMode(String raw) {
		if (raw == null || raw.isBlank()) {
			return BOARD_MODE_DEFAULT;
		}
		String mode = raw.trim();
		if (!BOARD_MODE_FOND.equals(mode)
				&& !BOARD_MODE_BLANC.equals(mode)
				&& !BOARD_MODE_TROUS.equals(mode)) {
			throw bad(BOARD_MODE_INVALID);
		}
		return mode;
	}

	/** Absent : 8 %. Hors de 0 à 25 % : refus. */
	private static double requireSnap(Double raw) {
		if (raw == null) {
			return SNAP_DEFAULT;
		}
		if (raw.isNaN() || raw < 0 || raw > SNAP_MAX) {
			throw bad(SNAP_RANGE);
		}
		return raw;
	}

	private static List<PuzzlePieceDraft> requirePieces(List<PuzzlePieceInput> pieces) {
		if (pieces == null || pieces.size() < 2) {
			throw bad(PIECES_REQUIRED);
		}
		List<PuzzlePieceDraft> drafts = new ArrayList<>();
		for (PuzzlePieceInput piece : pieces) {
			if (piece == null) {
				throw bad(PIECE_NAME_REQUIRED);
			}
			drafts.add(new PuzzlePieceDraft(
					requireName(piece.label()),
					DrawnImageRules.shape(piece.shape(), PIECE_SHAPE),
					DrawnImageRules.points(piece.shape(), piece.points(), PIECE_SHAPE, PIECE_POINTS)));
		}
		return List.copyOf(drafts);
	}

	private static String requireName(String label) {
		String name = label == null ? "" : label.trim();
		if (name.isEmpty()) {
			throw bad(PIECE_NAME_REQUIRED);
		}
		if (name.length() > NAME_MAX) {
			throw bad(PIECE_NAME_TOO_LONG);
		}
		return name;
	}
}

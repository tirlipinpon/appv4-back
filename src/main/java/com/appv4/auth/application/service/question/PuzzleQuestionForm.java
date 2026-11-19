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
import static com.appv4.auth.application.service.TeacherQuestionService.PIECE_SLOT_OFF_BOARD;
import static com.appv4.auth.application.service.TeacherQuestionService.PIECE_SLOT_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.PIECE_SLOT_REQUIRED_FOND;
import static com.appv4.auth.application.service.TeacherQuestionService.PIECE_SLOT_WIDTH_RANGE;
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
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickPoint;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PuzzleDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PuzzlePieceDraft;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;

/**
 * Règles de saisie d'un puzzle (SCRUM-183).
 *
 * <p><b>Business :</b> le professeur découpe une image en pièces nommées, puis choisit
 * le plateau : fond dédié, cadre blanc, ou image de découpe trouée. En mode blanc, il pose
 * chaque pièce sur le cadre pour fixer l'emplacement que l'enfant devra retrouver — sans
 * silhouette visible en jeu. En mode fond, il pose de même chaque pièce sur le fond et règle
 * sa taille : le fond n'a pas à reprendre le cadrage de la découpe (ex. une planche d'organes
 * et une poupée avec ses cavités). Pas de vert ni de rouge. L'énoncé peut rester vide. Au moins
 * deux pièces. Le magnétisme dit à quelle distance une pièce se colle sur sa place.
 * <p><b>Technique :</b> code {@code puzzle}, ordre 10. Images et points via {@link DrawnImageRules}.
 * Place = coin haut-gauche (proportion du plateau) ; taille = largeur sur le fond (2 à 100 %).
 * <p><b>Métier / flow :</b> le registre appelle {@link #prepare} avant l'enregistrement.
 */
public final class PuzzleQuestionForm implements QuestionFormHandler {

	private static final int NAME_MAX = 200;
	private static final double MIN_SPAN = 0.02;
	/** Plus petite taille d'une pièce sur le fond : même plancher qu'une forme tracée. */
	private static final double SLOT_WIDTH_MIN = MIN_SPAN;

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
				new PuzzleDraft(
						cut,
						mode,
						board,
						requireSnap(puzzle.snapDistance()),
						requirePieces(mode, puzzle.pieces())));
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

	private static List<PuzzlePieceDraft> requirePieces(String mode, List<PuzzlePieceInput> pieces) {
		if (pieces == null || pieces.size() < 2) {
			throw bad(PIECES_REQUIRED);
		}
		boolean blanc = BOARD_MODE_BLANC.equals(mode);
		boolean fond = BOARD_MODE_FOND.equals(mode);
		List<PuzzlePieceDraft> drafts = new ArrayList<>();
		for (PuzzlePieceInput piece : pieces) {
			if (piece == null) {
				throw bad(PIECE_NAME_REQUIRED);
			}
			List<ClickPoint> points = DrawnImageRules.points(
					piece.shape(), piece.points(), PIECE_SHAPE, PIECE_POINTS);
			Double slotX = null;
			Double slotY = null;
			Double slotWidth = null;
			if (blanc || fond) {
				if (piece.slotX() == null || piece.slotY() == null
						|| piece.slotX().isNaN() || piece.slotY().isNaN()) {
					throw bad(fond ? PIECE_SLOT_REQUIRED_FOND : PIECE_SLOT_REQUIRED);
				}
				// Cadre blanc : même ratio que la découpe, la taille reste celle de la découpe.
				slotWidth = fond ? optionalSlotWidth(piece.slotWidth()) : null;
				requireSlotOnBoard(piece.slotX(), piece.slotY(), slotWidth, points);
				slotX = piece.slotX();
				slotY = piece.slotY();
			}
			drafts.add(new PuzzlePieceDraft(
					requireName(piece.label()),
					DrawnImageRules.shape(piece.shape(), PIECE_SHAPE),
					points,
					slotX,
					slotY,
					slotWidth));
		}
		return List.copyOf(drafts);
	}

	/** Absente : taille de la découpe. Sinon entre 2 et 100 % de la largeur du plateau. */
	private static Double optionalSlotWidth(Double raw) {
		if (raw == null) {
			return null;
		}
		if (raw.isNaN() || raw < SLOT_WIDTH_MIN || raw > 1) {
			throw bad(PIECE_SLOT_WIDTH_RANGE);
		}
		return raw;
	}

	/**
	 * Coin haut-gauche de l'emplacement sur le plateau.
	 * Peut sortir un peu de [0, 1] (débord) tant que la pièce reste partiellement visible.
	 * Avec une taille sur le fond, la hauteur suit la même proportion que la largeur.
	 */
	private static void requireSlotOnBoard(
			double rawX, double rawY, Double slotWidth, List<ClickPoint> points) {
		double minX = points.get(0).x();
		double maxX = minX;
		double minY = points.get(0).y();
		double maxY = minY;
		for (ClickPoint point : points) {
			minX = Math.min(minX, point.x());
			maxX = Math.max(maxX, point.x());
			minY = Math.min(minY, point.y());
			maxY = Math.max(maxY, point.y());
		}
		double width = Math.max(MIN_SPAN, maxX - minX);
		double height = Math.max(MIN_SPAN, maxY - minY);
		if (slotWidth != null) {
			height = height * slotWidth / width;
			width = slotWidth;
		}
		boolean onBoard = rawX < 1 && rawY < 1 && rawX + width > 0 && rawY + height > 0;
		if (!onBoard) {
			throw bad(PIECE_SLOT_OFF_BOARD);
		}
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

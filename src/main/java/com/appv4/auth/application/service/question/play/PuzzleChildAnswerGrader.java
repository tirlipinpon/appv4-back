package com.appv4.auth.application.service.question.play;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.PuzzlePiecePlacementAnswer;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickPoint;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPuzzlePiece;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

/**
 * Note un puzzle (pièces replacées).
 *
 * <p><b>Business :</b> chaque pièce doit être assez près de son emplacement : la place posée
 * par le professeur (cadre blanc, fond dédié), sinon sa place sur la découpe (trous, anciens
 * fonds sans place). Score = pièces justes / total ; réussi à 100 %.
 * <p><b>Technique :</b> même rayon que le magnétisme front (fraction de largeur +
 * plancher 4 px sur une base 1000). Trace {@code target_kind = piece} avec
 * {@code answer_choice_id = pieceId} pour rester dans la contrainte des tentatives.
 * <p><b>Métier / flow :</b> POST attempts → {@link com.appv4.auth.application.service.ChildQuestionPlayService}.
 */
public final class PuzzleChildAnswerGrader {

	public static final String FORM = "puzzle";
	public static final String BAD_ANSWER = "Réponse invalide pour cette question.";
	public static final String BOARD_MODE_BLANC = "blanc";
	public static final String BOARD_MODE_FOND = "fond";

	/** Même plancher pixel que le front ({@code MAGNET_FLOOR_PX}). */
	private static final double MAGNET_FLOOR_PX = 4;
	/** Base neutre pour convertir la fraction de largeur en pixels côté serveur. */
	private static final double GRADE_WIDTH_PX = 1000;

	private PuzzleChildAnswerGrader() {
	}

	public static ChildAnswerGrade grade(StoredQuestion question, List<PuzzlePiecePlacementAnswer> placements) {
		List<StoredPuzzlePiece> pieces = question.puzzlePieces();
		if (pieces.isEmpty() || question.imageWebp().length == 0 || question.snapDistance() == null) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		String boardMode = question.boardMode() == null ? "" : question.boardMode();
		double snap = question.snapDistance();
		List<PuzzlePiecePlacementAnswer> submitted = placements == null ? List.of() : placements;
		if (submitted.size() != pieces.size()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Map<Long, StoredPuzzlePiece> byId = new HashMap<>();
		for (StoredPuzzlePiece piece : pieces) {
			byId.put(piece.id(), piece);
		}
		Set<Long> seen = new HashSet<>();
		int ok = 0;
		List<AnswerLine> lines = new ArrayList<>();
		for (PuzzlePiecePlacementAnswer placement : submitted) {
			if (placement == null
					|| !byId.containsKey(placement.pieceId())
					|| !seen.add(placement.pieceId())
					|| !finite01(placement.x())
					|| !finite01(placement.y())) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
			}
			StoredPuzzlePiece piece = byId.get(placement.pieceId());
			double[] home = homeTopLeft(piece, boardMode);
			boolean correct = withinMagnet(placement.x(), placement.y(), home[0], home[1], snap);
			if (correct) {
				ok++;
			}
			lines.add(new AnswerLine("piece", piece.id(), null, piece.id(), correct));
		}
		int score = (int) Math.round(100.0 * ok / pieces.size());
		return new ChildAnswerGrade(score, ok == pieces.size(), lines);
	}

	/**
	 * Emplacement cible du coin haut-gauche : place posée par le professeur (blanc, fond),
	 * sinon cadre de la découpe.
	 */
	static double[] homeTopLeft(StoredPuzzlePiece piece, String boardMode) {
		boolean placedByTeacher = BOARD_MODE_BLANC.equals(boardMode) || BOARD_MODE_FOND.equals(boardMode);
		if (placedByTeacher && piece.slotX() != null && piece.slotY() != null) {
			return new double[]{piece.slotX(), piece.slotY()};
		}
		return frameTopLeft(piece.shape(), piece.points());
	}

	static double[] frameTopLeft(String shape, List<ClickPoint> points) {
		if (points == null || points.isEmpty()) {
			return new double[]{0, 0};
		}
		if ("rect".equals(shape) && points.size() >= 2) {
			double x1 = points.get(0).x();
			double y1 = points.get(0).y();
			double x2 = points.get(1).x();
			double y2 = points.get(1).y();
			return new double[]{Math.min(x1, x2), Math.min(y1, y2)};
		}
		if ("ellipse".equals(shape) && points.size() >= 2) {
			double cx = points.get(0).x();
			double cy = points.get(0).y();
			double rx = Math.abs(points.get(1).x() - cx);
			double ry = Math.abs(points.get(1).y() - cy);
			return new double[]{cx - rx, cy - ry};
		}
		double minX = points.get(0).x();
		double minY = points.get(0).y();
		for (ClickPoint point : points) {
			minX = Math.min(minX, point.x());
			minY = Math.min(minY, point.y());
		}
		return new double[]{minX, minY};
	}

	static boolean withinMagnet(double x, double y, double homeX, double homeY, double snap) {
		double reach = Math.max(clampSnap(snap) * GRADE_WIDTH_PX, MAGNET_FLOOR_PX);
		double dx = (x - homeX) * GRADE_WIDTH_PX;
		double dy = (y - homeY) * GRADE_WIDTH_PX;
		return Math.hypot(dx, dy) <= reach;
	}

	private static double clampSnap(double snap) {
		if (!Double.isFinite(snap)) {
			return 0.08;
		}
		return Math.min(0.25, Math.max(0, snap));
	}

	private static boolean finite01(double value) {
		return Double.isFinite(value) && value >= -0.5 && value <= 1.5;
	}
}

package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.IMAGE_NOT_WEBP;
import static com.appv4.auth.application.service.TeacherQuestionService.IMAGE_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.IMAGE_TOO_LARGE;
import static com.appv4.auth.application.service.TeacherQuestionService.SHAPE_ELLIPSE;
import static com.appv4.auth.application.service.TeacherQuestionService.SHAPE_POLYGON;
import static com.appv4.auth.application.service.TeacherQuestionService.SHAPE_RECT;
import static com.appv4.auth.application.service.TeacherQuestionService.ZONE_POINTS;
import static com.appv4.auth.application.service.TeacherQuestionService.ZONE_SHAPE;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ClickPointInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickPoint;

/**
 * Image allégée et formes collées au dessin (SCRUM-181, réutilisé par SCRUM-183).
 *
 * <p><b>Business :</b> le fichier d'origine n'est pas gardé. Les points restent
 * entre 0 et 1 pour suivre l'image si l'écran change de taille.
 * <p><b>Technique :</b> WebP d'au plus 5 Mo. Rectangle et ellipse ont deux points,
 * le tracé libre en a entre 3 et 24.
 * <p><b>Métier / flow :</b> appelé par la question clic et par le puzzle avant l'enregistrement.
 */
final class DrawnImageRules {

	static final int IMAGE_MAX_BYTES = 5 * 1024 * 1024;
	private static final int POINTS_MAX = 24;
	private static final double MIN_SPAN = 0.02d;

	private DrawnImageRules() {
	}

	static byte[] requireWebp(String raw) {
		if (raw == null || raw.isBlank()) {
			throw bad(IMAGE_REQUIRED);
		}
		String payload = raw.trim();
		int comma = payload.indexOf(',');
		if (payload.startsWith("data:") && comma > 0) {
			payload = payload.substring(comma + 1);
		}
		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(payload);
		} catch (IllegalArgumentException ex) {
			throw bad(IMAGE_NOT_WEBP);
		}
		if (bytes.length == 0 || !isWebp(bytes)) {
			throw bad(IMAGE_NOT_WEBP);
		}
		if (bytes.length > IMAGE_MAX_BYTES) {
			throw bad(IMAGE_TOO_LARGE);
		}
		return bytes;
	}

	static String shape(String raw) {
		return shape(raw, ZONE_SHAPE);
	}

	static String shape(String raw, String invalidMessage) {
		String shape = raw == null ? "" : raw.trim();
		if (!SHAPE_POLYGON.equals(shape) && !SHAPE_RECT.equals(shape) && !SHAPE_ELLIPSE.equals(shape)) {
			throw bad(invalidMessage);
		}
		return shape;
	}

	static List<ClickPoint> points(String shape, List<ClickPointInput> raw) {
		return points(shape, raw, ZONE_SHAPE, ZONE_POINTS);
	}

	static List<ClickPoint> points(
			String shape, List<ClickPointInput> raw, String invalidShape, String invalidPoints) {
		String checked = shape(shape, invalidShape);
		List<ClickPoint> points = readPoints(raw, invalidPoints);
		if (SHAPE_POLYGON.equals(checked)) {
			if (points.size() < 3 || points.size() > POINTS_MAX) {
				throw bad(invalidPoints);
			}
			return points;
		}
		if (points.size() != 2) {
			throw bad(invalidPoints);
		}
		if (SHAPE_RECT.equals(checked)) {
			return rectangle(points.get(0), points.get(1), invalidPoints);
		}
		return ellipse(points.get(0), points.get(1), invalidPoints);
	}

	private static boolean isWebp(byte[] bytes) {
		return bytes.length >= 12
				&& bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
				&& bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
	}

	private static List<ClickPoint> readPoints(List<ClickPointInput> raw, String invalidPoints) {
		if (raw == null || raw.isEmpty()) {
			throw bad(invalidPoints);
		}
		List<ClickPoint> points = new ArrayList<>();
		for (ClickPointInput point : raw) {
			if (point == null || point.x() == null || point.y() == null) {
				throw bad(invalidPoints);
			}
			double x = point.x();
			double y = point.y();
			if (Double.isNaN(x) || Double.isNaN(y) || x < 0d || x > 1d || y < 0d || y > 1d) {
				throw bad(invalidPoints);
			}
			points.add(new ClickPoint(x, y));
		}
		return points;
	}

	private static List<ClickPoint> rectangle(ClickPoint first, ClickPoint second, String invalidPoints) {
		double x1 = Math.min(first.x(), second.x());
		double y1 = Math.min(first.y(), second.y());
		double x2 = Math.max(first.x(), second.x());
		double y2 = Math.max(first.y(), second.y());
		if (x2 - x1 < MIN_SPAN || y2 - y1 < MIN_SPAN) {
			throw bad(invalidPoints);
		}
		return List.of(new ClickPoint(x1, y1), new ClickPoint(x2, y2));
	}

	private static List<ClickPoint> ellipse(ClickPoint center, ClickPoint edge, String invalidPoints) {
		double rx = edge.x() - center.x();
		double ry = edge.y() - center.y();
		if (rx < MIN_SPAN || ry < MIN_SPAN) {
			throw bad(invalidPoints);
		}
		if (center.x() - rx < 0d || center.y() - ry < 0d || center.x() + rx > 1d || center.y() + ry > 1d) {
			throw bad(invalidPoints);
		}
		return List.of(center, edge);
	}
}

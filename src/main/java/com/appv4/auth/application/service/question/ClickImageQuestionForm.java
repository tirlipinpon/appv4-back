package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.IMAGE_CLICK;
import static com.appv4.auth.application.service.TeacherQuestionService.IMAGE_CLICK_LABEL;
import static com.appv4.auth.application.service.TeacherQuestionService.IMAGE_CLICK_ORDER;
import static com.appv4.auth.application.service.TeacherQuestionService.IMAGE_NOT_WEBP;
import static com.appv4.auth.application.service.TeacherQuestionService.IMAGE_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.IMAGE_TOO_LARGE;
import static com.appv4.auth.application.service.TeacherQuestionService.SHAPE_ELLIPSE;
import static com.appv4.auth.application.service.TeacherQuestionService.SHAPE_POLYGON;
import static com.appv4.auth.application.service.TeacherQuestionService.SHAPE_RECT;
import static com.appv4.auth.application.service.TeacherQuestionService.ZONE_NAME_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.ZONE_NAME_TOO_LONG;
import static com.appv4.auth.application.service.TeacherQuestionService.ZONE_POINTS;
import static com.appv4.auth.application.service.TeacherQuestionService.ZONE_RULE_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.ZONE_SHAPE;
import static com.appv4.auth.application.service.TeacherQuestionService.ZONE_SIDE_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.ZONES_REQUIRED;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ClickImageInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ClickPointInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ClickZoneInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickImageDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickPoint;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickZoneDraft;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;

/**
 * Règles de saisie d'une image à cliquer (SCRUM-181).
 *
 * <p><b>Business :</b> le professeur pose une image, nomme chaque zone, et dit si elle est
 * juste. Il dit aussi s'il faut cliquer toutes les zones justes. L'énoncé peut rester vide.
 * <p><b>Technique :</b> code {@code image_interactive}, ordre 8. L'image acceptée est un WebP
 * d'au plus 5 Mo. Les points sont entre 0 et 1.
 * <p><b>Métier / flow :</b> le registre appelle {@link #prepare} avant l'enregistrement.
 * L'élève ne joue pas encore.
 */
public final class ClickImageQuestionForm implements QuestionFormHandler {

	static final int IMAGE_MAX_BYTES = 5 * 1024 * 1024;
	private static final int NAME_MAX = 200;
	private static final int POINTS_MAX = 24;
	private static final double MIN_SPAN = 0.02d;

	@Override
	public FormLine form() {
		return new FormLine(IMAGE_CLICK, IMAGE_CLICK_LABEL, IMAGE_CLICK_ORDER);
	}

	@Override
	public Prepared prepare(String prompt, ContentInput input) {
		if (!(input instanceof ClickImageInput image)) {
			throw new IllegalArgumentException("Entrée incompatible avec l'image à cliquer");
		}
		if (image.requireAllCorrect() == null) {
			throw bad(ZONE_RULE_REQUIRED);
		}
		return new Prepared(
				QuestionValidation.optionalPrompt(prompt),
				new ClickImageDraft(
						requireWebp(image.imageWebp()), image.requireAllCorrect(), requireZones(image.zones())));
	}

	private static byte[] requireWebp(String raw) {
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

	private static boolean isWebp(byte[] bytes) {
		return bytes.length >= 12
				&& bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
				&& bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
	}

	private static List<ClickZoneDraft> requireZones(List<ClickZoneInput> zones) {
		if (zones == null || zones.isEmpty()) {
			throw bad(ZONES_REQUIRED);
		}
		List<ClickZoneDraft> drafts = new ArrayList<>();
		for (ClickZoneInput zone : zones) {
			if (zone == null) {
				throw bad(ZONE_NAME_REQUIRED);
			}
			drafts.add(new ClickZoneDraft(requireName(zone.label()), requireSide(zone.correct()), shape(zone), points(zone)));
		}
		return List.copyOf(drafts);
	}

	private static String requireName(String label) {
		String name = label == null ? "" : label.trim();
		if (name.isEmpty()) {
			throw bad(ZONE_NAME_REQUIRED);
		}
		if (name.length() > NAME_MAX) {
			throw bad(ZONE_NAME_TOO_LONG);
		}
		return name;
	}

	private static boolean requireSide(Boolean correct) {
		if (correct == null) {
			throw bad(ZONE_SIDE_REQUIRED);
		}
		return correct;
	}

	private static String shape(ClickZoneInput zone) {
		String shape = zone.shape() == null ? "" : zone.shape().trim();
		if (!SHAPE_POLYGON.equals(shape) && !SHAPE_RECT.equals(shape) && !SHAPE_ELLIPSE.equals(shape)) {
			throw bad(ZONE_SHAPE);
		}
		return shape;
	}

	private static List<ClickPoint> points(ClickZoneInput zone) {
		List<ClickPoint> points = readPoints(zone.points());
		String shape = zone.shape() == null ? "" : zone.shape().trim();
		if (SHAPE_POLYGON.equals(shape)) {
			if (points.size() < 3 || points.size() > POINTS_MAX) {
				throw bad(ZONE_POINTS);
			}
			return points;
		}
		if (points.size() != 2) {
			throw bad(ZONE_POINTS);
		}
		if (SHAPE_RECT.equals(shape)) {
			return rectangle(points.get(0), points.get(1));
		}
		return ellipse(points.get(0), points.get(1));
	}

	private static List<ClickPoint> readPoints(List<ClickPointInput> raw) {
		if (raw == null || raw.isEmpty()) {
			throw bad(ZONE_POINTS);
		}
		List<ClickPoint> points = new ArrayList<>();
		for (ClickPointInput point : raw) {
			if (point == null || point.x() == null || point.y() == null) {
				throw bad(ZONE_POINTS);
			}
			double x = point.x();
			double y = point.y();
			if (Double.isNaN(x) || Double.isNaN(y) || x < 0d || x > 1d || y < 0d || y > 1d) {
				throw bad(ZONE_POINTS);
			}
			points.add(new ClickPoint(x, y));
		}
		return points;
	}

	private static List<ClickPoint> rectangle(ClickPoint first, ClickPoint second) {
		double x1 = Math.min(first.x(), second.x());
		double y1 = Math.min(first.y(), second.y());
		double x2 = Math.max(first.x(), second.x());
		double y2 = Math.max(first.y(), second.y());
		if (x2 - x1 < MIN_SPAN || y2 - y1 < MIN_SPAN) {
			throw bad(ZONE_POINTS);
		}
		return List.of(new ClickPoint(x1, y1), new ClickPoint(x2, y2));
	}

	private static List<ClickPoint> ellipse(ClickPoint center, ClickPoint edge) {
		double rx = edge.x() - center.x();
		double ry = edge.y() - center.y();
		if (rx < MIN_SPAN || ry < MIN_SPAN) {
			throw bad(ZONE_POINTS);
		}
		if (center.x() - rx < 0d || center.y() - ry < 0d || center.x() + rx > 1d || center.y() + ry > 1d) {
			throw bad(ZONE_POINTS);
		}
		return List.of(center, edge);
	}
}

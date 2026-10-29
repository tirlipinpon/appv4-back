package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickPoint;

/**
 * Points d'une zone, écrits en proportion de l'image (SCRUM-181).
 *
 * <p><b>Business :</b> un point reste au même endroit du dessin quand l'écran change de taille.
 * <p><b>Technique :</b> chaîne {@code x,y;x,y}, six décimales, entre 0 et 1.
 * <p><b>Métier / flow :</b> la forme a déjà validé les points. Ici on les range et on les relit.
 */
final class ClickZonePoints {

	private ClickZonePoints() {
	}

	static String encode(List<ClickPoint> points) {
		StringBuilder text = new StringBuilder();
		for (ClickPoint point : points) {
			if (!text.isEmpty()) {
				text.append(';');
			}
			text.append(String.format(Locale.ROOT, "%.6f,%.6f", point.x(), point.y()));
		}
		return text.toString();
	}

	static List<ClickPoint> decode(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new IllegalStateException("Points de zone absents");
		}
		List<ClickPoint> points = new ArrayList<>();
		for (String pair : raw.split(";")) {
			String[] xy = pair.split(",");
			if (xy.length != 2) {
				throw new IllegalStateException("Points de zone illisibles");
			}
			try {
				points.add(new ClickPoint(Double.parseDouble(xy[0]), Double.parseDouble(xy[1])));
			} catch (NumberFormatException ex) {
				throw new IllegalStateException("Points de zone illisibles");
			}
		}
		return List.copyOf(points);
	}
}

package com.appv4.auth.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Fenêtre où l'app enfant est ouverte.
 *
 * <p><b>Business :</b> l'enfant entre à l'heure de début et sort à l'heure de fin.
 * Au départ, 6 h et 22 h. L'admin peut les changer, pour tous les enfants.
 * <p><b>Technique :</b> l'heure de fin est incluse dans la fermeture, l'heure de
 * début est exclue de la fermeture. Le début est toujours avant la fin, le même jour.
 * <p><b>Métier / flow :</b> lu par le login enfant et par le séjour. La phrase
 * renvoyée cite l'heure de début.
 */
public record AppHours(LocalTime opensAt, LocalTime closesAt) {

	public static final LocalTime DEFAULT_OPENS = LocalTime.of(6, 0);
	public static final LocalTime DEFAULT_CLOSES = LocalTime.of(22, 0);

	public AppHours {
		if (opensAt == null || closesAt == null || !opensAt.isBefore(closesAt)) {
			throw new IllegalArgumentException("L'heure de début est avant l'heure de fin.");
		}
	}

	public static AppHours defaults() {
		return new AppHours(DEFAULT_OPENS, DEFAULT_CLOSES);
	}

	/** Vrai à partir de l'heure de fin, et avant l'heure de début. */
	public boolean closed(LocalTime time) {
		return !time.isBefore(closesAt) || time.isBefore(opensAt);
	}

	public boolean closedAt(Instant instant, ZoneId zone) {
		return closed(instant.atZone(zone).toLocalTime());
	}

	/**
	 * L'heure de fin qui a ouvert la fermeture en cours.
	 * Avant l'heure de début, c'est celle de la veille.
	 */
	public Instant closedSince(Instant instant, ZoneId zone) {
		ZonedDateTime zoned = instant.atZone(zone);
		LocalDate day = zoned.toLocalDate();
		if (zoned.toLocalTime().isBefore(opensAt)) {
			day = day.minusDays(1);
		}
		return day.atTime(closesAt).atZone(zone).toInstant();
	}

	/** Phrase unique pendant la fermeture. À 6 h pile : « jusqu'à 6 h. » */
	public String closedMessage() {
		int hour = opensAt.getHour();
		int minute = opensAt.getMinute();
		if (minute == 0 && opensAt.getSecond() == 0) {
			return "L'app est fermée jusqu'à " + hour + " h.";
		}
		return "L'app est fermée jusqu'à " + hour + " h " + String.format("%02d", minute) + ".";
	}
}

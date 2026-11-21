package com.appv4.auth.application.port.out;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Étoile fermée d'une question pour un enfant.
 *
 * <p><b>Business :</b> rien, une étoile noire, ou une étoile jaune. Pas d'autre marque.
 * <p><b>Technique :</b> le JSON reste {@code none}, {@code black} ou {@code yellow}.
 * <p><b>Métier / flow :</b> le jeu l'écrit, la liste et le verdict la relisent telle quelle.
 */
public enum QuestionMark {
	NONE("none"),
	BLACK("black"),
	YELLOW("yellow");

	private static final Map<String, QuestionMark> BY_WIRE = Arrays.stream(values())
			.collect(Collectors.toUnmodifiableMap(QuestionMark::wire, Function.identity()));

	private final String wire;

	QuestionMark(String wire) {
		this.wire = wire;
	}

	@JsonValue
	public String wire() {
		return wire;
	}

	@JsonCreator
	public static QuestionMark fromWire(String raw) {
		QuestionMark mark = raw == null ? null : BY_WIRE.get(raw);
		if (mark == null) {
			throw new IllegalArgumentException("Marque inconnue.");
		}
		return mark;
	}
}

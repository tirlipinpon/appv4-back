package com.appv4.auth.application.result;

/**
 * Verdict de Valider pour une saisie lettre par lettre (SCRUM-182).
 *
 * <p><b>Business :</b> l'élève voit si le mot composé est le bon.
 * <p><b>Technique :</b> {@code accepted} décide, {@code message} est la phrase à afficher.
 * <p><b>Métier / flow :</b> résultat de {@code POST .../questions/{id}/attempt}.
 */
public record LetterAttemptView(boolean accepted, String message) {
}

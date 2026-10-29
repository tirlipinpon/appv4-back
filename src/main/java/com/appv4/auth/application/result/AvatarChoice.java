package com.appv4.auth.application.result;

/**
 * Un dessin proposé pour la fiche du compte.
 *
 * <p><b>Business :</b> le parent ou le professeur choisit un avatar dans cette liste, ou aucun.
 * <p><b>Technique :</b> le code est stocké ; le libellé est celui que l'écran affiche.
 * <p><b>Métier / flow :</b> la même liste sert la fiche parent, la fiche professeur et le bandeau.
 */
public record AvatarChoice(String code, String label) {
}

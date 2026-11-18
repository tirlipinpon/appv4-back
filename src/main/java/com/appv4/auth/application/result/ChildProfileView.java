package com.appv4.auth.application.result;

/**
 * Profil enfant pour l'accueil learning (BACK-14).
 *
 * <p><b>Business :</b> prénom et image déjà choisis par le parent, lus tels quels.
 */
public record ChildProfileView(long childId, String firstName, String avatarCode, boolean hasPhoto) {
}

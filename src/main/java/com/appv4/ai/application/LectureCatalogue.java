package com.appv4.ai.application;

/**
 * Ce qu'un catalogue a répondu lors de la lecture.
 *
 * <p><b>Business :</b> s'il est absent, les autres restent. On ne remplace pas par une liste locale.
 */
public record LectureCatalogue(String code, String nom, boolean disponible, String message) {
}

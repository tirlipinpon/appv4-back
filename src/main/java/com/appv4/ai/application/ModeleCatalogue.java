package com.appv4.ai.application;

import java.util.List;

/**
 * Un modèle vu dans un ou plusieurs catalogues (catalogue IA).
 *
 * <p><b>Business :</b> l'identifiant est celui du catalogue, le nom est celui qu'on lit.
 * <p><b>Technique :</b> {@code catalogues} dit quelles sources l'ont envoyé.
 */
public record ModeleCatalogue(String id, String nom, List<String> catalogues) {
}

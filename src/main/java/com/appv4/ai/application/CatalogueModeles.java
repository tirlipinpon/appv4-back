package com.appv4.ai.application;

import java.util.List;

/**
 * L'union des catalogues lus pour l'admin.
 *
 * <p><b>Business :</b> le plus grand choix est la réunion des sources qui ont répondu.
 */
public record CatalogueModeles(List<LectureCatalogue> lectures, List<FournisseurCatalogue> fournisseurs) {
}

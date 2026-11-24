package com.appv4.ai.application;

import java.util.List;

/**
 * Un fournisseur et les modèles que les catalogues ont renvoyés.
 *
 * <p><b>Business :</b> le nom vient du catalogue. On ne l'invente pas.
 */
public record FournisseurCatalogue(String code, String nom, List<ModeleCatalogue> modeles) {
}

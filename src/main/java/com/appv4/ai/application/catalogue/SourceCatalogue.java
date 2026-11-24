package com.appv4.ai.application.catalogue;

import java.util.List;

/**
 * Une source de catalogue (OpenRouter, models.dev ou Artificial Analysis).
 *
 * <p><b>Business :</b> si elle ne répond pas, elle le dit. Elle n'invente pas de modèle.
 */
public interface SourceCatalogue {

	String code();

	String nom();

	List<EntreeCatalogue> lire();
}

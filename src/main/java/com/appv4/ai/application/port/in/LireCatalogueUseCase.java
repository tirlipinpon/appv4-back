package com.appv4.ai.application.port.in;

import com.appv4.ai.application.CatalogueModeles;

/**
 * Lire le catalogue réuni pour l'admin.
 *
 * <p><b>Business :</b> seul l'admin voit les fournisseurs et les modèles du moment.
 * <p><b>Technique :</b> une source en échec n'efface pas les autres.
 * <p><b>Métier / flow :</b> page IA → ce port → OpenRouter, models.dev, Artificial Analysis.
 */
public interface LireCatalogueUseCase {

	CatalogueModeles lire(long callerId);
}

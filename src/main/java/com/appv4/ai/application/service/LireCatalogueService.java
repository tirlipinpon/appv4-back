package com.appv4.ai.application.service;

import java.util.ArrayList;
import java.util.List;

import com.appv4.ai.application.BesoinIaException;
import com.appv4.ai.application.CatalogueModeles;
import com.appv4.ai.application.LectureCatalogue;
import com.appv4.ai.application.catalogue.EntreeCatalogue;
import com.appv4.ai.application.catalogue.ReunionCatalogues;
import com.appv4.ai.application.catalogue.SourceCatalogue;
import com.appv4.ai.application.port.in.LireCatalogueUseCase;
import com.appv4.auth.application.port.out.ActiveRolePort;

/**
 * Réunit les catalogues pour l'admin.
 *
 * <p><b>Business :</b> le plus grand choix est tout ce que les sources ont envoyé, une fois chacun.
 * <p><b>Technique :</b> une source en échec laisse les autres. Pas de liste de secours.
 * <p><b>Métier / flow :</b> page IA → ce service → les trois lectures HTTP.
 */
public class LireCatalogueService implements LireCatalogueUseCase {

	static final String INTERDIT = "Réservé à l'admin.";

	private final ActiveRolePort roles;
	private final List<SourceCatalogue> sources;

	public LireCatalogueService(ActiveRolePort roles, List<SourceCatalogue> sources) {
		this.roles = roles;
		this.sources = List.copyOf(sources);
	}

	@Override
	public CatalogueModeles lire(long callerId) {
		if (!roles.hasAdminRole(callerId)) {
			throw new BesoinIaException(INTERDIT, BesoinIaException.Code.FORBIDDEN);
		}
		List<LectureCatalogue> lectures = new ArrayList<>();
		List<EntreeCatalogue> entrees = new ArrayList<>();
		for (SourceCatalogue source : sources) {
			try {
				List<EntreeCatalogue> lues = source.lire();
				entrees.addAll(lues);
				lectures.add(new LectureCatalogue(source.code(), source.nom(), true, lues.size() + " modèles"));
			} catch (RuntimeException ex) {
				String message = ex.getMessage() == null || ex.getMessage().isBlank()
						? "Catalogue indisponible."
						: ex.getMessage();
				lectures.add(new LectureCatalogue(source.code(), source.nom(), false, message));
			}
		}
		return new CatalogueModeles(List.copyOf(lectures), ReunionCatalogues.reunir(entrees));
	}
}

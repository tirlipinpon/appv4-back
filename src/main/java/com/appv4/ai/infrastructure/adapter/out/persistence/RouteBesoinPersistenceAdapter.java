package com.appv4.ai.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import com.appv4.ai.application.port.out.RouteBesoin;
import com.appv4.ai.application.port.out.RouteBesoinPort;

/**
 * Routes des besoins en base (BACK-30).
 *
 * <p><b>Business :</b> remplacer une route garde le même code.
 * <p><b>Technique :</b> {@code code} est la clé. Pas de clé de fournisseur dans cette table.
 * <p><b>Métier / flow :</b> adaptateur out du routeur.
 */
public class RouteBesoinPersistenceAdapter implements RouteBesoinPort {

	private final BackAiTaskRouteJpaRepository routes;

	public RouteBesoinPersistenceAdapter(BackAiTaskRouteJpaRepository routes) {
		this.routes = routes;
	}

	@Override
	public Optional<RouteBesoin> trouver(String code) {
		return routes.findById(code).map(this::toRoute);
	}

	@Override
	public List<RouteBesoin> toutes() {
		return routes.findAll().stream().map(this::toRoute).toList();
	}

	@Override
	public void enregistrer(RouteBesoin route) {
		BackAiTaskRouteEntity entity = routes.findById(route.code())
				.orElseGet(() -> new BackAiTaskRouteEntity(
						route.code(), route.nom(), route.fournisseur(), route.modele(), route.actif()));
		if (routes.existsById(route.code())) {
			entity.replace(route.nom(), route.fournisseur(), route.modele(), route.actif());
		}
		routes.save(entity);
	}

	private RouteBesoin toRoute(BackAiTaskRouteEntity entity) {
		return new RouteBesoin(
				entity.getCode(),
				entity.getLabel(),
				entity.getProvider(),
				entity.getModel(),
				entity.isActive());
	}
}

package com.appv4.ai.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.ai.application.CatalogueModeles;
import com.appv4.ai.application.FournisseurCatalogue;
import com.appv4.ai.application.LectureCatalogue;
import com.appv4.ai.application.ModeleCatalogue;
import com.appv4.ai.application.port.in.LireCatalogueUseCase;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.infrastructure.adapter.in.web.CurrentAuth;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Catalogue réuni pour choisir un modèle (page IA).
 *
 * <p><b>Business :</b> l'admin voit le plus grand choix renvoyé par les trois catalogues.
 * <p><b>Technique :</b> {@code GET /api/int/admin/ai/catalog}. Aucun modèle n'est ajouté ici.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → lectures OpenRouter, models.dev, Artificial Analysis.
 */
@RestController
@RequestMapping("/api/int/admin/ai/catalog")
@Tag(name = "Admin — IA")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class CatalogueController {

	private final LireCatalogueUseCase catalogue;

	public CatalogueController(LireCatalogueUseCase catalogue) {
		this.catalogue = catalogue;
	}

	@GetMapping
	@Operation(operationId = "readAiCatalog", summary = "Lire les fournisseurs et modèles des catalogues")
	public ResponseEntity<CatalogueResponse> read(@AuthenticationPrincipal AuthenticatedUser principal) {
		CatalogueModeles lu = catalogue.lire(CurrentAuth.require(principal).userId());
		return ResponseEntity.ok(toResponse(lu));
	}

	private static CatalogueResponse toResponse(CatalogueModeles lu) {
		return new CatalogueResponse(
				lu.lectures().stream().map(CatalogueController::lecture).toList(),
				lu.fournisseurs().stream().map(CatalogueController::fournisseur).toList());
	}

	private static LectureResponse lecture(LectureCatalogue lecture) {
		return new LectureResponse(lecture.code(), lecture.nom(), lecture.disponible(), lecture.message());
	}

	private static FournisseurResponse fournisseur(FournisseurCatalogue fournisseur) {
		return new FournisseurResponse(
				fournisseur.code(),
				fournisseur.nom(),
				fournisseur.modeles().stream().map(CatalogueController::modele).toList());
	}

	private static ModeleResponse modele(ModeleCatalogue modele) {
		return new ModeleResponse(modele.id(), modele.nom(), modele.catalogues());
	}

	public record LectureResponse(String code, String name, boolean available, String message) {
	}

	public record ModeleResponse(String id, String name, List<String> catalogs) {
	}

	public record FournisseurResponse(String code, String name, List<ModeleResponse> models) {
	}

	public record CatalogueResponse(List<LectureResponse> catalogs, List<FournisseurResponse> providers) {
	}
}

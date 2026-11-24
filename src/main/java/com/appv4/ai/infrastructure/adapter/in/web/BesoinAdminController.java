package com.appv4.ai.infrastructure.adapter.in.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.ai.application.BesoinIaException;
import com.appv4.ai.application.ChangementBesoin;
import com.appv4.ai.application.LectureBesoins;
import com.appv4.ai.application.LectureUsage;
import com.appv4.ai.application.LigneUsage;
import com.appv4.ai.application.port.in.ReglerBesoinUseCase;
import com.appv4.ai.application.port.out.RouteBesoin;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.infrastructure.adapter.in.web.CurrentAuth;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Réglage des besoins pour l'admin (BACK-32).
 *
 * <p><b>Business :</b> lire les besoins, en ajouter, changer la paire, lire la consommation et l'historique.
 * <p><b>Technique :</b> {@code /api/int/admin/ai/routes}, {@code /usage}, {@code /route-changes}.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link ReglerBesoinUseCase}. Pas d'appel au fournisseur.
 */
@RestController
@RequestMapping("/api/int/admin/ai")
@Tag(name = "Admin — IA")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class BesoinAdminController {

	private final ReglerBesoinUseCase besoins;

	public BesoinAdminController(ReglerBesoinUseCase besoins) {
		this.besoins = besoins;
	}

	@GetMapping("/routes")
	@Operation(operationId = "listAiRoutes", summary = "Lire les besoins et les fournisseurs branchables")
	public ResponseEntity<RoutesResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
		LectureBesoins lu = besoins.lire(CurrentAuth.require(principal).userId());
		return ResponseEntity.ok(new RoutesResponse(
				lu.besoins().stream().map(BesoinAdminController::route).toList(),
				lu.fournisseurs()));
	}

	@PostMapping("/routes")
	@Operation(operationId = "createAiRoute", summary = "Ajouter un besoin")
	public ResponseEntity<RouteResponse> create(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestBody CreateRouteRequest request) {
		RouteBesoin cree = besoins.creer(
				CurrentAuth.require(principal).userId(),
				request.code(),
				request.label(),
				request.provider(),
				request.model());
		return ResponseEntity.ok(route(cree));
	}

	@PutMapping("/routes/{taskCode}")
	@Operation(operationId = "changeAiRoute", summary = "Changer le fournisseur et le modèle d'un besoin")
	public ResponseEntity<RouteResponse> change(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable String taskCode,
			@RequestBody ChangeRouteRequest request) {
		RouteBesoin suivi = besoins.changer(
				CurrentAuth.require(principal).userId(),
				taskCode,
				request.provider(),
				request.model());
		return ResponseEntity.ok(route(suivi));
	}

	@PutMapping("/routes/{taskCode}/active")
	@Operation(operationId = "setAiRouteActive", summary = "Activer ou désactiver un besoin")
	public ResponseEntity<RouteResponse> active(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable String taskCode,
			@RequestBody ActiveRouteRequest request) {
		if (request.active() == null) {
			throw new BesoinIaException("Le statut est absent.");
		}
		RouteBesoin suivi = besoins.activer(CurrentAuth.require(principal).userId(), taskCode, request.active());
		return ResponseEntity.ok(route(suivi));
	}

	@GetMapping("/usage")
	@Operation(operationId = "readAiUsage", summary = "Lire la consommation d'une période UTC")
	public ResponseEntity<UsageResponse> usage(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		LectureUsage lu = besoins.usage(CurrentAuth.require(principal).userId(), from, to);
		return ResponseEntity.ok(new UsageResponse(
				lu.du(),
				lu.au(),
				lu.lignes().stream().map(BesoinAdminController::ligne).toList()));
	}

	@GetMapping("/route-changes")
	@Operation(operationId = "listAiRouteChanges", summary = "Lire l'historique des changements de modèle")
	public ResponseEntity<ChangesResponse> changes(@AuthenticationPrincipal AuthenticatedUser principal) {
		List<ChangeResponse> lignes = besoins.historique(CurrentAuth.require(principal).userId()).stream()
				.map(BesoinAdminController::changement)
				.toList();
		return ResponseEntity.ok(new ChangesResponse(lignes));
	}

	private static RouteResponse route(RouteBesoin route) {
		return new RouteResponse(route.code(), route.nom(), route.fournisseur(), route.modele(), route.actif());
	}

	private static UsageLineResponse ligne(LigneUsage ligne) {
		return new UsageLineResponse(
				ligne.jour(),
				ligne.code(),
				ligne.nom(),
				ligne.fournisseur(),
				ligne.modele(),
				ligne.appels(),
				ligne.tokens());
	}

	private static ChangeResponse changement(ChangementBesoin changement) {
		return new ChangeResponse(
				changement.code(),
				changement.nom(),
				changement.nomAdmin(),
				changement.dateLabel(),
				changement.timeLabel(),
				changement.ancienFournisseur(),
				changement.ancienModele(),
				changement.nouveauFournisseur(),
				changement.nouveauModele());
	}

	public record CreateRouteRequest(String code, String label, String provider, String model) {
	}

	public record ChangeRouteRequest(String provider, String model) {
	}

	public record ActiveRouteRequest(Boolean active) {
	}

	public record RouteResponse(String code, String label, String provider, String model, boolean active) {
	}

	public record RoutesResponse(List<RouteResponse> routes, List<String> providers) {
	}

	public record UsageLineResponse(
			LocalDate date,
			String code,
			String label,
			String provider,
			String model,
			int calls,
			int tokens) {
	}

	public record UsageResponse(LocalDate from, LocalDate to, List<UsageLineResponse> lines) {
	}

	public record ChangeResponse(
			String code,
			String label,
			String adminName,
			String dateLabel,
			String timeLabel,
			String oldProvider,
			String oldModel,
			String newProvider,
			String newModel) {
	}

	public record ChangesResponse(List<ChangeResponse> changes) {
	}
}

package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.SuggestChildInterestsUseCase;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Suggestions de centres d'intérêt (BACK-73).
 *
 * <p><b>Business :</b> le parent choisit une orthographe déjà écrite, sans voir à qui elle appartient.
 * <p><b>Technique :</b> {@code GET /api/int/parent/interest-suggestions?query=}. Un tableau de chaînes.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link SuggestChildInterestsUseCase}.
 */
@RestController
@RequestMapping("/api/int/parent/interest-suggestions")
@Tag(name = "Parent — centres d'intérêt")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class ParentInterestSuggestionController {

	private final SuggestChildInterestsUseCase suggestions;

	public ParentInterestSuggestionController(SuggestChildInterestsUseCase suggestions) {
		this.suggestions = suggestions;
	}

	@GetMapping
	@Operation(operationId = "suggestParentInterests", summary = "Proposer des centres d'intérêt déjà saisis")
	@ApiResponse(
			responseCode = "200",
			description = "Au plus huit orthographes, ou une liste vide",
			content = @Content(array = @ArraySchema(schema = @Schema(type = "string"))))
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public List<String> suggest(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(name = "query", required = false) String query) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return suggestions.suggest(user.activeRole(), query);
	}
}

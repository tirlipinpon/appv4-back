package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.service.SchoolCatalogSearchService;
import com.appv4.auth.application.service.SchoolCatalogSearchService.SchoolPage;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/** School directory search for parent and teacher forms. */
@RestController
@RequestMapping("/api/int/schools")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class SchoolCatalogController {
	private final SchoolCatalogSearchService search;

	public SchoolCatalogController(SchoolCatalogSearchService search) {
		this.search = search;
	}

	@GetMapping
	@Operation(operationId = "searchSchools", summary = "Search the school directory")
	public SchoolPage search(@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(defaultValue = "") String country, @RequestParam(defaultValue = "") String query,
			@RequestParam(defaultValue = "0") long afterId, @RequestParam(defaultValue = "40") int limit) {
		return search.search(CurrentAuth.require(principal).activeRole(), country, query, afterId, limit);
	}
}

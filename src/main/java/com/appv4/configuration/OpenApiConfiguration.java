package com.appv4.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Contrat OpenAPI (SCRUM-147) — métadonnées globales du spec.
 *
 * <p><b>Business :</b> le front Angular consomme les types générés depuis ce contrat
 * (pas de client HTTP généré de bout en bout).
 * <p><b>Technique :</b> bean SpringDoc ; Swagger UI activé seulement en profil {@code local}.
 * <p><b>Métier / flow :</b> adapters web annotés → {@code GET /v3/api-docs} → artefact CI
 * {@code openapi.json} → génération TS côté front (SCRUM-148).
 */
@Configuration
public class OpenApiConfiguration {

	public static final String BEARER_JWT = "bearer-jwt";

	@Bean
	OpenAPI appv4OpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("appv4 API")
						.description("Contrat HTTP admin (/api/int) et learning (/api/ext). "
								+ "Annotations OpenAPI limitées aux adapters web.")
						.version("0.0.1"))
				.components(new Components()
						.addSecuritySchemes(BEARER_JWT, new SecurityScheme()
								.name(BEARER_JWT)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("JWT admin émis par login / signup / OAuth")));
	}
}

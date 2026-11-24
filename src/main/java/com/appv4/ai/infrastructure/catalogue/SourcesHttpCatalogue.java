package com.appv4.ai.infrastructure.catalogue;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.appv4.ai.application.catalogue.EntreeCatalogue;
import com.appv4.ai.application.catalogue.SourceCatalogue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Lectures HTTP des trois catalogues.
 *
 * <p><b>Business :</b> Artificial Analysis ne part pas sans clé. Les deux autres sont publics.
 * <p><b>Technique :</b> un délai court par appel. Le corps n'est pas gardé.
 */
public final class SourcesHttpCatalogue {

	private SourcesHttpCatalogue() {
	}

	public static SourceCatalogue openRouter(RestClient client, String url) {
		return new SourceSimple("openrouter", "OpenRouter", () -> LectureJsonCatalogue.openRouter(lire(client, url, null)));
	}

	public static SourceCatalogue modelsDev(RestClient client, String url) {
		return new SourceSimple("modelsdev", "models.dev", () -> LectureJsonCatalogue.modelsDev(lire(client, url, null)));
	}

	public static SourceCatalogue artificialAnalysis(RestClient client, String url, String cle) {
		return new SourceSimple("artificial-analysis", "Artificial Analysis", () -> {
			if (cle == null || cle.isBlank()) {
				throw new IllegalStateException("Clé Artificial Analysis absente.");
			}
			List<EntreeCatalogue> toutes = new ArrayList<>();
			for (int page = 1; page <= 20; page++) {
				String adresse = url + (url.contains("?") ? "&" : "?") + "page=" + page;
				JsonNode root = lire(client, adresse, cle);
				toutes.addAll(LectureJsonCatalogue.artificialAnalysis(root));
				if (!root.path("pagination").path("has_more").asBoolean(false)) {
					break;
				}
			}
			return toutes;
		});
	}

	private static JsonNode lire(RestClient client, String url, String cle) {
		try {
			RestClient.RequestHeadersUriSpec<?> appel = client.get();
			String corps = (cle == null || cle.isBlank() ? appel.uri(url) : appel.uri(url).header("x-api-key", cle))
					.retrieve()
					.body(String.class);
			return new ObjectMapper().readTree(corps == null ? "{}" : corps);
		} catch (RestClientException ex) {
			throw new IllegalStateException("Catalogue indisponible.");
		} catch (Exception ex) {
			throw new IllegalStateException("Catalogue illisible.");
		}
	}

	private interface Lecture {
		List<EntreeCatalogue> lire();
	}

	private record SourceSimple(String code, String nom, Lecture lecture) implements SourceCatalogue {
		@Override
		public List<EntreeCatalogue> lire() {
			return lecture.lire();
		}
	}
}

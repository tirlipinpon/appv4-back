package com.appv4.ai.infrastructure.adapter.out.openai;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.appv4.ai.application.BesoinIaException;
import com.appv4.ai.application.CompletionIa;
import com.appv4.ai.application.MessageIa;
import com.appv4.ai.application.UsageIa;
import com.appv4.ai.application.port.out.FournisseurIaPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Fournisseur compatible chat completions (BACK-30).
 *
 * <p><b>Business :</b> sans clé, pas de réponse inventée.
 * <p><b>Technique :</b> la clé part dans l'en-tête et n'est pas recopiée dans l'erreur.
 * <p><b>Métier / flow :</b> adaptateur out, appelé seulement quand la route nomme {@code openai}.
 */
public class OpenAiFournisseurAdapter implements FournisseurIaPort {

	static final String CLE_ABSENTE = "La clé du fournisseur est absente.";
	static final String SANS_REPONSE = "Le fournisseur n'a pas renvoyé de réponse.";

	private final RestClient client;
	private final String apiKey;
	private final ObjectMapper json = new ObjectMapper();

	public OpenAiFournisseurAdapter(String baseUrl, String apiKey) {
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.client = RestClient.builder().baseUrl(baseUrl).build();
	}

	@Override
	public String code() {
		return "openai";
	}

	@Override
	public CompletionIa completer(String modele, List<MessageIa> messages) {
		return completer(modele, messages, apiKey);
	}

	@Override
	public CompletionIa completer(String modele, List<MessageIa> messages, String cle) {
		String key = cle == null ? "" : cle.trim();
		if (key.isEmpty()) {
			throw new BesoinIaException(CLE_ABSENTE);
		}
		String body;
		try {
			body = client.post()
					.uri("/chat/completions")
					.header("Authorization", "Bearer " + key)
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of(
							"model", modele,
							"messages", messages.stream()
									.map(message -> Map.of(
											"role", message.role(),
											"content", message.texte()))
									.toList()))
					.retrieve()
					.body(String.class);
		} catch (RestClientException ex) {
			throw new BesoinIaException(SANS_REPONSE);
		}
		return lire(body);
	}

	private CompletionIa lire(String body) {
		try {
			JsonNode root = json.readTree(body == null ? "" : body);
			String texte = root.path("choices").path(0).path("message").path("content").asText("");
			if (texte.isBlank()) {
				throw new BesoinIaException(SANS_REPONSE);
			}
			JsonNode usage = root.path("usage");
			UsageIa compte = usage.isMissingNode() || usage.isNull()
					? null
					: new UsageIa(entier(usage, "prompt_tokens"), entier(usage, "completion_tokens"));
			return new CompletionIa(texte, compte);
		} catch (BesoinIaException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new BesoinIaException(SANS_REPONSE);
		}
	}

	private static Integer entier(JsonNode node, String field) {
		JsonNode value = node.path(field);
		return value.isNumber() ? value.asInt() : null;
	}
}

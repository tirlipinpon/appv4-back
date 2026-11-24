package com.appv4.ai.infrastructure.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.appv4.ai.application.catalogue.EntreeCatalogue;
import com.fasterxml.jackson.databind.ObjectMapper;

class LectureJsonCatalogueTest {

	private final ObjectMapper json = new ObjectMapper();

	@Test
	void openRouterSepareLeFournisseurEtLeModele() throws Exception {
		var root = json.readTree("""
				{"data":[{"id":"deepseek/deepseek-flash","name":"DeepSeek Flash"}]}
				""");

		EntreeCatalogue entree = LectureJsonCatalogue.openRouter(root).get(0);

		assertThat(entree.fournisseurCode()).isEqualTo("deepseek");
		assertThat(entree.modeleId()).isEqualTo("deepseek/deepseek-flash");
		assertThat(entree.modeleNom()).isEqualTo("DeepSeek Flash");
	}

	@Test
	void modelsDevLitLeNomDuFournisseur() throws Exception {
		var root = json.readTree("""
				{"qwen":{"name":"Qwen","models":{"qwen4-max":{"name":"Qwen4 Max"}}}}
				""");

		EntreeCatalogue entree = LectureJsonCatalogue.modelsDev(root).get(0);

		assertThat(entree.fournisseurNom()).isEqualTo("Qwen");
		assertThat(entree.modeleId()).isEqualTo("qwen4-max");
		assertThat(entree.modeleNom()).isEqualTo("Qwen4 Max");
	}

	@Test
	void artificialAnalysisLitLeCreateur() throws Exception {
		var root = json.readTree("""
				{"data":[{"slug":"glm-5.3","name":"GLM 5.3","model_creator":{"slug":"zhipu","name":"Zhipu"}}]}
				""");

		EntreeCatalogue entree = LectureJsonCatalogue.artificialAnalysis(root).get(0);

		assertThat(entree.fournisseurNom()).isEqualTo("Zhipu");
		assertThat(entree.modeleId()).isEqualTo("glm-5.3");
	}
}

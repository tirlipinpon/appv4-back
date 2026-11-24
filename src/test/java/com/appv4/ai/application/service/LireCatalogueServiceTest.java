package com.appv4.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.appv4.ai.application.BesoinIaException;
import com.appv4.ai.application.CatalogueModeles;
import com.appv4.ai.application.catalogue.EntreeCatalogue;
import com.appv4.ai.application.catalogue.SourceCatalogue;
import com.appv4.auth.application.port.out.ActiveRolePort;

class LireCatalogueServiceTest {

	private final ActiveRolePort roles = mock(ActiveRolePort.class);

	@Test
	void leMemeModeleDeDeuxCataloguesNaPparaitQuUneFois() {
		when(roles.hasAdminRole(1L)).thenReturn(true);
		LireCatalogueService service = new LireCatalogueService(roles, List.of(
				source("openrouter", "OpenRouter", List.of(entree("openrouter", "openai", "gpt-a", "GPT A"))),
				source("modelsdev", "models.dev", List.of(entree("modelsdev", "openai", "gpt-a", "GPT A")))));

		CatalogueModeles lu = service.lire(1L);

		assertThat(lu.lectures()).hasSize(2);
		assertThat(lu.lectures()).allMatch(lecture -> lecture.disponible());
		assertThat(lu.fournisseurs()).hasSize(1);
		assertThat(lu.fournisseurs().get(0).modeles()).hasSize(1);
		assertThat(lu.fournisseurs().get(0).modeles().get(0).catalogues())
				.containsExactly("openrouter", "modelsdev");
	}

	@Test
	void uneSourceEnEchecLaisseLesAutres() {
		when(roles.hasAdminRole(1L)).thenReturn(true);
		LireCatalogueService service = new LireCatalogueService(roles, List.of(
				source("openrouter", "OpenRouter", List.of(entree("openrouter", "deepseek", "deepseek-flash", "Flash"))),
				new SourceCatalogue() {
					@Override
					public String code() {
						return "artificial-analysis";
					}

					@Override
					public String nom() {
						return "Artificial Analysis";
					}

					@Override
					public List<EntreeCatalogue> lire() {
						throw new IllegalStateException("Clé Artificial Analysis absente.");
					}
				}));

		CatalogueModeles lu = service.lire(1L);

		assertThat(lu.lectures().get(1).disponible()).isFalse();
		assertThat(lu.lectures().get(1).message()).isEqualTo("Clé Artificial Analysis absente.");
		assertThat(lu.fournisseurs().get(0).modeles().get(0).id()).isEqualTo("deepseek-flash");
	}

	@Test
	void unParentEstRefuse() {
		when(roles.hasAdminRole(2L)).thenReturn(false);
		LireCatalogueService service = new LireCatalogueService(roles, List.of());

		assertThatThrownBy(() -> service.lire(2L))
				.isInstanceOf(BesoinIaException.class)
				.hasMessage(LireCatalogueService.INTERDIT);
	}

	private static EntreeCatalogue entree(String catalogue, String fournisseur, String id, String nom) {
		return new EntreeCatalogue(catalogue, fournisseur, fournisseur, id, nom);
	}

	private static SourceCatalogue source(String code, String nom, List<EntreeCatalogue> entrees) {
		return new SourceCatalogue() {
			@Override
			public String code() {
				return code;
			}

			@Override
			public String nom() {
				return nom;
			}

			@Override
			public List<EntreeCatalogue> lire() {
				return entrees;
			}
		};
	}
}

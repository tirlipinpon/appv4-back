package com.appv4.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.ai.application.BesoinIaException;
import com.appv4.ai.application.CompletionIa;
import com.appv4.ai.application.CumulJour;
import com.appv4.ai.application.MessageIa;
import com.appv4.ai.application.ReponseIa;
import com.appv4.ai.application.UsageIa;
import com.appv4.ai.application.port.out.FournisseurIaPort;
import com.appv4.ai.application.port.out.RouteBesoin;
import com.appv4.ai.application.port.out.RouteBesoinPort;
import com.appv4.ai.application.port.out.UsageBesoinPort;
import com.appv4.ai.application.AppelEnregistre;

class DemanderBesoinServiceTest {

	private final MemoryRoutes routes = new MemoryRoutes();
	private final MemoryUsage usage = new MemoryUsage();
	private final StubFournisseur fournisseurA = new StubFournisseur("A");
	private final StubFournisseur fournisseurB = new StubFournisseur("B");
	private final Clock midi = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC);
	private DemanderBesoinService service;

	@BeforeEach
	void serviceSansDepart() {
		service = service(List.of(), midi);
	}

	@Test
	void questionDraftPartSurLeFournisseurEtLeModeleDeSaRoute() {
		routes.enregistrer(route("question-draft", "A", "M1", true));

		ReponseIa reponse = service.demander("question-draft", List.of(new MessageIa("user", "bonjour")));

		assertThat(reponse.fournisseur()).isEqualTo("A");
		assertThat(reponse.modele()).isEqualTo("M1");
		assertThat(reponse.texte()).isEqualTo("réponse M1");
		assertThat(fournisseurA.modeles).containsExactly("M1");
		assertThat(fournisseurB.modeles).isEmpty();
	}

	@Test
	void changerLaRouteEnvoieLAppelSuivantSansRejouerLePremier() {
		routes.enregistrer(route("question-draft", "A", "M1", true));
		service.demander("question-draft", List.of(new MessageIa("user", "un")));
		routes.enregistrer(route("question-draft", "B", "M2", true));

		ReponseIa suivante = service.demander("question-draft", List.of(new MessageIa("user", "deux")));

		assertThat(suivante.fournisseur()).isEqualTo("B");
		assertThat(suivante.modele()).isEqualTo("M2");
		assertThat(fournisseurA.modeles).containsExactly("M1");
		assertThat(fournisseurB.modeles).containsExactly("M2");
	}

	@Test
	void unBesoinAbsentDeLaTableEtDuDepartNAppelleAucunFournisseur() {
		assertThatThrownBy(() -> service.demander("question-draft", List.of()))
				.isInstanceOf(BesoinIaException.class)
				.hasMessage(DemanderBesoinService.ABSENT);
		assertThat(fournisseurA.modeles).isEmpty();
		assertThat(fournisseurB.modeles).isEmpty();
	}

	@Test
	void uneTableVidePrendLesRoutesDeDepart() {
		RouteBesoin depart = new RouteBesoin("question-draft", "Proposer des questions", "A", "M1", true);
		service = service(List.of(depart), midi);

		ReponseIa reponse = service.demander("question-draft", List.of());

		assertThat(reponse.texte()).isEqualTo("réponse M1");
		assertThat(routes.trouver("question-draft")).contains(depart);
	}

	@Test
	void unBesoinInactifNAppellePasLeFournisseur() {
		routes.enregistrer(route("question-draft", "A", "M1", false));

		assertThatThrownBy(() -> service.demander("question-draft", List.of()))
				.isInstanceOf(BesoinIaException.class)
				.hasMessage(DemanderBesoinService.INACTIF);
		assertThat(fournisseurA.modeles).isEmpty();
	}

	@Test
	void unFournisseurSansAdaptateurNInventePasDeReponse() {
		routes.enregistrer(route("question-draft", "inconnu", "M1", true));

		assertThatThrownBy(() -> service.demander("question-draft", List.of()))
				.isInstanceOf(BesoinIaException.class)
				.hasMessage(DemanderBesoinService.SANS_ADAPTATEUR);
		assertThat(fournisseurA.modeles).isEmpty();
	}

	@Test
	void answerAnalysisEtLearnerSupportChoisissentLeurPropreFournisseur() {
		routes.enregistrer(route("answer-analysis", "B", "analyse", true));
		routes.enregistrer(route("learner-support", "A", "soutien", true));

		assertThat(service.demander("answer-analysis", List.of()).modele()).isEqualTo("analyse");
		assertThat(service.demander("learner-support", List.of()).modele()).isEqualTo("soutien");
		assertThat(fournisseurB.modeles).containsExactly("analyse");
		assertThat(fournisseurA.modeles).containsExactly("soutien");
	}

	@Test
	void unAppelReussiCompteDouzeEtHuitSurLeJourUtc() {
		routes.enregistrer(route("question-draft", "A", "M1", true));
		fournisseurA.usage = new UsageIa(12, 8);

		service.demander("question-draft", List.of(new MessageIa("user", "bonjour")));

		AppelEnregistre appel = usage.appels().get(0);
		assertThat(appel.tokensPrompt()).isEqualTo(12);
		assertThat(appel.tokensReponse()).isEqualTo(8);
		assertThat(appel.tokensTotal()).isEqualTo(20);
		assertThat(appel.usageSignale()).isTrue();
		assertThat(appel.statut()).isEqualTo(DemanderBesoinService.STATUT_OK);
		assertThat(appel.jourUtc()).isEqualTo(LocalDate.of(2026, 11, 24));
		assertThat(usage.cumuls()).containsExactly(
				new CumulJour(LocalDate.of(2026, 11, 24), "question-draft", "A", "M1", 1, 20));
	}

	@Test
	void unSecondAppelLeMemeJourAugmenteLaMemeLigne() {
		routes.enregistrer(route("question-draft", "A", "M1", true));
		fournisseurA.usage = new UsageIa(12, 8);
		service.demander("question-draft", List.of());
		fournisseurA.usage = new UsageIa(5, 5);
		service.demander("question-draft", List.of());

		assertThat(usage.appels()).hasSize(2);
		assertThat(usage.cumuls()).containsExactly(
				new CumulJour(LocalDate.of(2026, 11, 24), "question-draft", "A", "M1", 2, 30));
	}

	@Test
	void unAutreModeleOuvreUneNouvelleLigneEtLaisseLAncienne() {
		routes.enregistrer(route("question-draft", "A", "M1", true));
		fournisseurA.usage = new UsageIa(12, 8);
		service.demander("question-draft", List.of());
		fournisseurA.usage = new UsageIa(5, 5);
		service.demander("question-draft", List.of());
		routes.enregistrer(route("question-draft", "B", "M2", true));
		fournisseurB.usage = new UsageIa(4, 4);
		service.demander("question-draft", List.of());

		assertThat(usage.cumuls()).containsExactly(
				new CumulJour(LocalDate.of(2026, 11, 24), "question-draft", "A", "M1", 2, 30),
				new CumulJour(LocalDate.of(2026, 11, 24), "question-draft", "B", "M2", 1, 8));
	}

	@Test
	void minuitEtDemiAParisEnHiverTombeSurLaVeilleUtc() {
		Clock uneHeureMoinsLeQuart = Clock.fixed(Instant.parse("2026-11-23T23:30:00Z"), ZoneOffset.UTC);
		service = service(List.of(), uneHeureMoinsLeQuart);
		routes.enregistrer(route("question-draft", "A", "M1", true));
		fournisseurA.usage = new UsageIa(1, 1);

		service.demander("question-draft", List.of());

		assertThat(usage.cumuls().get(0).jourUtc()).isEqualTo(LocalDate.of(2026, 11, 23));
	}

	@Test
	void sansUsageLeCumulCompteLAppelEtZeroToken() {
		routes.enregistrer(route("question-draft", "A", "M1", true));

		service.demander("question-draft", List.of());

		assertThat(usage.appels().get(0).tokensTotal()).isZero();
		assertThat(usage.appels().get(0).usageSignale()).isFalse();
		assertThat(usage.cumuls()).containsExactly(
				new CumulJour(LocalDate.of(2026, 11, 24), "question-draft", "A", "M1", 1, 0));
	}

	@Test
	void unFournisseurEnEchecCompteLAppelSansReponseInventee() {
		routes.enregistrer(route("question-draft", "A", "M1", true));
		fournisseurA.echec = new BesoinIaException("panne");

		assertThatThrownBy(() -> service.demander("question-draft", List.of()))
				.isInstanceOf(BesoinIaException.class)
				.hasMessage("panne");
		assertThat(usage.appels().get(0).statut()).isEqualTo(DemanderBesoinService.STATUT_ERREUR);
		assertThat(usage.appels().get(0).tokensTotal()).isZero();
		assertThat(usage.cumuls()).containsExactly(
				new CumulJour(LocalDate.of(2026, 11, 24), "question-draft", "A", "M1", 1, 0));
	}

	private DemanderBesoinService service(List<RouteBesoin> depart, Clock clock) {
		return new DemanderBesoinService(routes, List.of(fournisseurA, fournisseurB), depart, usage, clock);
	}

	private static RouteBesoin route(String code, String fournisseur, String modele, boolean actif) {
		return new RouteBesoin(code, code, fournisseur, modele, actif);
	}

	private static final class MemoryRoutes implements RouteBesoinPort {
		private final Map<String, RouteBesoin> rows = new LinkedHashMap<>();

		@Override
		public Optional<RouteBesoin> trouver(String code) {
			return Optional.ofNullable(rows.get(code));
		}

		@Override
		public List<RouteBesoin> toutes() {
			return List.copyOf(rows.values());
		}

		@Override
		public void enregistrer(RouteBesoin route) {
			rows.put(route.code(), route);
		}
	}

	private static final class StubFournisseur implements FournisseurIaPort {
		private final String code;
		private final List<String> modeles = new ArrayList<>();
		private UsageIa usage;
		private RuntimeException echec;

		private StubFournisseur(String code) {
			this.code = code;
		}

		@Override
		public String code() {
			return code;
		}

		@Override
		public CompletionIa completer(String modele, List<MessageIa> messages) {
			modeles.add(modele);
			if (echec != null) {
				throw echec;
			}
			return new CompletionIa("réponse " + modele, usage);
		}
	}

	private static final class MemoryUsage implements UsageBesoinPort {
		private final List<AppelEnregistre> appels = new ArrayList<>();
		private final List<CumulJour> cumuls = new ArrayList<>();

		@Override
		public void noter(AppelEnregistre appel) {
			appels.add(appel);
			for (int i = 0; i < cumuls.size(); i++) {
				CumulJour row = cumuls.get(i);
				if (row.jourUtc().equals(appel.jourUtc())
						&& row.codeBesoin().equals(appel.codeBesoin())
						&& row.fournisseur().equals(appel.fournisseur())
						&& row.modele().equals(appel.modele())) {
					cumuls.set(i, new CumulJour(
							row.jourUtc(),
							row.codeBesoin(),
							row.fournisseur(),
							row.modele(),
							row.appels() + 1,
							row.tokens() + appel.tokensTotal()));
					return;
				}
			}
			cumuls.add(new CumulJour(
					appel.jourUtc(), appel.codeBesoin(), appel.fournisseur(), appel.modele(), 1, appel.tokensTotal()));
		}

		@Override
		public List<AppelEnregistre> appels() {
			return List.copyOf(appels);
		}

		@Override
		public List<CumulJour> cumuls() {
			return List.copyOf(cumuls);
		}
	}
}

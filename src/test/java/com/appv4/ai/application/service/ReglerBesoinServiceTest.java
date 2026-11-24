package com.appv4.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.ai.application.BesoinIaException;
import com.appv4.ai.application.CumulJour;
import com.appv4.ai.application.LectureBesoins;
import com.appv4.ai.application.LectureUsage;
import com.appv4.ai.application.port.out.ChangementEnregistre;
import com.appv4.ai.application.port.out.FournisseurIaPort;
import com.appv4.ai.application.port.out.HistoriqueBesoinPort;
import com.appv4.ai.application.port.out.RouteBesoin;
import com.appv4.ai.application.port.out.RouteBesoinPort;
import com.appv4.ai.application.port.out.UsageBesoinPort;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;

class ReglerBesoinServiceTest {

	private static final Instant PARIS_MINUIT_TRENTE = Instant.parse("2026-11-23T23:30:00Z");

	private final ActiveRolePort roles = mock(ActiveRolePort.class);
	private final AuthUserAccountPort comptes = mock(AuthUserAccountPort.class);
	private final UsageBesoinPort usage = mock(UsageBesoinPort.class);
	private final FournisseurIaPort openai = mock(FournisseurIaPort.class);
	private final RoutesMemoire routes = new RoutesMemoire();
	private final HistoriqueMemoire historique = new HistoriqueMemoire();
	private ReglerBesoinService service;

	@BeforeEach
	void setUp() {
		when(roles.hasAdminRole(1L)).thenReturn(true);
		when(comptes.findById(1L)).thenReturn(Optional.of(new AuthUserAccount(1L, "ada@example.com", "Ada", true)));
		when(openai.code()).thenReturn("openai");
		service = new ReglerBesoinService(
				roles,
				comptes,
				routes,
				historique,
				usage,
				List.of(openai),
				List.of(new RouteBesoin("question-draft", "Proposer des questions", "openai", "depart", true)),
				Clock.fixed(PARIS_MINUIT_TRENTE, ZoneOffset.UTC));
	}

	@Test
	void laLecturePoseLeBesoinDeDepartEtLeFournisseurQuiAUnAdaptateur() {
		LectureBesoins lu = service.lire(1L);

		assertThat(lu.besoins()).containsExactly(
				new RouteBesoin("question-draft", "Proposer des questions", "openai", "depart", true));
		assertThat(lu.fournisseurs()).containsExactly("openai");
	}

	@Test
	void unParentEstRefuseEtRienNestEcrit() {
		when(roles.hasAdminRole(2L)).thenReturn(false);

		assertThatThrownBy(() -> service.creer(2L, "projet", "Créer un projet", "openai", "depart"))
				.isInstanceOf(BesoinIaException.class)
				.hasMessage(ReglerBesoinService.INTERDIT);
		assertThat(routes.toutes()).isEmpty();
		assertThat(historique.tous()).isEmpty();
	}

	@Test
	void creerUnBesoinGardeLeCodeEtNoteLHistorique() {
		service.lire(1L);

		RouteBesoin cree = service.creer(1L, "projet", " Créer un projet ", "openai", " depart ");

		assertThat(cree).isEqualTo(new RouteBesoin("projet", "Créer un projet", "openai", "depart", true));
		assertThat(historique.tous()).hasSize(1);
		assertThat(historique.tous().get(0).nomAdmin()).isEqualTo("Ada");
		assertThat(historique.tous().get(0).ancienModele()).isEmpty();
		assertThat(historique.tous().get(0).nouveauModele()).isEqualTo("depart");
		verify(openai, never()).completer(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void unCodeDejaPrisOuMalFormeEstRefuseSansHistorique() {
		service.lire(1L);

		assertThatThrownBy(() -> service.creer(1L, "question-draft", "Autre", "openai", "depart"))
				.hasMessage(ReglerBesoinService.CODE_PRIS);
		assertThatThrownBy(() -> service.creer(1L, "Projet", "Créer un projet", "openai", "depart"))
				.hasMessage(ReglerBesoinService.CODE_INVALIDE);
		assertThatThrownBy(() -> service.creer(1L, "projet", "  ", "openai", "depart"))
				.hasMessage(ReglerBesoinService.NOM_VIDE);
		assertThatThrownBy(() -> service.creer(1L, "projet", "Créer un projet", "qwen", "depart"))
				.hasMessage(ReglerBesoinService.SANS_ADAPTATEUR);
		assertThat(historique.tous()).isEmpty();
	}

	@Test
	void changerLaPaireLaisseLAncienneDansLHistorique() {
		service.lire(1L);

		RouteBesoin suivant = service.changer(1L, "question-draft", "openai", "mini");

		assertThat(suivant.modele()).isEqualTo("mini");
		assertThat(historique.tous()).hasSize(1);
		ChangementEnregistre ligne = historique.tous().get(0);
		assertThat(ligne.ancienModele()).isEqualTo("depart");
		assertThat(ligne.nouveauModele()).isEqualTo("mini");
		assertThat(service.historique(1L).get(0).dateLabel()).isEqualTo("24 novembre 2026");
		assertThat(service.historique(1L).get(0).timeLabel()).isEqualTo("00:30");
		assertThat(service.historique(1L).get(0).nomAdmin()).isEqualTo("Ada");
	}

	@Test
	void unChangementRefuseGardeLaPaire() {
		service.lire(1L);

		assertThatThrownBy(() -> service.changer(1L, "question-draft", "openai", "  "))
				.hasMessage(ReglerBesoinService.MODELE_VIDE);
		assertThatThrownBy(() -> service.changer(1L, "absent", "openai", "mini"))
				.hasMessage(ReglerBesoinService.INCONNU);

		assertThat(routes.trouver("question-draft").orElseThrow().modele()).isEqualTo("depart");
		assertThat(historique.tous()).isEmpty();
	}

	@Test
	void desactiverGardeLaLigneEtNEcritPasDHistorique() {
		service.lire(1L);

		RouteBesoin eteint = service.activer(1L, "question-draft", false);

		assertThat(eteint.actif()).isFalse();
		assertThat(routes.trouver("question-draft").orElseThrow().modele()).isEqualTo("depart");
		assertThat(historique.tous()).isEmpty();
		assertThat(service.activer(1L, "question-draft", true).actif()).isTrue();
	}

	@Test
	void laConsommationDuJourUtcNeCreePasDeLigneVide() {
		service.lire(1L);
		when(usage.cumuls()).thenReturn(List.of(
				new CumulJour(LocalDate.parse("2026-11-23"), "question-draft", "openai", "depart", 1, 20),
				new CumulJour(LocalDate.parse("2026-11-22"), "question-draft", "openai", "mini", 2, 4)));

		LectureUsage aujourdhui = service.usage(1L, null, null);
		LectureUsage vide = service.usage(1L, LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-02"));

		assertThat(aujourdhui.du()).isEqualTo(LocalDate.parse("2026-11-23"));
		assertThat(aujourdhui.au()).isEqualTo(LocalDate.parse("2026-11-23"));
		assertThat(aujourdhui.lignes()).hasSize(1);
		assertThat(aujourdhui.lignes().get(0).nom()).isEqualTo("Proposer des questions");
		assertThat(aujourdhui.lignes().get(0).appels()).isEqualTo(1);
		assertThat(aujourdhui.lignes().get(0).tokens()).isEqualTo(20);
		assertThat(vide.lignes()).isEmpty();
		assertThat(vide.du()).isEqualTo(LocalDate.parse("2026-11-01"));
	}

	private static final class RoutesMemoire implements RouteBesoinPort {
		private final List<RouteBesoin> lignes = new ArrayList<>();

		@Override
		public Optional<RouteBesoin> trouver(String code) {
			return lignes.stream().filter(route -> route.code().equals(code)).findFirst();
		}

		@Override
		public List<RouteBesoin> toutes() {
			return List.copyOf(lignes);
		}

		@Override
		public void enregistrer(RouteBesoin route) {
			lignes.removeIf(ligne -> ligne.code().equals(route.code()));
			lignes.add(route);
		}
	}

	private static final class HistoriqueMemoire implements HistoriqueBesoinPort {
		private final List<ChangementEnregistre> lignes = new ArrayList<>();

		@Override
		public void ajouter(ChangementEnregistre changement) {
			lignes.add(0, changement);
		}

		@Override
		public List<ChangementEnregistre> tous() {
			return List.copyOf(lignes);
		}
	}
}

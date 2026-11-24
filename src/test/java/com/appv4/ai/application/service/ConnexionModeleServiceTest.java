package com.appv4.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.ai.application.BesoinIaException;
import com.appv4.ai.application.CompletionIa;
import com.appv4.ai.application.ConnexionModele;
import com.appv4.ai.application.EssaiConnexion;
import com.appv4.ai.application.MessageIa;
import com.appv4.ai.application.port.out.ConnexionModelePort;
import com.appv4.ai.application.port.out.ConnexionModelePort.Secrete;
import com.appv4.ai.application.port.out.FournisseurIaPort;
import com.appv4.auth.application.port.out.ActiveRolePort;

class ConnexionModeleServiceTest {

	private static final long ADMIN = 1L;
	private static final long PARENT = 2L;

	private final ActiveRolePort roles = mock(ActiveRolePort.class);
	private final MemoryConnexions connexions = new MemoryConnexions();
	private final StubFournisseur openai = new StubFournisseur("openai");
	private ConnexionModeleService service;

	@BeforeEach
	void adminSeul() {
		when(roles.hasAdminRole(ADMIN)).thenReturn(true);
		when(roles.hasAdminRole(PARENT)).thenReturn(false);
		service = new ConnexionModeleService(roles, connexions, List.of(openai));
	}

	@Test
	void creerRelitLeModeleSansLaCle() {
		ConnexionModele creee = service.creer(ADMIN, " openai ", " mini ", " cle-secrete ");

		assertThat(creee.fournisseur()).isEqualTo("openai");
		assertThat(creee.modele()).isEqualTo("mini");
		assertThat(service.lister(ADMIN)).containsExactly(creee);
		assertThat(connexions.cleDe(creee.id())).isEqualTo("cle-secrete");
	}

	@Test
	void unEssaiReussiRenvoieLeTexteDuFournisseur() {
		ConnexionModele creee = service.creer(ADMIN, "openai", "mini", "cle");
		openai.texte = "pong";

		EssaiConnexion essai = service.essayer(ADMIN, creee.id());

		assertThat(essai.succes()).isTrue();
		assertThat(essai.message()).isEqualTo("pong");
		assertThat(openai.modeles).containsExactly("mini");
		assertThat(openai.cles).containsExactly("cle");
	}

	@Test
	void unEssaiRateNeReussitPas() {
		ConnexionModele creee = service.creer(ADMIN, "openai", "mini", "cle");
		openai.echec = new BesoinIaException("panne");

		EssaiConnexion essai = service.essayer(ADMIN, creee.id());

		assertThat(essai.succes()).isFalse();
		assertThat(essai.message()).isEqualTo("panne");
	}

	@Test
	void uneCleVideNEcritRien() {
		assertThatThrownBy(() -> service.creer(ADMIN, "openai", "mini", "  "))
				.isInstanceOf(BesoinIaException.class)
				.hasMessage(ConnexionModeleService.CLE_VIDE);
		assertThat(service.lister(ADMIN)).isEmpty();
	}

	@Test
	void unFournisseurSansAdaptateurNEcritRien() {
		assertThatThrownBy(() -> service.creer(ADMIN, "autre", "mini", "cle"))
				.isInstanceOf(BesoinIaException.class)
				.hasMessage(ConnexionModeleService.SANS_ADAPTATEUR);
		assertThat(service.lister(ADMIN)).isEmpty();
		assertThat(openai.modeles).isEmpty();
	}

	@Test
	void unParentEstRefuseEtRienNEstEcrit() {
		assertThatThrownBy(() -> service.creer(PARENT, "openai", "mini", "cle"))
				.isInstanceOf(BesoinIaException.class)
				.hasMessage(ConnexionModeleService.INTERDIT);
		assertThat(connexions.lister()).isEmpty();
	}

	private static final class MemoryConnexions implements ConnexionModelePort {
		private final Map<Long, Secrete> rows = new LinkedHashMap<>();
		private long next = 1L;

		@Override
		public ConnexionModele enregistrer(String fournisseur, String modele, String cle) {
			long id = next++;
			rows.put(id, new Secrete(id, fournisseur, modele, cle));
			return new ConnexionModele(id, fournisseur, modele);
		}

		@Override
		public List<ConnexionModele> lister() {
			return rows.values().stream()
					.map(row -> new ConnexionModele(row.id(), row.fournisseur(), row.modele()))
					.toList();
		}

		@Override
		public Optional<Secrete> trouver(long id) {
			return Optional.ofNullable(rows.get(id));
		}

		private String cleDe(long id) {
			return rows.get(id).cle();
		}
	}

	private static final class StubFournisseur implements FournisseurIaPort {
		private final String code;
		private final List<String> modeles = new ArrayList<>();
		private final List<String> cles = new ArrayList<>();
		private String texte = "pong";
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
			return completer(modele, messages, "");
		}

		@Override
		public CompletionIa completer(String modele, List<MessageIa> messages, String cle) {
			modeles.add(modele);
			cles.add(cle);
			if (echec != null) {
				throw echec;
			}
			return new CompletionIa(texte, null);
		}
	}
}

package com.appv4.ai.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.appv4.ai.application.AppelEnregistre;
import com.appv4.ai.application.BesoinIaException;
import com.appv4.ai.application.CompletionIa;
import com.appv4.ai.application.MessageIa;
import com.appv4.ai.application.ReponseIa;
import com.appv4.ai.application.UsageIa;
import com.appv4.ai.application.port.in.DemanderBesoinUseCase;
import com.appv4.ai.application.port.out.FournisseurIaPort;
import com.appv4.ai.application.port.out.RouteBesoin;
import com.appv4.ai.application.port.out.RouteBesoinPort;
import com.appv4.ai.application.port.out.UsageBesoinPort;

/**
 * Route un besoin vers le modèle configuré (BACK-30).
 *
 * <p><b>Business :</b> changer le modèle ne rejoue pas l'appel précédent. Un besoin inactif ne part pas.
 * <p><b>Technique :</b> si aucune route n'est encore là, les routes de départ sont posées une fois.
 * <p><b>Métier / flow :</b> port in → route → adaptateur → cumul du jour UTC.
 */
public class DemanderBesoinService implements DemanderBesoinUseCase {

	static final String ABSENT = "Ce besoin n'a pas de modèle.";
	static final String INACTIF = "Ce besoin est inactif.";
	static final String SANS_ADAPTATEUR = "Ce fournisseur n'a pas d'adaptateur.";
	static final String STATUT_OK = "ok";
	static final String STATUT_ERREUR = "erreur";

	private final RouteBesoinPort routes;
	private final Map<String, FournisseurIaPort> fournisseurs;
	private final List<RouteBesoin> depart;
	private final UsageBesoinPort usage;
	private final Clock clock;

	public DemanderBesoinService(
			RouteBesoinPort routes,
			List<FournisseurIaPort> fournisseurs,
			List<RouteBesoin> depart,
			UsageBesoinPort usage,
			Clock clock) {
		this.routes = routes;
		this.fournisseurs = fournisseurs.stream()
				.collect(Collectors.toMap(FournisseurIaPort::code, Function.identity()));
		this.depart = List.copyOf(depart);
		this.usage = usage;
		this.clock = clock;
	}

	@Override
	public ReponseIa demander(String codeBesoin, List<MessageIa> messages) {
		poserLeDepartSiVide();
		RouteBesoin route = routes.trouver(codeBesoin).orElseThrow(() -> new BesoinIaException(ABSENT));
		if (!route.actif()) {
			throw new BesoinIaException(INACTIF);
		}
		FournisseurIaPort fournisseur = fournisseurs.get(route.fournisseur());
		if (fournisseur == null) {
			throw new BesoinIaException(SANS_ADAPTATEUR);
		}
		long debut = clock.millis();
		try {
			CompletionIa completion = fournisseur.completer(route.modele(), messages);
			noter(route, completion.usage(), true, STATUT_OK, debut);
			return new ReponseIa(route.fournisseur(), route.modele(), completion.texte(), completion.usage());
		} catch (RuntimeException ex) {
			noter(route, null, false, STATUT_ERREUR, debut);
			throw ex;
		}
	}

	private void noter(RouteBesoin route, UsageIa usageIa, boolean signale, String statut, long debut) {
		int prompt = usageIa == null || usageIa.tokensPrompt() == null ? 0 : usageIa.tokensPrompt();
		int reponse = usageIa == null || usageIa.tokensReponse() == null ? 0 : usageIa.tokensReponse();
		Instant instant = clock.instant();
		usage.noter(new AppelEnregistre(
				route.code(),
				route.fournisseur(),
				route.modele(),
				LocalDate.ofInstant(instant, ZoneOffset.UTC),
				prompt,
				reponse,
				prompt + reponse,
				signale && usageIa != null,
				statut,
				clock.millis() - debut,
				instant));
	}

	private void poserLeDepartSiVide() {
		if (!routes.toutes().isEmpty() || depart.isEmpty()) {
			return;
		}
		for (RouteBesoin route : depart) {
			routes.enregistrer(route);
		}
	}
}

package com.appv4.ai.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.appv4.ai.application.BesoinIaException;
import com.appv4.ai.application.ChangementBesoin;
import com.appv4.ai.application.CumulJour;
import com.appv4.ai.application.LectureBesoins;
import com.appv4.ai.application.LectureUsage;
import com.appv4.ai.application.LigneUsage;
import com.appv4.ai.application.port.in.ReglerBesoinUseCase;
import com.appv4.ai.application.port.out.ChangementEnregistre;
import com.appv4.ai.application.port.out.FournisseurIaPort;
import com.appv4.ai.application.port.out.HistoriqueBesoinPort;
import com.appv4.ai.application.port.out.RouteBesoin;
import com.appv4.ai.application.port.out.RouteBesoinPort;
import com.appv4.ai.application.port.out.UsageBesoinPort;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.AuthUserAccountPort;

/**
 * Règle les besoins sans appeler le fournisseur (BACK-32).
 *
 * <p><b>Business :</b> l'admin ajoute un besoin, change sa paire, le désactive. La consommation passée reste.
 * <p><b>Technique :</b> le code suit {@code ^[a-z0-9]+(?:-[a-z0-9]+)*$}. Le jour lu est UTC.
 * L'heure d'un changement est libellée en Europe/Brussels.
 * <p><b>Métier / flow :</b> page IA → ce service → route, historique, cumul. Pas d'adaptateur.
 */
public class ReglerBesoinService implements ReglerBesoinUseCase {

	static final String INTERDIT = "Réservé à l'admin.";
	static final String NOM_VIDE = "Le nom est vide.";
	static final String CODE_VIDE = "Le code est vide.";
	static final String CODE_INVALIDE = "Le code doit être un mot court en minuscules.";
	static final String NOM_TROP_LONG = "Le nom est trop long.";
	static final String MODELE_TROP_LONG = "Le modèle est trop long.";
	static final String CODE_PRIS = "Ce code existe déjà.";
	static final String MODELE_VIDE = "Le modèle est vide.";
	static final String SANS_ADAPTATEUR = "Ce fournisseur n'a pas d'adaptateur.";
	static final String INCONNU = "Ce besoin est inconnu.";
	static final String COMPTE_INCONNU = "Ce compte est introuvable.";

	private static final Pattern CODE = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
	private static final ZoneId BRUSSELS = ZoneId.of("Europe/Brussels");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
	private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);

	private final ActiveRolePort roles;
	private final AuthUserAccountPort comptes;
	private final RouteBesoinPort routes;
	private final HistoriqueBesoinPort historique;
	private final UsageBesoinPort usage;
	private final Map<String, FournisseurIaPort> fournisseurs;
	private final List<RouteBesoin> depart;
	private final Clock clock;

	public ReglerBesoinService(
			ActiveRolePort roles,
			AuthUserAccountPort comptes,
			RouteBesoinPort routes,
			HistoriqueBesoinPort historique,
			UsageBesoinPort usage,
			List<FournisseurIaPort> fournisseurs,
			List<RouteBesoin> depart,
			Clock clock) {
		this.roles = roles;
		this.comptes = comptes;
		this.routes = routes;
		this.historique = historique;
		this.usage = usage;
		this.fournisseurs = fournisseurs.stream()
				.collect(Collectors.toMap(FournisseurIaPort::code, Function.identity()));
		this.depart = List.copyOf(depart);
		this.clock = clock;
	}

	@Override
	public LectureBesoins lire(long callerId) {
		exigerAdmin(callerId);
		poserLeDepartSiVide();
		List<String> maisons = fournisseurs.keySet().stream().sorted().toList();
		return new LectureBesoins(routes.toutes(), maisons);
	}

	@Override
	public RouteBesoin creer(long callerId, String code, String nom, String fournisseur, String modele) {
		exigerAdmin(callerId);
		poserLeDepartSiVide();
		String crochet = code == null ? "" : code.trim();
		String titre = nom == null ? "" : nom.trim();
		String maison = fournisseur == null ? "" : fournisseur.trim();
		String nomModele = modele == null ? "" : modele.trim();
		if (titre.isEmpty()) {
			throw new BesoinIaException(NOM_VIDE);
		}
		if (crochet.isEmpty()) {
			throw new BesoinIaException(CODE_VIDE);
		}
		if (!CODE.matcher(crochet).matches() || crochet.length() > 64) {
			throw new BesoinIaException(CODE_INVALIDE);
		}
		if (titre.length() > 120) {
			throw new BesoinIaException(NOM_TROP_LONG);
		}
		if (nomModele.isEmpty()) {
			throw new BesoinIaException(MODELE_VIDE);
		}
		if (nomModele.length() > 120) {
			throw new BesoinIaException(MODELE_TROP_LONG);
		}
		if (routes.trouver(crochet).isPresent()) {
			throw new BesoinIaException(CODE_PRIS);
		}
		exigerFournisseur(maison);
		RouteBesoin route = new RouteBesoin(crochet, titre, maison, nomModele, true);
		routes.enregistrer(route);
		noterChangement(callerId, route, "", "", maison, nomModele);
		return route;
	}

	@Override
	public RouteBesoin changer(long callerId, String code, String fournisseur, String modele) {
		exigerAdmin(callerId);
		poserLeDepartSiVide();
		RouteBesoin actuel = exigerRoute(code);
		String maison = fournisseur == null ? "" : fournisseur.trim();
		String nomModele = modele == null ? "" : modele.trim();
		if (nomModele.isEmpty()) {
			throw new BesoinIaException(MODELE_VIDE);
		}
		if (nomModele.length() > 120) {
			throw new BesoinIaException(MODELE_TROP_LONG);
		}
		exigerFournisseur(maison);
		RouteBesoin suivant = new RouteBesoin(actuel.code(), actuel.nom(), maison, nomModele, actuel.actif());
		routes.enregistrer(suivant);
		noterChangement(callerId, suivant, actuel.fournisseur(), actuel.modele(), maison, nomModele);
		return suivant;
	}

	@Override
	public RouteBesoin activer(long callerId, String code, boolean actif) {
		exigerAdmin(callerId);
		poserLeDepartSiVide();
		RouteBesoin actuel = exigerRoute(code);
		RouteBesoin suivant = new RouteBesoin(
				actuel.code(), actuel.nom(), actuel.fournisseur(), actuel.modele(), actif);
		routes.enregistrer(suivant);
		return suivant;
	}

	@Override
	public LectureUsage usage(long callerId, LocalDate du, LocalDate au) {
		exigerAdmin(callerId);
		poserLeDepartSiVide();
		LocalDate aujourdhui = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
		LocalDate debut = du == null ? aujourdhui : du;
		LocalDate fin = au == null ? aujourdhui : au;
		Map<String, String> noms = routes.toutes().stream()
				.collect(Collectors.toMap(RouteBesoin::code, RouteBesoin::nom, (gauche, droite) -> gauche));
		List<LigneUsage> lignes = new ArrayList<>();
		for (CumulJour cumul : usage.cumuls()) {
			if (cumul.jourUtc().isBefore(debut) || cumul.jourUtc().isAfter(fin)) {
				continue;
			}
			lignes.add(new LigneUsage(
					cumul.jourUtc(),
					cumul.codeBesoin(),
					noms.getOrDefault(cumul.codeBesoin(), ""),
					cumul.fournisseur(),
					cumul.modele(),
					cumul.appels(),
					cumul.tokens()));
		}
		lignes.sort(Comparator
				.comparing(LigneUsage::jour)
				.thenComparing(LigneUsage::code)
				.thenComparing(LigneUsage::fournisseur)
				.thenComparing(LigneUsage::modele));
		return new LectureUsage(debut, fin, List.copyOf(lignes));
	}

	@Override
	public List<ChangementBesoin> historique(long callerId) {
		exigerAdmin(callerId);
		return historique.tous().stream().map(this::libelle).toList();
	}

	private ChangementBesoin libelle(ChangementEnregistre changement) {
		var quand = changement.instant().atZone(BRUSSELS);
		return new ChangementBesoin(
				changement.code(),
				changement.nom(),
				changement.nomAdmin(),
				DATE.format(quand),
				HEURE.format(quand),
				changement.ancienFournisseur(),
				changement.ancienModele(),
				changement.nouveauFournisseur(),
				changement.nouveauModele());
	}

	private void noterChangement(
			long callerId,
			RouteBesoin route,
			String ancienFournisseur,
			String ancienModele,
			String nouveauFournisseur,
			String nouveauModele) {
		String nomAdmin = comptes.findById(callerId)
				.map(compte -> compte.displayName() == null ? "" : compte.displayName())
				.orElseThrow(() -> new BesoinIaException(COMPTE_INCONNU, BesoinIaException.Code.NOT_FOUND));
		historique.ajouter(new ChangementEnregistre(
				route.code(),
				route.nom(),
				nomAdmin,
				clock.instant(),
				ancienFournisseur,
				ancienModele,
				nouveauFournisseur,
				nouveauModele));
	}

	private RouteBesoin exigerRoute(String code) {
		return routes.trouver(code).orElseThrow(() -> new BesoinIaException(INCONNU, BesoinIaException.Code.NOT_FOUND));
	}

	private void exigerFournisseur(String maison) {
		if (!fournisseurs.containsKey(maison)) {
			throw new BesoinIaException(SANS_ADAPTATEUR);
		}
	}

	private void exigerAdmin(long callerId) {
		if (!roles.hasAdminRole(callerId)) {
			throw new BesoinIaException(INTERDIT, BesoinIaException.Code.FORBIDDEN);
		}
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

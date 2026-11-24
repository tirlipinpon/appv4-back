package com.appv4.ai.application.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.appv4.ai.application.BesoinIaException;
import com.appv4.ai.application.ConnexionModele;
import com.appv4.ai.application.EssaiConnexion;
import com.appv4.ai.application.MessageIa;
import com.appv4.ai.application.port.in.ConnexionModeleUseCase;
import com.appv4.ai.application.port.out.ConnexionModelePort;
import com.appv4.ai.application.port.out.ConnexionModelePort.Secrete;
import com.appv4.ai.application.port.out.FournisseurIaPort;
import com.appv4.auth.application.port.out.ActiveRolePort;

/**
 * Garde une connexion et dit si l'essai a répondu (BACK-84).
 *
 * <p><b>Business :</b> l'essai ne compte pas comme un appel de besoin.
 * <p><b>Technique :</b> la clé n'est pas dans {@link ConnexionModele}. Un échec du fournisseur
 * reste un résultat, pas une réponse inventée.
 * <p><b>Métier / flow :</b> port in admin → connexion → adaptateur du fournisseur.
 */
public class ConnexionModeleService implements ConnexionModeleUseCase {

	static final String INTERDIT = "Réservé à l'admin.";
	static final String CLE_VIDE = "La clé du fournisseur est absente.";
	static final String SANS_ADAPTATEUR = "Ce fournisseur n'a pas d'adaptateur.";
	static final String MODELE_VIDE = "Le modèle est vide.";
	static final String INCONNUE = "Cette connexion est introuvable.";

	private final ActiveRolePort roles;
	private final ConnexionModelePort connexions;
	private final Map<String, FournisseurIaPort> fournisseurs;

	public ConnexionModeleService(
			ActiveRolePort roles,
			ConnexionModelePort connexions,
			List<FournisseurIaPort> fournisseurs) {
		this.roles = roles;
		this.connexions = connexions;
		this.fournisseurs = fournisseurs.stream()
				.collect(Collectors.toMap(FournisseurIaPort::code, Function.identity()));
	}

	@Override
	public List<ConnexionModele> lister(long callerId) {
		exigerAdmin(callerId);
		return connexions.lister();
	}

	@Override
	public ConnexionModele creer(long callerId, String fournisseur, String modele, String cle) {
		exigerAdmin(callerId);
		String maison = fournisseur == null ? "" : fournisseur.trim();
		String nom = modele == null ? "" : modele.trim();
		String secret = cle == null ? "" : cle.trim();
		if (nom.isEmpty()) {
			throw new BesoinIaException(MODELE_VIDE);
		}
		if (secret.isEmpty()) {
			throw new BesoinIaException(CLE_VIDE);
		}
		if (!fournisseurs.containsKey(maison)) {
			throw new BesoinIaException(SANS_ADAPTATEUR);
		}
		return connexions.enregistrer(maison, nom, secret);
	}

	@Override
	public EssaiConnexion essayer(long callerId, long connexionId) {
		exigerAdmin(callerId);
		Secrete secrete = connexions.trouver(connexionId)
				.orElseThrow(() -> new BesoinIaException(INCONNUE, BesoinIaException.Code.NOT_FOUND));
		FournisseurIaPort fournisseur = fournisseurs.get(secrete.fournisseur());
		if (fournisseur == null) {
			throw new BesoinIaException(SANS_ADAPTATEUR);
		}
		try {
			String texte = fournisseur
					.completer(secrete.modele(), List.of(new MessageIa("user", "ping")), secrete.cle())
					.texte();
			return new EssaiConnexion(true, texte);
		} catch (RuntimeException ex) {
			return new EssaiConnexion(false, ex.getMessage());
		}
	}

	private void exigerAdmin(long callerId) {
		if (!roles.hasAdminRole(callerId)) {
			throw new BesoinIaException(INTERDIT, BesoinIaException.Code.FORBIDDEN);
		}
	}
}

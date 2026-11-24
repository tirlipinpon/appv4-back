package com.appv4.ai.application.port.in;

import java.util.List;

import com.appv4.ai.application.ConnexionModele;
import com.appv4.ai.application.EssaiConnexion;

/**
 * Enregistrer une connexion et l'essayer (BACK-84).
 *
 * <p><b>Business :</b> seul l'admin ajoute un modèle et vérifie qu'il répond.
 * <p><b>Technique :</b> la clé vide ou un fournisseur sans adaptateur n'écrit rien.
 * <p><b>Métier / flow :</b> page IA → ce port → adaptateur. Le cumul des besoins n'est pas touché.
 */
public interface ConnexionModeleUseCase {

	List<ConnexionModele> lister(long callerId);

	ConnexionModele creer(long callerId, String fournisseur, String modele, String cle);

	EssaiConnexion essayer(long callerId, long connexionId);
}

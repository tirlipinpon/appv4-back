package com.appv4.ai.application.port.in;

import java.util.List;

import com.appv4.ai.application.MessageIa;
import com.appv4.ai.application.ReponseIa;

/**
 * Demander une réponse au modèle prévu pour un besoin (BACK-30).
 *
 * <p><b>Business :</b> la fonction métier nomme le besoin. Elle ne nomme pas le fournisseur.
 * <p><b>Technique :</b> un besoin inconnu, inactif ou sans adaptateur n'invente pas de texte.
 * <p><b>Métier / flow :</b> port in du module IA. Aucune route HTTP dans ce ticket.
 */
public interface DemanderBesoinUseCase {

	ReponseIa demander(String codeBesoin, List<MessageIa> messages);
}

package com.appv4.ai.application.port.in;

import java.time.LocalDate;
import java.util.List;

import com.appv4.ai.application.ChangementBesoin;
import com.appv4.ai.application.LectureBesoins;
import com.appv4.ai.application.LectureUsage;
import com.appv4.ai.application.port.out.RouteBesoin;

/**
 * Réglage des besoins par l'admin (BACK-32).
 *
 * <p><b>Business :</b> l'admin voit les besoins, en ajoute, change le modèle, lit la consommation.
 * <p><b>Technique :</b> un refus n'écrit pas d'historique et n'appelle pas le fournisseur.
 * <p><b>Métier / flow :</b> page IA → ce port → routes, cumuls, historique.
 */
public interface ReglerBesoinUseCase {

	LectureBesoins lire(long callerId);

	RouteBesoin creer(long callerId, String code, String nom, String fournisseur, String modele);

	RouteBesoin changer(long callerId, String code, String fournisseur, String modele);

	RouteBesoin activer(long callerId, String code, boolean actif);

	LectureUsage usage(long callerId, LocalDate du, LocalDate au);

	List<ChangementBesoin> historique(long callerId);
}

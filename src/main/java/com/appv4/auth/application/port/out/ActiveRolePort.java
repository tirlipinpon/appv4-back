package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Port sortant — rôles d'espace et dernier choix (SCRUM-124).
 *
 * <p><b>Business :</b> parent et professeur sont les seuls espaces. Le dernier
 * choix doit rester en base, pas dans le navigateur.
 * <p><b>Technique :</b> lit {@code back_user_roles} et {@code back_users.last_active_role}.
 * <p><b>Métier / flow :</b> appelé à chaque émission de JWT et par switch-role.
 * N'invente jamais un rôle qui n'est pas déjà accordé.
 */
public interface ActiveRolePort {

	/**
	 * Rôles d'espace déjà accordés ({@code parent} et/ou {@code teacher}).
	 * Une demande professeur en attente n'apparaît pas ici.
	 */
	List<String> findSpaceRoles(long userId);

	boolean hasPendingTeacherRequest(long userId);

	/** Dernier espace mémorisé, vide si la colonne est null. */
	Optional<String> findLastActiveRole(long userId);

	/**
	 * Écrit le dernier espace. {@code null} efface le souvenir
	 * (deux rôles sans choix valide, ou plus aucun rôle).
	 */
	void saveLastActiveRole(long userId, String role);
}

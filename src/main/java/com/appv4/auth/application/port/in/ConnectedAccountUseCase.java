package com.appv4.auth.application.port.in;

/**
 * Port entrant — nom de la personne déjà connectée.
 *
 * <p><b>Business :</b> l'en-tête doit dire qui est là (parent ou professeur),
 * pas un titre d'application identique pour tout le monde.
 * <p><b>Technique :</b> lit {@code back_users.display_name}. Le JWT ne porte pas ce nom,
 * donc un changement de profil est visible sans se reconnecter.
 * <p><b>Métier / flow :</b> bandeau Angular → GET {@code /api/int/auth/me} → ce port
 * → {@code AuthUserAccountPort}.
 */
public interface ConnectedAccountUseCase {

	/** Nom enregistré du compte. Le même pour l'espace parent et l'espace professeur. */
	String displayName(long userId);

	/**
	 * Nom et avatar pour le bandeau. {@code avatarCode} est null s'il n'y en a pas.
	 */
	ConnectedBanner banner(long userId);

	/**
	 * @param avatarCode code du catalogue, ou null
	 */
	record ConnectedBanner(String displayName, String avatarCode) {
	}
}

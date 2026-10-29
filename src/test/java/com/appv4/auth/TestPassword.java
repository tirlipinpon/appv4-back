package com.appv4.auth;

/**
 * Mot de passe valide partagé par les tests d'auth.
 *
 * <p><b>Business :</b> les scénarios de connexion et d'inscription utilisent le même secret que les comptes locaux.
 * <p><b>Technique :</b> une seule constante, pour ne pas recopier le mot de passe dans chaque test.
 * <p><b>Métier / flow :</b> login, signup et règles de mot de passe s'en servent.
 */
public final class TestPassword {

	public static final String VALID = "Test1!ss";

	private TestPassword() {
	}
}

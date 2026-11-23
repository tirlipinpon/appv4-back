package com.appv4.auth.application.port.out;

/**
 * Lie une transaction OAuth au navigateur qui l'a démarrée (issue #32 / RFC 9700).
 *
 * <p><b>Business :</b> sans {@code state} vérifié, un code obtenu ailleurs peut être
 * injecté dans le callback d'une victime.
 * <p><b>Technique :</b> jeton signé (HMAC) à usage unique, TTL court.
 * <p><b>Métier / flow :</b> authorize-url → {@link #issue} ; login → {@link #consume} ;
 * callback Apple form_post → {@link #peek} puis login → {@link #consume}.
 */
public interface OAuthBrowserBindingPort {

	String PROVIDER_GOOGLE = "google";
	String PROVIDER_FACEBOOK = "facebook";
	String PROVIDER_APPLE = "apple";

	/**
	 * Émet un {@code state} pour ce fournisseur et cette redirect / reprise FE.
	 */
	String issue(String provider, String redirectUri);

	/**
	 * Vérifie signature, TTL, provider, redirectUri et marque le jeton consommé.
	 *
	 * @throws com.appv4.auth.application.exception.AuthDomainException si invalide / rejoué
	 */
	void consume(String provider, String redirectUri, String state);

	/**
	 * Vérifie sans consommer (relais form_post Apple avant l'échange de code).
	 *
	 * @return redirectUri / reprise FE encodée dans le state
	 */
	String peek(String provider, String state);
}

package com.appv4.auth.application.port.out;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Port sortant — créer / signer un JWT.
 *
 * <p><b>Métier :</b> après un login réussi, on donne au front un jeton
 * « prouve que tu es user X ». Le front le renvoie sur chaque appel admin.
 *
 * <p><b>Technique :</b> le domaine ne connaît pas JJWT ; l'impl est
 * {@code JjwtTokenAdapter}. {@code userId} + {@code email} partent en claims.
 */
public interface JwtTokenEncoder {

	JwtTokenResult encode(long userId, String email);
}

package com.appv4.auth.application.port.out;

import java.util.List;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Port sortant — créer / signer un JWT.
 *
 * <p><b>Métier :</b> après un login réussi, on donne au front un jeton
 * « prouve que tu es user X ». Le front le renvoie sur chaque appel admin.
 *
 * <p><b>Technique :</b> le domaine ne connaît pas JJWT ; l'impl est
 * {@code JjwtTokenAdapter}. Claims : {@code sub}, {@code email}, {@code roles}
 * (espaces déjà accordés), et {@code activeRole} seulement quand un espace est choisi.
 */
public interface JwtTokenEncoder {

	/**
	 * @param activeRole {@code parent}, {@code teacher}, ou {@code null} pour ne pas
	 *                   prétendre un espace
	 * @param roles      espaces accordés, pour que le front sache s'il peut changer
	 */
	JwtTokenResult encode(long userId, String email, String activeRole, List<String> roles);
}

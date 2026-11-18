package com.appv4.auth.application.port.out;

import java.util.Optional;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Port sortant — jeton de session de l'app enfant (BACK-13).
 *
 * <p><b>Business :</b> ce badge prouve qu'un enfant est entré avec son prénom et son code.
 * Il ne prouve pas qu'un parent ou un professeur est connecté à l'admin.
 * <p><b>Technique :</b> même signature que le JWT adulte, claim {@code kind=child},
 * sujet {@code child:{id}}. Le décodeur adulte refuse ce jeton.
 * <p><b>Métier / flow :</b> use case login → cet port → adapter JJWT.
 */
public interface ChildSessionTokenPort {

	JwtTokenResult issue(long childId, String firstName);

	Optional<ChildSession> read(String rawToken);

	/** Identité lue dans un jeton enfant encore signé et non expiré. */
	record ChildSession(long childId) {
	}
}

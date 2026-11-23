package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.ChildLoginResult;
import com.appv4.auth.application.result.ChildSessionView;

/**
 * Port entrant — entrer dans l'app enfant, puis relire qui est là (BACK-13).
 *
 * <p><b>Business :</b> prénom + code à 4 chiffres. Un échec ne dit pas si le prénom existe.
 * De 22 h à 6 h, l'entrée est refusée avec la phrase de la Nuit, sans jeton.
 * <p><b>Technique :</b> le jeton émis n'est pas un JWT adulte. L'horloge est celle du bean,
 * lue en Europe/Paris.
 * <p><b>Métier / flow :</b> controller {@code /api/ext/auth} → ce port → phrase de la Nuit,
 * ou hash PIN + jeton enfant.
 */
public interface LoginChildUseCase {

	ChildLoginResult login(String firstName, String pin);

	ChildSessionView session(String rawToken);
}

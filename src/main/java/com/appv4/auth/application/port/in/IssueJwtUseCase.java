package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Émet un JWT pour un utilisateur admin authentifié.
 */
public interface IssueJwtUseCase {

	JwtTokenResult issue(long userId, String email);
}

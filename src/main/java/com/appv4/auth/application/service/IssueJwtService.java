package com.appv4.auth.application.service;

import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.out.JwtTokenEncoder;
import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Service applicatif : orchestre l'émission du JWT.
 *
 * <p>Pour l'instant il délègue juste à l'adaptateur JWT. Plus tard on pourra
 * y ajouter des checks métier (user actif, rôles dans le token, etc.) sans
 * toucher au controller HTTP.
 */
public class IssueJwtService implements IssueJwtUseCase {

	private final JwtTokenEncoder jwtTokenEncoder;

	public IssueJwtService(JwtTokenEncoder jwtTokenEncoder) {
		this.jwtTokenEncoder = jwtTokenEncoder;
	}

	@Override
	public JwtTokenResult issue(long userId, String email) {
		return jwtTokenEncoder.encode(userId, email);
	}
}

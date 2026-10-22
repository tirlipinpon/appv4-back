package com.appv4.auth.application.service;

import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.out.JwtTokenEncoder;
import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Service applicatif — émission JWT.
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

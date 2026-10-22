package com.appv4.auth.application.port.out;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Port sortant — encodage / signature JWT.
 */
public interface JwtTokenEncoder {

	JwtTokenResult encode(long userId, String email);
}

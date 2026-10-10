package com.appv4.hello.application.command;

import java.util.Objects;

import com.appv4.hello.domain.model.HelloAudience;

/**
 * Entrée du cas d'usage {@code GetHelloMessage}.
 */
public record GetHelloMessageCommand(HelloAudience audience) {

	public GetHelloMessageCommand {
		Objects.requireNonNull(audience, "audience");
	}
}

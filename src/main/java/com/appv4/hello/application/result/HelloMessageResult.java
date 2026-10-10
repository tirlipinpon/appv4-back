package com.appv4.hello.application.result;

import java.util.Objects;

/**
 * Résultat applicatif du cas d'usage {@code GetHelloMessage}.
 */
public record HelloMessageResult(String text) {

	public HelloMessageResult {
		Objects.requireNonNull(text, "text");
	}
}

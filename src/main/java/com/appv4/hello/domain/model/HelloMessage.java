package com.appv4.hello.domain.model;

import com.appv4.shared.domain.DomainException;
import com.appv4.shared.domain.ErrorCode;

/**
 * Message hello du MVP (ex. {@code "hello learning"}, {@code "admin-helloworld"}).
 * Value object : pas d'identité, juste le texte métier.
 */
public record HelloMessage(String text) {

	public HelloMessage {
		if (text == null || text.isBlank()) {
			throw new DomainException(ErrorCode.HELLO_MESSAGE_TEXT_REQUIRED, "Hello message text is required");
		}
	}
}

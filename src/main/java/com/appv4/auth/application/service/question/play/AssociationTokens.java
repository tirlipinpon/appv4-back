package com.appv4.auth.application.service.question.play;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Identifiants distincts pour chaque face ou option affichée. */
final class AssociationTokens {
	private AssociationTokens() {
	}

	static UUID forSide(String form, long id, boolean first) {
		return UUID.nameUUIDFromBytes((form + ":" + id + ":" + first).getBytes(StandardCharsets.UTF_8));
	}
}

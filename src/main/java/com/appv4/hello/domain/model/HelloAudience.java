package com.appv4.hello.domain.model;

/**
 * Qui consomme le message hello.
 * LEARNING → table {@code front_*} / API {@code /api/ext/**}
 * ADMIN → table {@code back_*} / API {@code /api/int/**}
 */
public enum HelloAudience {
	LEARNING,
	ADMIN
}

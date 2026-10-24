package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compteur en mémoire : au plus {@code maxRequests} horodatages par clé dans la fenêtre.
 *
 * <p><b>Métier / flow :</b> appelé par {@link AuthRateLimitFilter} avant login / forgot /
 * OAuth. Thread-safe via verrou par clé (deque synchronisée sur l'entrée map).
 */
public class InMemoryAuthRateLimiter {

	private final int maxRequests;
	private final long windowMillis;
	private final Map<String, Deque<Long>> hitsByKey = new ConcurrentHashMap<>();

	public InMemoryAuthRateLimiter(int maxRequests, int windowSeconds) {
		if (maxRequests < 1) {
			throw new IllegalArgumentException("maxRequests must be >= 1");
		}
		if (windowSeconds < 1) {
			throw new IllegalArgumentException("windowSeconds must be >= 1");
		}
		this.maxRequests = maxRequests;
		this.windowMillis = windowSeconds * 1000L;
	}

	/**
	 * @return {@code true} si la requête est acceptée et comptée ; {@code false} si plafond atteint
	 */
	public boolean tryAcquire(String key, long nowEpochMs) {
		Deque<Long> hits = hitsByKey.computeIfAbsent(key, k -> new ArrayDeque<>());
		synchronized (hits) {
			long floor = nowEpochMs - windowMillis;
			while (!hits.isEmpty() && hits.peekFirst() < floor) {
				hits.removeFirst();
			}
			if (hits.size() >= maxRequests) {
				return false;
			}
			hits.addLast(nowEpochMs);
			return true;
		}
	}
}

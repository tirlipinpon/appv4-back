package com.appv4.auth.application.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Exécute une action après le commit de la transaction courante.
 *
 * <p><b>Business :</b> un email ne doit pas annoncer une opération encore annulable
 * (lien reset, approbation professeur).
 * <p><b>Technique :</b> {@link TransactionSynchronization#afterCommit()} ; hors transaction
 * (tests unitaires sans TX) → exécution immédiate.
 * <p><b>Métier / flow :</b> services application après écriture port out → notification.
 */
final class AfterCommit {

	private AfterCommit() {
	}

	static void run(Runnable action) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					action.run();
				}
			});
			return;
		}
		action.run();
	}
}

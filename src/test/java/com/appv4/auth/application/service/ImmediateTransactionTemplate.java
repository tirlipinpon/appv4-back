package com.appv4.auth.application.service;

import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Exécute le callback sans vraie transaction — pour les tests unitaires OAuth (issue #39).
 */
final class ImmediateTransactionTemplate extends TransactionTemplate {

	@Override
	public <T> T execute(TransactionCallback<T> action) throws TransactionException {
		return action.doInTransaction(new SimpleTransactionStatus());
	}
}

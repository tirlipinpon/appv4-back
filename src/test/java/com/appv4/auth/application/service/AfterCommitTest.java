package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class AfterCommitTest {

	@Test
	void withoutTransactionRunsImmediately() {
		AtomicBoolean ran = new AtomicBoolean(false);
		AfterCommit.run(() -> ran.set(true));
		assertThat(ran).isTrue();
	}

	@Test
	void withActiveSynchronizationDefersUntilAfterCommit() {
		TransactionSynchronizationManager.initSynchronization();
		try {
			List<String> order = new ArrayList<>();
			AfterCommit.run(() -> order.add("mail"));
			order.add("before-commit");
			assertThat(order).containsExactly("before-commit");

			for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
				sync.afterCommit();
			}
			assertThat(order).containsExactly("before-commit", "mail");
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}
}

package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/** Bounded cleanup of reset tokens after their retention period. */
@Configuration
@EnableScheduling
@Profile("!test & !persistence-test")
public class PasswordResetTokenCleanup {
	private static final Logger LOG = LoggerFactory.getLogger(PasswordResetTokenCleanup.class);
	private final BackPasswordResetTokenJpaRepository tokens;
	private final MeterRegistry meters;
	private final int retentionDays;
	private final int batchSize;

	public PasswordResetTokenCleanup(BackPasswordResetTokenJpaRepository tokens, MeterRegistry meters,
			@Value("${appv4.auth.password-reset.cleanup.retention-days:30}") int retentionDays,
			@Value("${appv4.auth.password-reset.cleanup.batch-size:500}") int batchSize) {
		if (retentionDays < 1 || batchSize < 1 || batchSize > 1000) {
			throw new IllegalArgumentException("Invalid password reset cleanup configuration");
		}
		this.tokens = tokens;
		this.meters = meters;
		this.retentionDays = retentionDays;
		this.batchSize = batchSize;
	}

	@Scheduled(cron = "${appv4.auth.password-reset.cleanup.cron:0 0 3 * * *}", zone = "UTC")
	@Transactional
	public void cleanup() {
		Timer.Sample sample = Timer.start(meters);
		try {
			int deleted = tokens.deleteExpiredBatch(Instant.now().minus(retentionDays, ChronoUnit.DAYS), batchSize);
			meters.counter("appv4.password_reset.cleanup.deleted").increment(deleted);
			LOG.info("Password reset cleanup deleted {} expired tokens", deleted);
		} catch (RuntimeException ex) {
			meters.counter("appv4.password_reset.cleanup.errors").increment();
			throw ex;
		} finally {
			sample.stop(meters.timer("appv4.password_reset.cleanup.duration"));
		}
	}
}

package com.appv4.auth.infrastructure.adapter.out.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appv4.auth.application.port.out.TeacherApprovedMailPort;

/**
 * Approbation professeur hors local sans SMTP (issue #58).
 */
public class NoOpTeacherApprovedMailAdapter implements TeacherApprovedMailPort {

	private static final Logger LOG = LoggerFactory.getLogger(NoOpTeacherApprovedMailAdapter.class);

	@Override
	public void sendTeacherApproved(String email) {
		LOG.info("Teacher approved notification skipped (mail transport=noop)");
	}
}

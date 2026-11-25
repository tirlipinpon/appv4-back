package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.QuestionTextPort;

/**
 * Lit le texte actuel des questions.
 *
 * <p><b>Business :</b> une question supprimée n'a plus de texte à montrer.
 * <p><b>Technique :</b> {@link BackSubjectQuestionJpaRepository#findAllById}. Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out de {@link QuestionTextPort}, pour le suivi parent.
 */
@Component
@Profile("!test")
public class QuestionTextPersistenceAdapter implements QuestionTextPort {

	private final BackSubjectQuestionJpaRepository questions;

	public QuestionTextPersistenceAdapter(BackSubjectQuestionJpaRepository questions) {
		this.questions = questions;
	}

	@Override
	public Map<Long, String> promptsOf(Collection<Long> questionIds) {
		if (questionIds == null || questionIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, String> prompts = new HashMap<>();
		for (BackSubjectQuestionEntity question : questions.findAllById(questionIds)) {
			prompts.put(question.getId(), question.getPrompt());
		}
		return prompts;
	}
}

package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.TeacherQuestionPort;

/** Questions communes et contenus délégués aux stores de chaque forme. */
@Component
@Profile("!test")
public class TeacherQuestionPersistenceAdapter implements TeacherQuestionPort {

	private static final int CONTENT_BATCH_SIZE = 500;

	private final BackSubjectQuestionJpaRepository questions;
	private final BackSubjectQuestionFormJpaRepository forms;
	private final Map<String, QuestionContentStore> stores;

	public TeacherQuestionPersistenceAdapter(
			BackSubjectQuestionJpaRepository questions,
			BackSubjectQuestionFormJpaRepository forms,
			List<QuestionContentStore> stores) {
		this.questions = questions;
		this.forms = forms;
		// Un doublon est une erreur de câblage : échec au démarrage, pas de choix implicite.
		this.stores = stores.stream().collect(Collectors.toUnmodifiableMap(
				QuestionContentStore::formCode, Function.identity()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredQuestion> findBySubject(long subjectId) {
		List<BackSubjectQuestionEntity> rows = questions.findBySubjectIdOrderByIdAsc(subjectId);
		Map<String, List<BackSubjectQuestionEntity>> byForm = rows.stream()
				.collect(Collectors.groupingBy(BackSubjectQuestionEntity::getFormCode));
		Map<Long, StoredContent> contents = new HashMap<>();
		byForm.forEach((formCode, group) -> {
			QuestionContentStore store = store(formCode);
			List<Long> ids = group.stream().map(BackSubjectQuestionEntity::getId).toList();
			for (int start = 0; start < ids.size(); start += CONTENT_BATCH_SIZE) {
				contents.putAll(store.load(ids.subList(start, Math.min(start + CONTENT_BATCH_SIZE, ids.size()))));
			}
		});
		return rows.stream().map(row -> stored(row, contents.get(row.getId()))).toList();
	}

	@Override
	@Transactional
	public StoredQuestion insert(long subjectId, String formCode, String prompt, DraftContent content) {
		QuestionContentStore store = store(formCode);
		BackSubjectQuestionEntity saved = questions.save(new BackSubjectQuestionEntity(subjectId, formCode, prompt));
		return stored(saved, store.write(saved.getId(), content));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredQuestion> findInSubject(long subjectId, long questionId) {
		return questions.findByIdAndSubjectId(questionId, subjectId)
				.map(row -> stored(row, store(row.getFormCode()).load(List.of(row.getId())).get(row.getId())));
	}

	@Override
	@Transactional
	public StoredQuestion replace(long questionId, String prompt, DraftContent content) {
		// Sérialise les corrections concurrentes pour ne pas mélanger deux contenus.
		BackSubjectQuestionEntity question = questions.findForUpdate(questionId).orElseThrow();
		QuestionContentStore store = store(question.getFormCode());
		question.setPrompt(prompt);
		store.delete(questionId);
		return stored(question, store.write(questionId, content));
	}

	@Override
	@Transactional
	public void ensureForm(long subjectId, String formCode, String label, int sortOrder) {
		forms.insertIfAbsent(subjectId, formCode, label, sortOrder);
	}

	private QuestionContentStore store(String formCode) {
		QuestionContentStore store = stores.get(formCode);
		if (store == null) {
			throw new IllegalStateException("Aucun stockage pour la forme : " + formCode);
		}
		return store;
	}

	private static StoredQuestion stored(BackSubjectQuestionEntity question, StoredContent content) {
		if (content == null) {
			throw new IllegalStateException("Contenu non chargé pour la question : " + question.getId());
		}
		return new StoredQuestion(question.getId(), question.getSubjectId(), question.getFormCode(), question.getPrompt(), content);
	}
}

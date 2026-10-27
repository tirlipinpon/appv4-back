package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ChoiceDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PairDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoice;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPair;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

/**
 * Persiste les choix multiples d'une matière (SCRUM-174).
 *
 * <p><b>Business :</b> la première question d'une forme note cette forme sur la
 * matière. Les suivantes réutilisent la même ligne. Retirer la matière retire
 * les énoncés (clé étrangère).
 * <p><b>Technique :</b> absent du profil {@code test} : les tests web ne démarrent pas JPA.
 * <p><b>Métier / flow :</b> adapter out du port {@link TeacherQuestionPort}.
 */
@Component
@Profile("!test")
public class TeacherQuestionPersistenceAdapter implements TeacherQuestionPort {

	private final BackSubjectQuestionJpaRepository questions;
	private final BackSubjectQuestionChoiceJpaRepository choices;
	private final BackSubjectQuestionPairJpaRepository pairs;
	private final BackSubjectQuestionFormJpaRepository forms;

	public TeacherQuestionPersistenceAdapter(
			BackSubjectQuestionJpaRepository questions,
			BackSubjectQuestionChoiceJpaRepository choices,
			BackSubjectQuestionPairJpaRepository pairs,
			BackSubjectQuestionFormJpaRepository forms) {
		this.questions = questions;
		this.choices = choices;
		this.pairs = pairs;
		this.forms = forms;
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredQuestion> findBySubject(long subjectId) {
		List<StoredQuestion> rows = new ArrayList<>();
		for (BackSubjectQuestionEntity question : questions.findBySubjectIdOrderByIdAsc(subjectId)) {
			rows.add(toStored(question));
		}
		return List.copyOf(rows);
	}

	@Override
	@Transactional
	public StoredQuestion insert(
			long subjectId,
			String formCode,
			String prompt,
			List<ChoiceDraft> choiceDrafts,
			List<PairDraft> pairDrafts) {
		BackSubjectQuestionEntity saved = questions.saveAndFlush(
				new BackSubjectQuestionEntity(subjectId, formCode, prompt));
		return new StoredQuestion(
				saved.getId(),
				saved.getSubjectId(),
				saved.getFormCode(),
				saved.getPrompt(),
				writeChoices(saved.getId(), choiceDrafts),
				writePairs(saved.getId(), pairDrafts));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredQuestion> findInSubject(long subjectId, long questionId) {
		return questions.findById(questionId)
				.filter(question -> question.getSubjectId() == subjectId)
				.map(this::toStored);
	}

	@Override
	@Transactional
	public StoredQuestion replace(
			long questionId, String prompt, List<ChoiceDraft> choiceDrafts, List<PairDraft> pairDrafts) {
		BackSubjectQuestionEntity question = questions.findById(questionId).orElseThrow();
		question.setPrompt(prompt);
		questions.saveAndFlush(question);
		choices.deleteByQuestionId(questionId);
		choices.flush();
		pairs.deleteByQuestionId(questionId);
		pairs.flush();
		return new StoredQuestion(
				question.getId(),
				question.getSubjectId(),
				question.getFormCode(),
				question.getPrompt(),
				writeChoices(questionId, choiceDrafts),
				writePairs(questionId, pairDrafts));
	}

	@Override
	@Transactional
	public void ensureForm(long subjectId, String formCode, String label, int sortOrder) {
		if (forms.findBySubjectIdAndFormCode(subjectId, formCode).isPresent()) {
			return;
		}
		try {
			forms.saveAndFlush(new BackSubjectQuestionFormEntity(subjectId, formCode, label, sortOrder));
		} catch (DataIntegrityViolationException ex) {
			if (forms.findBySubjectIdAndFormCode(subjectId, formCode).isEmpty()) {
				throw ex;
			}
		}
	}

	private List<StoredChoice> writeChoices(long questionId, List<ChoiceDraft> drafts) {
		List<StoredChoice> storedChoices = new ArrayList<>();
		int order = 0;
		for (ChoiceDraft draft : drafts) {
			BackSubjectQuestionChoiceEntity choice = choices.saveAndFlush(
					new BackSubjectQuestionChoiceEntity(questionId, draft.label(), draft.correct(), order));
			storedChoices.add(new StoredChoice(choice.getId(), choice.getLabel(), choice.isCorrect()));
			order++;
		}
		return List.copyOf(storedChoices);
	}

	private List<StoredPair> writePairs(long questionId, List<PairDraft> drafts) {
		List<StoredPair> storedPairs = new ArrayList<>();
		if (drafts == null) {
			return List.of();
		}
		int order = 0;
		for (PairDraft draft : drafts) {
			BackSubjectQuestionPairEntity pair = pairs.saveAndFlush(new BackSubjectQuestionPairEntity(
					questionId, draft.questionSide(), draft.answerSide(), order));
			storedPairs.add(new StoredPair(pair.getId(), pair.getQuestionSide(), pair.getAnswerSide()));
			order++;
		}
		return List.copyOf(storedPairs);
	}

	private StoredQuestion toStored(BackSubjectQuestionEntity question) {
		List<StoredChoice> storedChoices = new ArrayList<>();
		for (BackSubjectQuestionChoiceEntity choice : choices.findByQuestionIdOrderBySortOrderAsc(question.getId())) {
			storedChoices.add(new StoredChoice(choice.getId(), choice.getLabel(), choice.isCorrect()));
		}
		return new StoredQuestion(
				question.getId(),
				question.getSubjectId(),
				question.getFormCode(),
				question.getPrompt(),
				List.copyOf(storedChoices),
				storedPairs(question.getId()));
	}

	private List<StoredPair> storedPairs(long questionId) {
		List<StoredPair> storedPairs = new ArrayList<>();
		for (BackSubjectQuestionPairEntity pair : pairs.findByQuestionIdOrderBySortOrderAsc(questionId)) {
			storedPairs.add(new StoredPair(pair.getId(), pair.getQuestionSide(), pair.getAnswerSide()));
		}
		return List.copyOf(storedPairs);
	}
}

package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.SimonDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.SimonElementDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredSimon;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredSimonElement;
import com.appv4.auth.application.service.TeacherQuestionService;

/**
 * Stockage du Simon (BACK-54) : genre, éléments, puis suite.
 *
 * <p><b>Business :</b> le professeur retrouve exactement ses éléments et sa suite ; l'élève
 * rejoue cette suite-là.
 * <p><b>Technique :</b> lecture groupée sur {@code back_subject_question_simon_options},
 * {@code _simon_elements} et {@code _simon_steps} (trois requêtes par lot). Un pas range la
 * position de son élément ; la base refuse une position qui n'existe pas dans la question.
 * <p><b>Métier / flow :</b> le port des questions délègue ici dès que le code est {@code simon}.
 */
@Component
@Profile("!test")
public class SimonQuestionContentStore implements QuestionContentStore {

	private final BackSubjectQuestionSimonOptionsJpaRepository options;
	private final BackSubjectQuestionSimonElementJpaRepository elements;
	private final BackSubjectQuestionSimonStepJpaRepository steps;

	public SimonQuestionContentStore(
			BackSubjectQuestionSimonOptionsJpaRepository options,
			BackSubjectQuestionSimonElementJpaRepository elements,
			BackSubjectQuestionSimonStepJpaRepository steps) {
		this.options = options;
		this.elements = elements;
		this.steps = steps;
	}

	@Override
	public String formCode() {
		return TeacherQuestionService.SIMON;
	}

	@Override
	public Map<Long, StoredContent> load(List<Long> questionIds) {
		Map<Long, String> typeById = new HashMap<>();
		for (BackSubjectQuestionSimonOptionsEntity row : options.findByQuestionIdIn(questionIds)) {
			typeById.put(row.getQuestionId(), row.getElementType());
		}
		Map<Long, List<StoredSimonElement>> elementsById = new HashMap<>();
		Map<Long, List<Integer>> sequenceById = new HashMap<>();
		for (long id : questionIds) {
			elementsById.put(id, new ArrayList<>());
			sequenceById.put(id, new ArrayList<>());
		}
		for (BackSubjectQuestionSimonElementEntity row : elements
				.findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(questionIds)) {
			elementsById.get(row.getQuestionId())
					.add(new StoredSimonElement(row.getId(), row.getLabel(), row.getColor()));
		}
		for (BackSubjectQuestionSimonStepEntity row : steps
				.findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(questionIds)) {
			sequenceById.get(row.getQuestionId()).add(row.getElementIndex());
		}
		Map<Long, StoredContent> contents = new HashMap<>();
		for (long id : questionIds) {
			contents.put(id, new StoredSimon(typeById.get(id), elementsById.get(id), sequenceById.get(id)));
		}
		return contents;
	}

	@Override
	public StoredContent write(long questionId, DraftContent content) {
		if (!(content instanceof SimonDraft draft)) {
			throw new IllegalArgumentException("Contenu incompatible avec le Simon");
		}
		BackSubjectQuestionSimonOptionsEntity saved = options.saveAndFlush(
				new BackSubjectQuestionSimonOptionsEntity(questionId, draft.elementType()));
		List<BackSubjectQuestionSimonElementEntity> elementRows = new ArrayList<>();
		for (SimonElementDraft element : draft.elements()) {
			elementRows.add(new BackSubjectQuestionSimonElementEntity(
					questionId, element.label(), element.color(), elementRows.size()));
		}
		List<StoredSimonElement> storedElements = elements.saveAllAndFlush(elementRows).stream()
				.map(row -> new StoredSimonElement(row.getId(), row.getLabel(), row.getColor()))
				.toList();
		List<BackSubjectQuestionSimonStepEntity> stepRows = new ArrayList<>();
		for (int elementIndex : draft.sequence()) {
			stepRows.add(new BackSubjectQuestionSimonStepEntity(questionId, elementIndex, stepRows.size()));
		}
		List<Integer> sequence = steps.saveAllAndFlush(stepRows).stream()
				.map(BackSubjectQuestionSimonStepEntity::getElementIndex)
				.toList();
		return new StoredSimon(saved.getElementType(), storedElements, sequence);
	}

	/**
	 * Les pas partent avant leurs éléments. Le genre est retiré comme entité puis vidé
	 * en base, pour que la correction puisse réécrire le même {@code question_id}.
	 */
	@Override
	public void delete(long questionId) {
		steps.deleteByQuestionId(questionId);
		elements.deleteByQuestionId(questionId);
		options.findById(questionId).ifPresent(options::delete);
		options.flush();
	}
}

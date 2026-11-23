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
 * Stockage du Simon (BACK-54, BACK-55) : genre et éléments. Pas de suite.
 *
 * <p><b>Business :</b> le professeur retrouve ses éléments. La suite est tirée à la partie.
 * <p><b>Technique :</b> lecture groupée sur {@code back_subject_question_simon_options} et
 * {@code _simon_elements}.
 * <p><b>Métier / flow :</b> le port des questions délègue ici dès que le code est {@code simon}.
 */
@Component
@Profile("!test")
public class SimonQuestionContentStore implements QuestionContentStore {

	private final BackSubjectQuestionSimonOptionsJpaRepository options;
	private final BackSubjectQuestionSimonElementJpaRepository elements;

	public SimonQuestionContentStore(
			BackSubjectQuestionSimonOptionsJpaRepository options,
			BackSubjectQuestionSimonElementJpaRepository elements) {
		this.options = options;
		this.elements = elements;
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
		for (long id : questionIds) {
			elementsById.put(id, new ArrayList<>());
		}
		for (BackSubjectQuestionSimonElementEntity row : elements
				.findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(questionIds)) {
			elementsById.get(row.getQuestionId())
					.add(new StoredSimonElement(row.getId(), row.getLabel(), row.getColor(), row.getImageWebp()));
		}
		Map<Long, StoredContent> contents = new HashMap<>();
		for (long id : questionIds) {
			contents.put(id, new StoredSimon(typeById.get(id), elementsById.get(id)));
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
					questionId, element.label(), element.color(), elementRows.size(), element.imageWebp()));
		}
		List<StoredSimonElement> storedElements = elements.saveAllAndFlush(elementRows).stream()
				.map(row -> new StoredSimonElement(row.getId(), row.getLabel(), row.getColor(), row.getImageWebp()))
				.toList();
		return new StoredSimon(saved.getElementType(), storedElements);
	}

	/**
	 * Les éléments partent avant le genre. Le genre est retiré comme entité puis vidé
	 * en base, pour que la correction puisse réécrire le même {@code question_id}.
	 */
	@Override
	public void delete(long questionId) {
		elements.deleteByQuestionId(questionId);
		options.findById(questionId).ifPresent(options::delete);
		options.flush();
	}
}

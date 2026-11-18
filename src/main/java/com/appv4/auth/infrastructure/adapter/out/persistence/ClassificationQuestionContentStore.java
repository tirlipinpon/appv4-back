package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.TeacherQuestionPort.ClassificationDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredCard;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClassification;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredColumn;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.service.TeacherQuestionService;

/**
 * Stockage de la structure classement. Colonnes d'abord, puis cartes.
 *
 * <p><b>Business :</b> une lecture groupée, indépendante des autres formes.
 * <p><b>Technique :</b> {@code back_subject_question_columns} + {@code _cards}.
 * <p><b>Métier / flow :</b> {@link TeacherQuestionPersistenceAdapter} délègue ici.
 */
@Component
@Profile("!test")
public class ClassificationQuestionContentStore implements QuestionContentStore {

	private final BackSubjectQuestionColumnJpaRepository columns;
	private final BackSubjectQuestionCardJpaRepository cards;

	public ClassificationQuestionContentStore(
			BackSubjectQuestionColumnJpaRepository columns,
			BackSubjectQuestionCardJpaRepository cards) {
		this.columns = columns;
		this.cards = cards;
	}

	@Override
	public String formCode() {
		return TeacherQuestionService.CLASSIFICATION;
	}

	@Override
	public Map<Long, StoredContent> load(List<Long> questionIds) {
		Map<Long, List<StoredColumn>> columnsByQuestion = new HashMap<>();
		Map<Long, List<StoredCard>> cardsByQuestion = new HashMap<>();
		for (long id : questionIds) {
			columnsByQuestion.put(id, new ArrayList<>());
			cardsByQuestion.put(id, new ArrayList<>());
		}
		for (BackSubjectQuestionColumnEntity row : columns
				.findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(questionIds)) {
			columnsByQuestion.get(row.getQuestionId())
					.add(new StoredColumn(row.getId(), row.getLabel()));
		}
		for (BackSubjectQuestionCardEntity row : cards
				.findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(questionIds)) {
			cardsByQuestion.get(row.getQuestionId())
					.add(new StoredCard(row.getId(), row.getLabel(), row.getCorrectColumnId()));
		}
		Map<Long, StoredContent> contents = new HashMap<>();
		for (long id : questionIds) {
			contents.put(id, new StoredClassification(columnsByQuestion.get(id), cardsByQuestion.get(id)));
		}
		return contents;
	}

	@Override
	public StoredContent write(long questionId, DraftContent content) {
		if (!(content instanceof ClassificationDraft draft)) {
			throw new IllegalArgumentException("Contenu incompatible avec la forme classement");
		}
		List<BackSubjectQuestionColumnEntity> columnRows = new ArrayList<>();
		for (var column : draft.columns()) {
			columnRows.add(new BackSubjectQuestionColumnEntity(questionId, column.label(), columnRows.size()));
		}
		List<BackSubjectQuestionColumnEntity> savedColumns = columns.saveAllAndFlush(columnRows);
		List<Long> columnIds = savedColumns.stream().map(BackSubjectQuestionColumnEntity::getId).toList();
		List<BackSubjectQuestionCardEntity> cardRows = new ArrayList<>();
		for (var card : draft.cards()) {
			cardRows.add(new BackSubjectQuestionCardEntity(
					questionId, card.label(), columnIds.get(card.columnIndex()), cardRows.size()));
		}
		List<BackSubjectQuestionCardEntity> savedCards = cards.saveAllAndFlush(cardRows);
		return new StoredClassification(
				savedColumns.stream().map(row -> new StoredColumn(row.getId(), row.getLabel())).toList(),
				savedCards.stream()
						.map(row -> new StoredCard(row.getId(), row.getLabel(), row.getCorrectColumnId()))
						.toList());
	}

	@Override
	public void delete(long questionId) {
		cards.deleteByQuestionId(questionId);
		columns.deleteByQuestionId(questionId);
	}
}

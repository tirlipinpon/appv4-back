package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ItemsDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredItem;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredItems;

/**
 * Stockage de la structure order (SCRUM-176, BACK-46).
 *
 * <p><b>Business :</b> items dans l'ordre juste ; optionnellement une frise avec jalons.
 * <p><b>Technique :</b> lecture groupée items + options 1:1.
 * <p><b>Métier / flow :</b> une lecture groupée, indépendante des autres formes.
 */
@Component
@Profile("!test")
public class ItemQuestionContentStore implements QuestionContentStore {

	private final BackSubjectQuestionItemJpaRepository repository;
	private final BackSubjectQuestionOrderOptionsJpaRepository options;

	public ItemQuestionContentStore(
			BackSubjectQuestionItemJpaRepository repository,
			BackSubjectQuestionOrderOptionsJpaRepository options) {
		this.repository = repository;
		this.options = options;
	}

	@Override
	public String formCode() {
		return "order";
	}

	@Override
	public Map<Long, StoredContent> load(List<Long> questionIds) {
		Map<Long, List<StoredItem>> grouped = new HashMap<>();
		for (long id : questionIds) {
			grouped.put(id, new ArrayList<>());
		}
		Set<Long> timelineOn = new HashSet<>();
		for (var row : repository.findItemsWithOptions(questionIds)) {
			if (row.getItemId() != null) {
				grouped.get(row.getQuestionId()).add(new StoredItem(row.getItemId(), row.getLabel(), row.getAxisLabel()));
			}
			if (row.getTimelineEnabled()) {
				timelineOn.add(row.getQuestionId());
			}
		}
		Map<Long, StoredContent> contents = new HashMap<>();
		grouped.forEach((id, rows) -> contents.put(id, new StoredItems(rows, timelineOn.contains(id))));
		return contents;
	}

	@Override
	public StoredContent write(long questionId, DraftContent content) {
		if (!(content instanceof ItemsDraft draft)) {
			throw new IllegalArgumentException("Contenu incompatible avec la forme order");
		}
		options.saveAndFlush(new BackSubjectQuestionOrderOptionsEntity(questionId, draft.timelineEnabled()));
		List<BackSubjectQuestionItemEntity> rows = new ArrayList<>();
		for (var row : draft.items()) {
			rows.add(new BackSubjectQuestionItemEntity(
					questionId, row.label(), row.axisLabel(), rows.size()));
		}
		return new StoredItems(
				repository.saveAllAndFlush(rows).stream()
						.map(row -> new StoredItem(row.getId(), row.getLabel(), row.getAxisLabel()))
						.toList(),
				draft.timelineEnabled());
	}

	@Override
	public void delete(long questionId) {
		options.deleteByQuestionId(questionId);
		repository.deleteByQuestionId(questionId);
	}
}

package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ItemsDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredItem;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredItems;

/** Stockage de la structure order. Une lecture groupée, indépendante des autres formes. */
@Component
@Profile("!test")
public class ItemQuestionContentStore implements QuestionContentStore {

	private final BackSubjectQuestionItemJpaRepository repository;

	public ItemQuestionContentStore(BackSubjectQuestionItemJpaRepository repository) {
		this.repository = repository;
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
		for (BackSubjectQuestionItemEntity row : repository.findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(questionIds)) {
			grouped.get(row.getQuestionId()).add(new StoredItem(row.getId(), row.getLabel()));
		}
		Map<Long, StoredContent> contents = new HashMap<>();
		grouped.forEach((id, rows) -> contents.put(id, new StoredItems(rows)));
		return contents;
	}

	@Override
	public StoredContent write(long questionId, DraftContent content) {
		if (!(content instanceof ItemsDraft draft)) {
			throw new IllegalArgumentException("Contenu incompatible avec la forme order");
		}
		List<BackSubjectQuestionItemEntity> rows = new ArrayList<>();
		for (var row : draft.items()) {
			rows.add(new BackSubjectQuestionItemEntity(questionId, row.label(), rows.size()));
		}
		return new StoredItems(repository.saveAllAndFlush(rows).stream()
				.map(row -> new StoredItem(row.getId(), row.getLabel())).toList());
	}

	@Override
	public void delete(long questionId) {
		repository.deleteByQuestionId(questionId);
	}
}

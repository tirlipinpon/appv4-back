package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ChoicesDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoice;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoices;

/** Stockage de la structure qcm. Une lecture groupée, indépendante des autres formes. */
@Component
@Profile("!test")
public class ChoiceQuestionContentStore implements QuestionContentStore {

	private final BackSubjectQuestionChoiceJpaRepository repository;

	public ChoiceQuestionContentStore(BackSubjectQuestionChoiceJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public String formCode() {
		return "qcm";
	}

	@Override
	public Map<Long, StoredContent> load(List<Long> questionIds) {
		Map<Long, List<StoredChoice>> grouped = new HashMap<>();
		for (long id : questionIds) {
			grouped.put(id, new ArrayList<>());
		}
		for (BackSubjectQuestionChoiceEntity row : repository.findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(questionIds)) {
			grouped.get(row.getQuestionId()).add(new StoredChoice(row.getId(), row.getLabel(), row.isCorrect()));
		}
		Map<Long, StoredContent> contents = new HashMap<>();
		grouped.forEach((id, rows) -> contents.put(id, new StoredChoices(rows)));
		return contents;
	}

	@Override
	public StoredContent write(long questionId, DraftContent content) {
		if (!(content instanceof ChoicesDraft draft)) {
			throw new IllegalArgumentException("Contenu incompatible avec la forme qcm");
		}
		List<BackSubjectQuestionChoiceEntity> rows = new ArrayList<>();
		for (var row : draft.choices()) {
			rows.add(new BackSubjectQuestionChoiceEntity(questionId, row.label(), row.correct(), rows.size()));
		}
		return new StoredChoices(repository.saveAllAndFlush(rows).stream()
				.map(row -> new StoredChoice(row.getId(), row.getLabel(), row.isCorrect())).toList());
	}

	@Override
	public void delete(long questionId) {
		repository.deleteByQuestionId(questionId);
	}
}

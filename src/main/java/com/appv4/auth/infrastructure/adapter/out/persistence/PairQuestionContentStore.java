package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PairsDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPair;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPairs;

/** Stockage de la structure memory. Une lecture groupée, indépendante des autres formes. */
@Component
@Profile("!test")
public class PairQuestionContentStore implements QuestionContentStore {

	private final BackSubjectQuestionPairJpaRepository repository;

	public PairQuestionContentStore(BackSubjectQuestionPairJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public String formCode() {
		return "memory";
	}

	@Override
	public Map<Long, StoredContent> load(List<Long> questionIds) {
		Map<Long, List<StoredPair>> grouped = new HashMap<>();
		for (long id : questionIds) {
			grouped.put(id, new ArrayList<>());
		}
		for (BackSubjectQuestionPairEntity row : repository.findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(questionIds)) {
			grouped.get(row.getQuestionId()).add(new StoredPair(row.getId(), row.getQuestionSide(), row.getAnswerSide()));
		}
		Map<Long, StoredContent> contents = new HashMap<>();
		grouped.forEach((id, rows) -> contents.put(id, new StoredPairs(rows)));
		return contents;
	}

	@Override
	public StoredContent write(long questionId, DraftContent content) {
		if (!(content instanceof PairsDraft draft)) {
			throw new IllegalArgumentException("Contenu incompatible avec la forme memory");
		}
		List<BackSubjectQuestionPairEntity> rows = new ArrayList<>();
		for (var row : draft.pairs()) {
			rows.add(new BackSubjectQuestionPairEntity(questionId, row.questionSide(), row.answerSide(), rows.size()));
		}
		return new StoredPairs(repository.saveAllAndFlush(rows).stream()
				.map(row -> new StoredPair(row.getId(), row.getQuestionSide(), row.getAnswerSide())).toList());
	}

	@Override
	public void delete(long questionId) {
		repository.deleteByQuestionId(questionId);
	}
}

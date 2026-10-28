package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StatementsDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredStatement;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredStatements;

/** Stockage de la structure vrai_faux. Une lecture groupée, indépendante des autres formes. */
@Component
@Profile("!test")
public class StatementQuestionContentStore implements QuestionContentStore {

	private final BackSubjectQuestionStatementJpaRepository repository;

	public StatementQuestionContentStore(BackSubjectQuestionStatementJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public String formCode() {
		return "vrai_faux";
	}

	@Override
	public Map<Long, StoredContent> load(List<Long> questionIds) {
		Map<Long, List<StoredStatement>> grouped = new HashMap<>();
		for (long id : questionIds) {
			grouped.put(id, new ArrayList<>());
		}
		for (BackSubjectQuestionStatementEntity row : repository
				.findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(questionIds)) {
			grouped.get(row.getQuestionId()).add(new StoredStatement(row.getId(), row.getLabel(), row.isTruthful()));
		}
		Map<Long, StoredContent> contents = new HashMap<>();
		grouped.forEach((id, rows) -> contents.put(id, new StoredStatements(rows)));
		return contents;
	}

	@Override
	public StoredContent write(long questionId, DraftContent content) {
		if (!(content instanceof StatementsDraft draft)) {
			throw new IllegalArgumentException("Contenu incompatible avec la forme vrai_faux");
		}
		List<BackSubjectQuestionStatementEntity> rows = new ArrayList<>();
		for (var row : draft.statements()) {
			rows.add(new BackSubjectQuestionStatementEntity(questionId, row.label(), row.truthful(), rows.size()));
		}
		return new StoredStatements(repository.saveAllAndFlush(rows).stream()
				.map(row -> new StoredStatement(row.getId(), row.getLabel(), row.isTruthful())).toList());
	}

	@Override
	public void delete(long questionId) {
		repository.deleteByQuestionId(questionId);
	}
}

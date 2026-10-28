package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.LinksDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredLinks;

/** Stockage de la structure liens. Une lecture groupée, indépendante des autres formes. */
@Component
@Profile("!test")
public class LinkQuestionContentStore implements QuestionContentStore {

	private final BackSubjectQuestionLinkJpaRepository repository;

	public LinkQuestionContentStore(BackSubjectQuestionLinkJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public String formCode() {
		return "liens";
	}

	@Override
	public Map<Long, StoredContent> load(List<Long> questionIds) {
		Map<Long, List<StoredLink>> grouped = new HashMap<>();
		for (long id : questionIds) {
			grouped.put(id, new ArrayList<>());
		}
		for (BackSubjectQuestionLinkEntity row : repository
				.findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(questionIds)) {
			grouped.get(row.getQuestionId())
					.add(new StoredLink(row.getId(), row.getLeftLabel(), row.getRightLabel()));
		}
		Map<Long, StoredContent> contents = new HashMap<>();
		grouped.forEach((id, rows) -> contents.put(id, new StoredLinks(rows)));
		return contents;
	}

	@Override
	public StoredContent write(long questionId, DraftContent content) {
		if (!(content instanceof LinksDraft draft)) {
			throw new IllegalArgumentException("Contenu incompatible avec la forme liens");
		}
		List<BackSubjectQuestionLinkEntity> rows = new ArrayList<>();
		for (var row : draft.links()) {
			rows.add(new BackSubjectQuestionLinkEntity(questionId, row.leftLabel(), row.rightLabel(), rows.size()));
		}
		return new StoredLinks(repository.saveAllAndFlush(rows).stream()
				.map(row -> new StoredLink(row.getId(), row.getLeftLabel(), row.getRightLabel())).toList());
	}

	@Override
	public void delete(long questionId) {
		repository.deleteByQuestionId(questionId);
	}
}

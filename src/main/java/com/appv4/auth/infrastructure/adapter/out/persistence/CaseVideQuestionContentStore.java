package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.GapsDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredGapWord;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredGaps;

/** Stockage de la structure case vide. Une lecture groupée, indépendante des autres formes. */
@Component
@Profile("!test")
public class CaseVideQuestionContentStore implements QuestionContentStore {

	private final BackSubjectQuestionGapJpaRepository repository;

	public CaseVideQuestionContentStore(BackSubjectQuestionGapJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public String formCode() {
		return "case_vide";
	}

	@Override
	public Map<Long, StoredContent> load(List<Long> questionIds) {
		Map<Long, String> sentences = new HashMap<>();
		Map<Long, List<StoredGapWord>> answers = new HashMap<>();
		Map<Long, List<StoredGapWord>> decoys = new HashMap<>();
		for (long id : questionIds) {
			sentences.put(id, "");
			answers.put(id, new ArrayList<>());
			decoys.put(id, new ArrayList<>());
		}
		for (BackSubjectQuestionGapEntity row : repository
				.findByQuestionIdInOrderByQuestionIdAscDecoyAscSortOrderAsc(questionIds)) {
			long questionId = row.getQuestionId();
			sentences.put(questionId, row.getSentence());
			StoredGapWord word = new StoredGapWord(row.getId(), row.getWordLabel());
			(row.isDecoy() ? decoys : answers).get(questionId).add(word);
		}
		Map<Long, StoredContent> contents = new HashMap<>();
		for (long id : questionIds) {
			contents.put(id, new StoredGaps(sentences.get(id), answers.get(id), decoys.get(id)));
		}
		return contents;
	}

	@Override
	public StoredContent write(long questionId, DraftContent content) {
		if (!(content instanceof GapsDraft draft)) {
			throw new IllegalArgumentException("Contenu incompatible avec la forme case vide");
		}
		List<BackSubjectQuestionGapEntity> rows = new ArrayList<>();
		int answerOrder = 0;
		for (var answer : draft.answers()) {
			rows.add(new BackSubjectQuestionGapEntity(
					questionId, draft.sentence(), answer.label(), false, answerOrder++));
		}
		int decoyOrder = 0;
		for (var decoy : draft.decoys()) {
			rows.add(new BackSubjectQuestionGapEntity(
					questionId, draft.sentence(), decoy.label(), true, decoyOrder++));
		}
		List<StoredGapWord> answers = new ArrayList<>();
		List<StoredGapWord> decoys = new ArrayList<>();
		for (BackSubjectQuestionGapEntity row : repository.saveAllAndFlush(rows)) {
			StoredGapWord word = new StoredGapWord(row.getId(), row.getWordLabel());
			if (row.isDecoy()) {
				decoys.add(word);
			} else {
				answers.add(word);
			}
		}
		return new StoredGaps(draft.sentence(), answers, decoys);
	}

	@Override
	public void delete(long questionId) {
		repository.deleteByQuestionId(questionId);
	}
}

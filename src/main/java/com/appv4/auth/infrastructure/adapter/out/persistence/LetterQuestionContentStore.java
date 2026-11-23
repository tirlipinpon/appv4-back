package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.FreeTextDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredFreeText;
import com.appv4.auth.application.service.TeacherQuestionService;

/**
 * Stockage de la saisie lettre par lettre (SCRUM-182).
 *
 * <p><b>Business :</b> une question de cette forme a exactement un mot attendu.
 * <p><b>Technique :</b> une lecture groupée, dans sa table, à part de la réponse libre.
 * <p><b>Métier / flow :</b> le port des questions délègue ici dès que le code est {@code reponse_libre}.
 */
@Component
@Profile("!test")
public class LetterQuestionContentStore implements QuestionContentStore {

	private final BackSubjectQuestionLetterJpaRepository repository;

	public LetterQuestionContentStore(BackSubjectQuestionLetterJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public String formCode() {
		return TeacherQuestionService.REPONSE_LIBRE;
	}

	@Override
	public Map<Long, StoredContent> load(List<Long> questionIds) {
		Map<Long, String> answers = new HashMap<>();
		for (long id : questionIds) {
			answers.put(id, "");
		}
		for (BackSubjectQuestionLetterEntity row : repository.findByQuestionIdIn(questionIds)) {
			answers.put(row.getQuestionId(), row.getExpectedAnswer());
		}
		Map<Long, StoredContent> contents = new HashMap<>();
		for (long id : questionIds) {
			contents.put(id, new StoredFreeText(answers.get(id)));
		}
		return contents;
	}

	@Override
	public StoredContent write(long questionId, DraftContent content) {
		if (!(content instanceof FreeTextDraft draft)) {
			throw new IllegalArgumentException("Contenu incompatible avec la saisie lettre par lettre");
		}
		BackSubjectQuestionLetterEntity saved = repository.saveAndFlush(
				new BackSubjectQuestionLetterEntity(questionId, draft.expectedAnswer()));
		return new StoredFreeText(saved.getExpectedAnswer());
	}

	@Override
	public void delete(long questionId) {
		repository.deleteByQuestionId(questionId);
	}
}

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
 * Stockage de la réponse libre (SCRUM-198).
 *
 * <p><b>Business :</b> une question de cette forme a exactement une réponse attendue.
 * <p><b>Technique :</b> une lecture groupée, indépendante des autres formes.
 * <p><b>Métier / flow :</b> le port des questions délègue ici dès que le code est {@code texte}.
 */
@Component
@Profile("!test")
public class TexteQuestionContentStore implements QuestionContentStore {

	private final BackSubjectQuestionTextJpaRepository repository;

	public TexteQuestionContentStore(BackSubjectQuestionTextJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public String formCode() {
		return TeacherQuestionService.TEXTE;
	}

	@Override
	public Map<Long, StoredContent> load(List<Long> questionIds) {
		Map<Long, String> answers = new HashMap<>();
		for (long id : questionIds) {
			answers.put(id, "");
		}
		for (BackSubjectQuestionTextEntity row : repository.findByQuestionIdIn(questionIds)) {
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
			throw new IllegalArgumentException("Contenu incompatible avec la forme texte");
		}
		BackSubjectQuestionTextEntity saved = repository.saveAndFlush(
				new BackSubjectQuestionTextEntity(questionId, draft.expectedAnswer()));
		return new StoredFreeText(saved.getExpectedAnswer());
	}

	@Override
	public void delete(long questionId) {
		repository.deleteByQuestionId(questionId);
	}
}

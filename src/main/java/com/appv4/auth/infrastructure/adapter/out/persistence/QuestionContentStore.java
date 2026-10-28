package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Map;

import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;

/** Extension par forme : nouvelle structure = nouveau store, pas de branche dans l'adaptateur commun. */
public interface QuestionContentStore {

	String formCode();

	/** Appelé avec 1 à 500 identifiants appartenant ? cette forme. Inclut les contenus vides. */
	Map<Long, StoredContent> load(List<Long> questionIds);

	StoredContent write(long questionId, DraftContent content);

	void delete(long questionId);
}

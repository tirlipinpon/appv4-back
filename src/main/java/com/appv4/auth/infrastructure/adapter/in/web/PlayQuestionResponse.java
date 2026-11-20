package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.UUID;

import com.appv4.auth.application.result.ChildPlayQuestionView;
import com.appv4.auth.application.result.ChildPlayQuestionView.Payload;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayClassification;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayGaps;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayImage;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayLetters;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayLinks;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayMemory;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayOrder;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayPuzzle;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayQcm;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlaySimon;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayText;
import com.appv4.auth.application.result.ChildPlayQuestionView.PlayTrueFalse;
import com.appv4.auth.application.service.question.QuestionFormLooks;
import com.appv4.auth.application.service.question.QuestionFormRegistry;

import io.swagger.v3.oas.annotations.media.Schema;

/** Contrat de rendu commun à Learning et à l'aperçu professeur. */
public record PlayQuestionResponse(
		long questionId, long subjectId, String formCode, String prompt,
		String formLabel, String iconCode, String color, UUID challengeId,
		@Schema(oneOf = {PlayQcm.class, PlayTrueFalse.class, PlayMemory.class, PlayOrder.class,
				PlayClassification.class, PlayLinks.class, PlayGaps.class, PlayImage.class,
				PlayLetters.class, PlayText.class, PlayPuzzle.class, PlaySimon.class})
		Payload content) {

	static PlayQuestionResponse from(ChildPlayQuestionView view, QuestionFormRegistry registry) {
		QuestionFormLooks.Look look = QuestionFormLooks.of(view.formCode());
		return new PlayQuestionResponse(
				view.questionId(), view.subjectId(), view.formCode(), view.prompt(), registry.labelOf(view.formCode()),
				look == null ? null : look.iconCode(), look == null ? null : look.color(),
				view.challengeId(), view.content());
	}
}

package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.LINKS_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.LINK_INCOMPLETE;
import static com.appv4.auth.application.service.TeacherQuestionService.LINK_TOO_LONG;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

import java.util.ArrayList;
import java.util.List;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.LinkInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.LinksInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.LinksDraft;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;

/** Règles de saisie de la forme liens. */
public final class LinkQuestionForm implements QuestionFormHandler {

	private static final int LABEL_MAX = 200;

	@Override
	public FormLine form() {
		return new FormLine("liens", "Liens", 5);
	}

	@Override
	public Prepared prepare(String prompt, ContentInput input) {
		if (!(input instanceof LinksInput links)) {
			throw new IllegalArgumentException("Entrée incompatible avec liens");
		}
		return new Prepared(QuestionValidation.optionalPrompt(prompt), new LinksDraft(requireLinks(links.links())));
	}

	private static List<TeacherQuestionPort.LinkDraft> requireLinks(List<LinkInput> links) {
		List<TeacherQuestionPort.LinkDraft> kept = new ArrayList<>();
		if (links != null) {
			for (LinkInput link : links) {
				if (link == null) {
					continue;
				}
				PieceFaces.Face leftFace = PieceFaces.optional(
						link.leftLabel(), link.leftImageWebp(), LABEL_MAX, LINK_TOO_LONG);
				PieceFaces.Face rightFace = PieceFaces.optional(
						link.rightLabel(), link.rightImageWebp(), LABEL_MAX, LINK_TOO_LONG);
				if (leftFace == null && rightFace == null) {
					continue;
				}
				if (leftFace == null || rightFace == null) {
					throw bad(LINK_INCOMPLETE);
				}
				kept.add(new TeacherQuestionPort.LinkDraft(
						leftFace.label(), rightFace.label(), leftFace.imageWebp(), rightFace.imageWebp()));
			}
		}
		if (kept.size() < 2) {
			throw bad(LINKS_REQUIRED);
		}
		return List.copyOf(kept);
	}
}

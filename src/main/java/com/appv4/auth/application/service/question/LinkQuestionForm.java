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
				String left = link.leftLabel() == null ? "" : link.leftLabel().trim();
				String right = link.rightLabel() == null ? "" : link.rightLabel().trim();
				if (left.isEmpty() && right.isEmpty()) {
					continue;
				}
				if (left.isEmpty() || right.isEmpty()) {
					throw bad(LINK_INCOMPLETE);
				}
				if (left.length() > LABEL_MAX || right.length() > LABEL_MAX) {
					throw bad(LINK_TOO_LONG);
				}
				kept.add(new TeacherQuestionPort.LinkDraft(left, right));
			}
		}
		if (kept.size() < 2) {
			throw bad(LINKS_REQUIRED);
		}
		return List.copyOf(kept);
	}
}

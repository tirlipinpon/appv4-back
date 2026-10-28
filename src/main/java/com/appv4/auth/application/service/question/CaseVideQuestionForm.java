package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.GAP_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.GAP_SENTENCE_TOO_LONG;
import static com.appv4.auth.application.service.TeacherQuestionService.GAP_WORD_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.GAP_WORD_TOO_LONG;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

import java.util.ArrayList;
import java.util.List;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.GapWordInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.GapsInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort.GapWordDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.GapsDraft;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;

/** Règles de saisie de la forme case vide. */
public final class CaseVideQuestionForm implements QuestionFormHandler {

	static final String HOLE = "___";

	private static final int SENTENCE_MAX = 500;
	private static final int WORD_MAX = 200;

	@Override
	public FormLine form() {
		return new FormLine("case_vide", "Case vide", 6);
	}

	@Override
	public Prepared prepare(String prompt, ContentInput input) {
		if (!(input instanceof GapsInput gaps)) {
			throw new IllegalArgumentException("Entrée incompatible avec case vide");
		}
		String sentence = gaps.sentence() == null ? "" : gaps.sentence().trim();
		if (sentence.length() > SENTENCE_MAX) {
			throw bad(GAP_SENTENCE_TOO_LONG);
		}
		int holes = countHoles(sentence);
		if (holes < 1) {
			throw bad(GAP_REQUIRED);
		}
		return new Prepared(
				QuestionValidation.optionalPrompt(prompt),
				new GapsDraft(sentence, requireAnswers(gaps.answers(), holes), keepDecoys(gaps.decoys())));
	}

	static int countHoles(String sentence) {
		int count = 0;
		int from = 0;
		while (from <= sentence.length() - HOLE.length()) {
			int at = sentence.indexOf(HOLE, from);
			if (at < 0) {
				break;
			}
			count++;
			from = at + HOLE.length();
		}
		return count;
	}

	private static List<GapWordDraft> requireAnswers(List<GapWordInput> answers, int holes) {
		List<GapWordDraft> kept = new ArrayList<>();
		if (answers != null) {
			for (GapWordInput answer : answers) {
				if (answer == null) {
					continue;
				}
				String label = answer.label() == null ? "" : answer.label().trim();
				if (label.isEmpty()) {
					throw bad(GAP_WORD_REQUIRED);
				}
				if (label.length() > WORD_MAX) {
					throw bad(GAP_WORD_TOO_LONG);
				}
				kept.add(new GapWordDraft(label));
			}
		}
		if (kept.size() != holes) {
			throw bad(GAP_WORD_REQUIRED);
		}
		return List.copyOf(kept);
	}

	private static List<GapWordDraft> keepDecoys(List<GapWordInput> decoys) {
		List<GapWordDraft> kept = new ArrayList<>();
		if (decoys != null) {
			for (GapWordInput decoy : decoys) {
				if (decoy == null) {
					continue;
				}
				String label = decoy.label() == null ? "" : decoy.label().trim();
				if (label.isEmpty()) {
					continue;
				}
				if (label.length() > WORD_MAX) {
					throw bad(GAP_WORD_TOO_LONG);
				}
				kept.add(new GapWordDraft(label));
			}
		}
		return List.copyOf(kept);
	}
}

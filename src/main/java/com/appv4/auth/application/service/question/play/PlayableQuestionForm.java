package com.appv4.auth.application.service.question.play;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;

/** Formes auxquelles le moteur enfant et les missions savent donner accès. */
public enum PlayableQuestionForm {
	QCM(QcmChildAnswerGrader.FORM),
	TRUE_FALSE(TrueFalseChildAnswerGrader.FORM),
	MEMORY(MemoryChildAnswerGrader.FORM),
	ORDER(OrderChildAnswerGrader.FORM),
	CLASSIFICATION(ClassificationChildAnswerGrader.FORM),
	LINKS(LinkChildAnswerGrader.FORM),
	GAPS(GapChildAnswerGrader.FORM),
	CLICK_IMAGE(ClickChildAnswerGrader.FORM),
	LETTERS(LetterChildAnswerGrader.FORM),
	TEXT(FreeTextChildAnswerGrader.FORM),
	PUZZLE(PuzzleChildAnswerGrader.FORM),
	SIMON(SimonChildAnswerGrader.FORM);

	public static final String UNSUPPORTED = "Cette forme de question n'est pas encore jouable.";

	private static final Map<String, PlayableQuestionForm> FORMS = Arrays.stream(values())
			.collect(Collectors.toUnmodifiableMap(PlayableQuestionForm::code, Function.identity()));

	private final String code;

	PlayableQuestionForm(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}

	public static Set<String> codes() {
		return FORMS.keySet();
	}

	public static PlayableQuestionForm require(String code) {
		PlayableQuestionForm form = FORMS.get(code);
		if (form == null) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, UNSUPPORTED);
		}
		return form;
	}
}

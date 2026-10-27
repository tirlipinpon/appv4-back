package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ChoiceInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.PairInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ChoiceDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.result.TeacherQuestionFormsView;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.ChoiceLine;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.QuestionLine;

/**
 * Choix multiple et Memory préparés dans une matière (SCRUM-174, SCRUM-175).
 *
 * <p><b>Business :</b> le professeur ne voit que les formes qu'on sait enregistrer.
 * Le choix multiple exige un énoncé, deux propositions et une bonne réponse.
 * Le Memory exige deux paires complètes. L'énoncé du Memory peut rester vide.
 * Corriger une question remplace son contenu. La forme reste.
 * Le nom de la matière ne change pas. L'élève ne joue pas ici.
 * <p><b>Technique :</b> catalogue en code ({@code qcm} puis {@code memory}). Les
 * énoncés passent par {@link TeacherQuestionPort}. L'accès matière passe par
 * {@link TeacherSubjectPort#findOwned}.
 * <p><b>Métier / flow :</b> port in → ce service → ports out. L'écran élève
 * n'est pas dans ce service.
 */
public class TeacherQuestionService implements TeacherQuestionUseCase {

	public static final String QCM = "qcm";
	public static final String QCM_LABEL = "Choix multiples";
	public static final int QCM_ORDER = 1;

	public static final String MEMORY = "memory";
	public static final String MEMORY_LABEL = "Memory";
	public static final int MEMORY_ORDER = 2;

	public static final String PROMPT_REQUIRED = "L'énoncé est obligatoire.";
	public static final String PROMPT_TOO_LONG = "L'énoncé dépasse 500 caractères.";
	public static final String CHOICE_TOO_LONG = "Une proposition dépasse 200 caractères.";
	public static final String CHOICES_REQUIRED = "Il faut au moins deux propositions.";
	public static final String CORRECT_REQUIRED = "Il faut marquer au moins une bonne réponse.";
	public static final String FORM_UNKNOWN = "Cette forme de question n'est pas disponible.";
	public static final String FORM_LOCKED = "La forme de la question reste celle déjà enregistrée.";
	public static final String QUESTION_MISSING = "Cette question n'existe pas.";
	public static final String PAIRS_REQUIRED = "Il faut au moins deux paires.";
	public static final String PAIR_TOO_LONG = "Un côté de paire dépasse 200 caractères.";

	private static final int PROMPT_MAX = 500;
	private static final int CHOICE_MAX = 200;

	private static final List<FormLine> OFFERED = List.of(
			new FormLine(QCM, QCM_LABEL, QCM_ORDER),
			new FormLine(MEMORY, MEMORY_LABEL, MEMORY_ORDER));

	private final TeacherSubjectPort teacherSubjectPort;
	private final TeacherQuestionPort teacherQuestionPort;

	public TeacherQuestionService(
			TeacherSubjectPort teacherSubjectPort,
			TeacherQuestionPort teacherQuestionPort) {
		this.teacherSubjectPort = teacherSubjectPort;
		this.teacherQuestionPort = teacherQuestionPort;
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherQuestionFormsView listForms(long userId, String activeRole) {
		requireTeacher(activeRole);
		return new TeacherQuestionFormsView(OFFERED);
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherSubjectQuestionsView list(long userId, String activeRole, long subjectId) {
		requireTeacher(activeRole);
		requireOwned(userId, subjectId);
		return new TeacherSubjectQuestionsView(lines(teacherQuestionPort.findBySubject(subjectId)));
	}

	@Override
	@Transactional
	public QuestionLine create(
			long userId,
			String activeRole,
			long subjectId,
			String formCode,
			String prompt,
			List<ChoiceInput> choices,
			List<PairInput> pairs) {
		requireTeacher(activeRole);
		requireOwned(userId, subjectId);
		FormLine form = requireForm(formCode);
		Prepared prepared = prepare(form, prompt, choices, pairs);
		teacherQuestionPort.ensureForm(subjectId, form.code(), form.label(), form.sortOrder());
		StoredQuestion saved = teacherQuestionPort.insert(
				subjectId, form.code(), prepared.prompt(), prepared.choices(), prepared.pairs());
		return line(saved);
	}

	@Override
	@Transactional
	public QuestionLine update(
			long userId,
			String activeRole,
			long subjectId,
			long questionId,
			String formCode,
			String prompt,
			List<ChoiceInput> choices,
			List<PairInput> pairs) {
		requireTeacher(activeRole);
		requireOwned(userId, subjectId);
		FormLine form = requireForm(formCode);
		StoredQuestion current = teacherQuestionPort.findInSubject(subjectId, questionId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, QUESTION_MISSING));
		if (!form.code().equals(current.formCode())) {
			throw bad(FORM_LOCKED);
		}
		Prepared prepared = prepare(form, prompt, choices, pairs);
		return line(teacherQuestionPort.replace(
				questionId, prepared.prompt(), prepared.choices(), prepared.pairs()));
	}

	private static Prepared prepare(
			FormLine form, String prompt, List<ChoiceInput> choices, List<PairInput> pairs) {
		if (MEMORY.equals(form.code())) {
			return new Prepared(optionalPrompt(prompt), List.of(), requirePairs(pairs));
		}
		return new Prepared(requirePrompt(prompt), requireChoices(choices), List.of());
	}

	private static FormLine requireForm(String formCode) {
		String code = formCode == null ? "" : formCode.trim();
		for (FormLine form : OFFERED) {
			if (form.code().equals(code)) {
				return form;
			}
		}
		throw bad(FORM_UNKNOWN);
	}

	private static String requirePrompt(String prompt) {
		String statement = prompt == null ? "" : prompt.trim();
		if (statement.isEmpty()) {
			throw bad(PROMPT_REQUIRED);
		}
		if (statement.length() > PROMPT_MAX) {
			throw bad(PROMPT_TOO_LONG);
		}
		return statement;
	}

	private static String optionalPrompt(String prompt) {
		String statement = prompt == null ? "" : prompt.trim();
		if (statement.length() > PROMPT_MAX) {
			throw bad(PROMPT_TOO_LONG);
		}
		return statement;
	}

	private static List<ChoiceDraft> requireChoices(List<ChoiceInput> choices) {
		List<ChoiceDraft> kept = new ArrayList<>();
		if (choices != null) {
			for (ChoiceInput choice : choices) {
				if (choice == null) {
					continue;
				}
				String label = choice.label() == null ? "" : choice.label().trim();
				if (label.isEmpty()) {
					continue;
				}
				if (label.length() > CHOICE_MAX) {
					throw bad(CHOICE_TOO_LONG);
				}
				kept.add(new ChoiceDraft(label, Boolean.TRUE.equals(choice.correct())));
			}
		}
		if (kept.size() < 2) {
			throw bad(CHOICES_REQUIRED);
		}
		boolean oneCorrect = false;
		for (ChoiceDraft choice : kept) {
			if (choice.correct()) {
				oneCorrect = true;
				break;
			}
		}
		if (!oneCorrect) {
			throw bad(CORRECT_REQUIRED);
		}
		return List.copyOf(kept);
	}

	private static List<TeacherQuestionPort.PairDraft> requirePairs(List<PairInput> pairs) {
		List<TeacherQuestionPort.PairDraft> kept = new ArrayList<>();
		if (pairs != null) {
			for (PairInput pair : pairs) {
				if (pair == null) {
					continue;
				}
				String questionSide = pair.questionSide() == null ? "" : pair.questionSide().trim();
				String answerSide = pair.answerSide() == null ? "" : pair.answerSide().trim();
				if (questionSide.isEmpty() || answerSide.isEmpty()) {
					continue;
				}
				if (questionSide.length() > CHOICE_MAX || answerSide.length() > CHOICE_MAX) {
					throw bad(PAIR_TOO_LONG);
				}
				kept.add(new TeacherQuestionPort.PairDraft(questionSide, answerSide));
			}
		}
		if (kept.size() < 2) {
			throw bad(PAIRS_REQUIRED);
		}
		return List.copyOf(kept);
	}

	private void requireOwned(long userId, long subjectId) {
		if (teacherSubjectPort.findOwned(userId, subjectId).isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, TeacherSubjectService.SUBJECT_MISSING);
		}
	}

	private static List<QuestionLine> lines(List<StoredQuestion> stored) {
		if (stored == null || stored.isEmpty()) {
			return List.of();
		}
		List<QuestionLine> lines = new ArrayList<>();
		for (StoredQuestion question : stored) {
			lines.add(line(question));
		}
		return List.copyOf(lines);
	}

	private static QuestionLine line(StoredQuestion question) {
		List<ChoiceLine> choices = new ArrayList<>();
		if (question.choices() != null) {
			for (TeacherQuestionPort.StoredChoice choice : question.choices()) {
				choices.add(new ChoiceLine(choice.id(), choice.label(), choice.correct()));
			}
		}
		return new QuestionLine(
				question.id(),
				question.formCode(),
				labelOf(question.formCode()),
				question.prompt(),
				List.copyOf(choices),
				pairsOf(question));
	}

	private static List<TeacherSubjectQuestionsView.PairLine> pairsOf(StoredQuestion question) {
		List<TeacherSubjectQuestionsView.PairLine> pairs = new ArrayList<>();
		if (question.pairs() != null) {
			for (TeacherQuestionPort.StoredPair pair : question.pairs()) {
				pairs.add(new TeacherSubjectQuestionsView.PairLine(
						pair.id(), pair.questionSide(), pair.answerSide()));
			}
		}
		return List.copyOf(pairs);
	}

	private static String labelOf(String formCode) {
		for (FormLine form : OFFERED) {
			if (form.code().equals(formCode)) {
				return form.label();
			}
		}
		return formCode;
	}

	private static void requireTeacher(String activeRole) {
		if (!ActiveRoles.TEACHER.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceHello.DENIED);
		}
	}

	private static AuthDomainException bad(String message) {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST, message);
	}

	private record Prepared(String prompt, List<ChoiceDraft> choices, List<TeacherQuestionPort.PairDraft> pairs) {
	}
}

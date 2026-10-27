package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase;
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
 * Choix multiple préparé dans une matière (SCRUM-174).
 *
 * <p><b>Business :</b> le professeur ne voit que les formes qu'on sait enregistrer.
 * Il rattache l'énoncé à une matière à laquelle il a accès. Une proposition vide
 * disparaît. Sans deux propositions, ou sans bonne réponse, rien n'est créé.
 * Corriger une question remplace l'énoncé et les propositions. La forme reste.
 * Le nom de la matière ne change pas.
 * <p><b>Technique :</b> catalogue en code (une ligne {@code qcm}). Les énoncés
 * passent par {@link TeacherQuestionPort}. L'accès matière passe par
 * {@link TeacherSubjectPort#findOwned}.
 * <p><b>Métier / flow :</b> port in → ce service → ports out. L'écran élève
 * n'est pas dans ce service.
 */
public class TeacherQuestionService implements TeacherQuestionUseCase {

	public static final String QCM = "qcm";
	public static final String QCM_LABEL = "Choix multiples";
	public static final int QCM_ORDER = 1;

	public static final String PROMPT_REQUIRED = "L'énoncé est obligatoire.";
	public static final String PROMPT_TOO_LONG = "L'énoncé dépasse 500 caractères.";
	public static final String CHOICE_TOO_LONG = "Une proposition dépasse 200 caractères.";
	public static final String CHOICES_REQUIRED = "Il faut au moins deux propositions.";
	public static final String CORRECT_REQUIRED = "Il faut marquer au moins une bonne réponse.";
	public static final String FORM_UNKNOWN = "Cette forme de question n'est pas disponible.";
	public static final String FORM_LOCKED = "La forme de la question reste celle déjà enregistrée.";
	public static final String QUESTION_MISSING = "Cette question n'existe pas.";

	private static final int PROMPT_MAX = 500;
	private static final int CHOICE_MAX = 200;

	private static final List<FormLine> OFFERED = List.of(new FormLine(QCM, QCM_LABEL, QCM_ORDER));

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
			List<ChoiceInput> choices) {
		requireTeacher(activeRole);
		requireOwned(userId, subjectId);
		FormLine form = requireForm(formCode);
		String statement = requirePrompt(prompt);
		List<ChoiceDraft> kept = requireChoices(choices);
		teacherQuestionPort.ensureForm(subjectId, form.code(), form.label(), form.sortOrder());
		StoredQuestion saved = teacherQuestionPort.insert(subjectId, form.code(), statement, kept);
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
			List<ChoiceInput> choices) {
		requireTeacher(activeRole);
		requireOwned(userId, subjectId);
		FormLine form = requireForm(formCode);
		StoredQuestion current = teacherQuestionPort.findInSubject(subjectId, questionId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, QUESTION_MISSING));
		if (!form.code().equals(current.formCode())) {
			throw bad(FORM_LOCKED);
		}
		String statement = requirePrompt(prompt);
		List<ChoiceDraft> kept = requireChoices(choices);
		return line(teacherQuestionPort.replace(questionId, statement, kept));
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
				List.copyOf(choices));
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
}

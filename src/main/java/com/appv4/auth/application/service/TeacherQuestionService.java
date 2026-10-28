package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.result.TeacherQuestionFormsView;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.QuestionLine;
import com.appv4.auth.application.service.question.QuestionFormHandler;
import com.appv4.auth.application.service.question.QuestionFormHandler.Prepared;
import com.appv4.auth.application.service.question.QuestionFormRegistry;

/**
 * Choix multiple, Memory et Ordre préparés dans une matière (SCRUM-174, SCRUM-175, SCRUM-176).
 *
 * <p><b>Business :</b> le professeur ne voit que les formes qu'on sait enregistrer.
 * Le choix multiple exige un énoncé, deux propositions et une bonne réponse.
 * Le Memory exige deux paires complètes. L'ordre exige deux éléments déjà rangés.
 * L'énoncé du Memory et de l'ordre peut rester vide.
 * Corriger une question remplace son contenu. La forme reste.
 * Le nom de la matière ne change pas. L'élève ne joue pas ici.
 * <p><b>Technique :</b> registre de validateurs indépendants par forme.
 * Les énoncés passent par {@link TeacherQuestionPort}. L'accès matière passe par
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

	public static final String ORDER = "order";
	public static final String ORDER_LABEL = "Ordre";
	public static final int ORDER_SORT = 3;

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
	public static final String ITEMS_REQUIRED = "Il faut au moins deux éléments.";
	public static final String ITEM_TOO_LONG = "Un élément dépasse 200 caractères.";

	private final QuestionFormRegistry forms;

	private final TeacherSubjectPort teacherSubjectPort;
	private final TeacherQuestionPort teacherQuestionPort;

	public TeacherQuestionService(
			TeacherSubjectPort teacherSubjectPort,
			TeacherQuestionPort teacherQuestionPort,
			QuestionFormRegistry forms) {
		this.teacherSubjectPort = teacherSubjectPort;
		this.teacherQuestionPort = teacherQuestionPort;
		this.forms = forms;
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherQuestionFormsView listForms(long userId, String activeRole) {
		requireTeacher(activeRole);
		return new TeacherQuestionFormsView(forms.forms());
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
	public QuestionLine create(long userId, String activeRole, long subjectId, String formCode, String prompt,
			List<ChoiceInput> choices, List<PairInput> pairs, List<ItemInput> items) {
		return create(userId, activeRole, subjectId, formCode, prompt, new LegacyInput(choices, pairs, items));
	}

	@Override
	@Transactional
	public QuestionLine create(
			long userId,
			String activeRole,
			long subjectId,
			String formCode,
			String prompt,
			ContentInput content) {
		requireTeacher(activeRole);
		requireOwned(userId, subjectId);
		QuestionFormHandler handler = forms.require(formCode);
		FormLine form = handler.form();
		Prepared prepared = handler.prepare(prompt, content);
		teacherQuestionPort.ensureForm(subjectId, form.code(), form.label(), form.sortOrder());
		StoredQuestion saved = teacherQuestionPort.insert(
				subjectId,
				form.code(),
				prepared.prompt(),
				prepared.content());
		return line(saved);
	}

	@Override
	@Transactional
	public QuestionLine update(long userId, String activeRole, long subjectId, long questionId, String formCode, String prompt,
			List<ChoiceInput> choices, List<PairInput> pairs, List<ItemInput> items) {
		return update(userId, activeRole, subjectId, questionId, formCode, prompt, new LegacyInput(choices, pairs, items));
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
			ContentInput content) {
		requireTeacher(activeRole);
		requireOwned(userId, subjectId);
		QuestionFormHandler handler = forms.require(formCode);
		FormLine form = handler.form();
		StoredQuestion current = teacherQuestionPort.findInSubject(subjectId, questionId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, QUESTION_MISSING));
		if (!form.code().equals(current.formCode())) {
			throw bad(FORM_LOCKED);
		}
		Prepared prepared = handler.prepare(prompt, content);
		return line(teacherQuestionPort.replace(
				questionId, prepared.prompt(), prepared.content()));
	}

	private void requireOwned(long userId, long subjectId) {
		if (teacherSubjectPort.findOwned(userId, subjectId).isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, TeacherSubjectService.SUBJECT_MISSING);
		}
	}

	private List<QuestionLine> lines(List<StoredQuestion> stored) {
		if (stored == null || stored.isEmpty()) {
			return List.of();
		}
		List<QuestionLine> lines = new ArrayList<>();
		for (StoredQuestion question : stored) {
			lines.add(line(question));
		}
		return List.copyOf(lines);
	}

	private QuestionLine line(StoredQuestion question) {
		return new QuestionLine(question.id(), question.formCode(), forms.labelOf(question.formCode()),
				question.prompt(), question.content());
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

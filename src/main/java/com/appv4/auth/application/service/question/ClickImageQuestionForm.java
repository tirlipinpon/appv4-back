package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.IMAGE_CLICK;
import static com.appv4.auth.application.service.TeacherQuestionService.IMAGE_CLICK_LABEL;
import static com.appv4.auth.application.service.TeacherQuestionService.IMAGE_CLICK_ORDER;
import static com.appv4.auth.application.service.TeacherQuestionService.ZONE_NAME_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.ZONE_NAME_TOO_LONG;
import static com.appv4.auth.application.service.TeacherQuestionService.ZONE_RULE_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.ZONE_SIDE_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.ZONES_REQUIRED;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

import java.util.ArrayList;
import java.util.List;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ClickImageInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ClickZoneInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickImageDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickZoneDraft;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;

/**
 * Règles de saisie d'une image à cliquer (SCRUM-181).
 *
 * <p><b>Business :</b> le professeur pose une image, nomme chaque zone, et dit si elle est
 * juste. Il dit aussi s'il faut cliquer toutes les zones justes. L'énoncé peut rester vide.
 * <p><b>Technique :</b> code {@code image_interactive}, ordre 8. L'image et les points
 * passent par {@link DrawnImageRules}.
 * <p><b>Métier / flow :</b> le registre appelle {@link #prepare} avant l'enregistrement.
 * L'élève ne joue pas encore.
 */
public final class ClickImageQuestionForm implements QuestionFormHandler {

	private static final int NAME_MAX = 200;

	@Override
	public FormLine form() {
		return new FormLine(IMAGE_CLICK, IMAGE_CLICK_LABEL, IMAGE_CLICK_ORDER);
	}

	@Override
	public Prepared prepare(String prompt, ContentInput input) {
		if (!(input instanceof ClickImageInput image)) {
			throw new IllegalArgumentException("Entrée incompatible avec l'image à cliquer");
		}
		if (image.requireAllCorrect() == null) {
			throw bad(ZONE_RULE_REQUIRED);
		}
		return new Prepared(
				QuestionValidation.optionalPrompt(prompt),
				new ClickImageDraft(
						DrawnImageRules.requireWebp(image.imageWebp()),
						image.requireAllCorrect(),
						requireZones(image.zones())));
	}

	private static List<ClickZoneDraft> requireZones(List<ClickZoneInput> zones) {
		if (zones == null || zones.isEmpty()) {
			throw bad(ZONES_REQUIRED);
		}
		List<ClickZoneDraft> drafts = new ArrayList<>();
		for (ClickZoneInput zone : zones) {
			if (zone == null) {
				throw bad(ZONE_NAME_REQUIRED);
			}
			drafts.add(new ClickZoneDraft(
					requireName(zone.label()),
					requireSide(zone.correct()),
					DrawnImageRules.shape(zone.shape()),
					DrawnImageRules.points(zone.shape(), zone.points())));
		}
		return List.copyOf(drafts);
	}

	private static String requireName(String label) {
		String name = label == null ? "" : label.trim();
		if (name.isEmpty()) {
			throw bad(ZONE_NAME_REQUIRED);
		}
		if (name.length() > NAME_MAX) {
			throw bad(ZONE_NAME_TOO_LONG);
		}
		return name;
	}

	private static boolean requireSide(Boolean correct) {
		if (correct == null) {
			throw bad(ZONE_SIDE_REQUIRED);
		}
		return correct;
	}
}

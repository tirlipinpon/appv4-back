package com.appv4.auth.application.service.question.play;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.ZoneClickAnswer;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickZone;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

/**
 * Note une image à cliquer.
 *
 * <p><b>Business :</b> sans ordre, le score compte les zones justes trouvées
 * (un clic faux en plus ne retire pas de point). Avec ordre, chaque étape doit
 * être la bonne zone, et la partie n'est réussie que si toute la suite l'est.
 * <p><b>Technique :</b> la trace réutilise {@code answer_choice_id} avec
 * {@code target_kind = zone}, pour rester dans la contrainte des tentatives.
 * <p><b>Métier / flow :</b> POST attempts → {@link com.appv4.auth.application.service.ChildQuestionPlayService}.
 */
public final class ClickChildAnswerGrader {

	public static final String FORM = "image_interactive";
	public static final String BAD_ANSWER = "Réponse invalide pour cette question.";

	private ClickChildAnswerGrader() {
	}

	public static ChildAnswerGrade grade(StoredQuestion question, List<ZoneClickAnswer> clicks) {
		List<StoredClickZone> zones = question.clickZones();
		if (zones.isEmpty() || question.imageWebp().length == 0) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		boolean ordered = question.clicksOrdered() == null || question.clicksOrdered();
		List<ZoneClickAnswer> submitted = clicks == null ? List.of() : clicks;
		Set<Long> known = new HashSet<>();
		for (StoredClickZone zone : zones) {
			known.add(zone.id());
		}
		Set<Long> seen = new HashSet<>();
		for (ZoneClickAnswer click : submitted) {
			if (click == null || !known.contains(click.zoneId()) || !seen.add(click.zoneId())) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
			}
		}
		if (ordered) {
			return gradeOrdered(zones, submitted);
		}
		return gradeUnordered(question, zones, submitted);
	}

	private static ChildAnswerGrade gradeOrdered(List<StoredClickZone> zones, List<ZoneClickAnswer> submitted) {
		List<StoredClickZone> steps = zones.stream()
				.filter(StoredClickZone::correct)
				.sorted(Comparator.comparing(zone -> zone.clickOrder() == null ? Integer.MAX_VALUE : zone.clickOrder()))
				.toList();
		if (steps.isEmpty() || steps.stream().anyMatch(zone -> zone.clickOrder() == null)) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		if (submitted.size() != steps.size()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		int ok = 0;
		List<AnswerLine> lines = new ArrayList<>();
		for (int index = 0; index < steps.size(); index++) {
			long chosen = submitted.get(index).zoneId();
			boolean correct = chosen == steps.get(index).id();
			if (correct) {
				ok++;
			}
			lines.add(new AnswerLine("zone", steps.get(index).id(), null, chosen, correct));
		}
		int score = (int) Math.round(100.0 * ok / steps.size());
		return new ChildAnswerGrade(score, ok == steps.size(), lines);
	}

	private static ChildAnswerGrade gradeUnordered(
			StoredQuestion question, List<StoredClickZone> zones, List<ZoneClickAnswer> submitted) {
		if (submitted.isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Set<Long> correctIds = new HashSet<>();
		for (StoredClickZone zone : zones) {
			if (zone.correct()) {
				correctIds.add(zone.id());
			}
		}
		if (correctIds.isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		int ok = 0;
		List<AnswerLine> lines = new ArrayList<>();
		for (ZoneClickAnswer click : submitted) {
			boolean correct = correctIds.contains(click.zoneId());
			if (correct) {
				ok++;
			}
			lines.add(new AnswerLine("zone", click.zoneId(), null, click.zoneId(), correct));
		}
		int score = (int) Math.round(100.0 * ok / correctIds.size());
		boolean passed = Boolean.TRUE.equals(question.requireAllCorrect())
				? ok == correctIds.size()
				: ok >= 1;
		return new ChildAnswerGrade(score, passed, lines);
	}
}

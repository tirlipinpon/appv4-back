package com.appv4.auth.application.service.question.play;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredItem;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

/**
 * Note un Ordre enfant : la liste envoyée doit suivre exactement l'ordre professeur.
 *
 * <p><b>Business :</b> Valider l'ordre — réussi seulement si chaque position est juste.
 * <p><b>Technique :</b> compare {@code orderedItemIds} à la liste {@code items} (sort_order).
 * <p><b>Métier / flow :</b> POST attempts → {@link com.appv4.auth.application.service.ChildQuestionPlayService}.
 */
public final class OrderChildAnswerGrader {

	public static final String FORM = "order";
	public static final String BAD_ANSWER = "Réponse invalide pour cette question.";

	private OrderChildAnswerGrader() {
	}

	public static ChildAnswerGrade grade(StoredQuestion question, List<Long> orderedItemIds) {
		List<StoredItem> items = question.items();
		if (items.isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		List<Long> submitted = orderedItemIds == null ? List.of() : orderedItemIds;
		if (submitted.size() != items.size()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Set<Long> known = items.stream().map(StoredItem::id).collect(Collectors.toSet());
		Set<Long> seen = new HashSet<>();
		for (Long id : submitted) {
			if (id == null || !known.contains(id) || !seen.add(id)) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
			}
		}
		int ok = 0;
		List<AnswerLine> lines = new ArrayList<>();
		for (int i = 0; i < items.size(); i++) {
			StoredItem expected = items.get(i);
			long chosen = submitted.get(i);
			boolean correct = chosen == expected.id();
			if (correct) {
				ok++;
			}
			lines.add(new AnswerLine("item", expected.id(), null, chosen, correct));
		}
		int score = (int) Math.round(100.0 * ok / items.size());
		boolean passed = ok == items.size();
		return new ChildAnswerGrade(score, passed, lines);
	}
}

package com.appv4.auth.application.service.question.play;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.PlacementAnswer;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredCard;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredColumn;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.service.TeacherQuestionService;

/**
 * Note un classement enfant : chaque carte doit être dans sa colonne juste.
 *
 * <p><b>Business :</b> score = % de cartes bien placées ; réussi seulement à 100 %.
 * <p><b>Technique :</b> compare {@code placements} aux {@code cards.correctColumnId}.
 * <p><b>Métier / flow :</b> POST attempts → {@link com.appv4.auth.application.service.ChildQuestionPlayService}.
 */
public final class ClassificationChildAnswerGrader {

	public static final String FORM = TeacherQuestionService.CLASSIFICATION;
	public static final String BAD_ANSWER = "Réponse invalide pour cette question.";

	private ClassificationChildAnswerGrader() {
	}

	public static ChildAnswerGrade grade(StoredQuestion question, List<PlacementAnswer> placements) {
		List<StoredCard> cards = question.cards();
		List<StoredColumn> columns = question.columns();
		if (cards.isEmpty() || columns.isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Set<Long> knownCards = cards.stream().map(StoredCard::id).collect(Collectors.toSet());
		Set<Long> knownColumns = columns.stream().map(StoredColumn::id).collect(Collectors.toSet());
		List<PlacementAnswer> submitted = placements == null ? List.of() : placements;
		if (submitted.size() != cards.size()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Map<Long, Long> chosenByCard = new HashMap<>();
		Set<Long> seenCards = new HashSet<>();
		for (PlacementAnswer placement : submitted) {
			if (placement == null
					|| !knownCards.contains(placement.cardId())
					|| !knownColumns.contains(placement.columnId())
					|| !seenCards.add(placement.cardId())) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
			}
			chosenByCard.put(placement.cardId(), placement.columnId());
		}
		int ok = 0;
		List<AnswerLine> lines = new ArrayList<>();
		for (StoredCard card : cards) {
			long chosen = chosenByCard.get(card.id());
			boolean correct = chosen == card.correctColumnId();
			if (correct) {
				ok++;
			}
			lines.add(new AnswerLine("card", card.id(), null, chosen, correct));
		}
		int score = (int) Math.round(100.0 * ok / cards.size());
		boolean passed = ok == cards.size();
		return new ChildAnswerGrade(score, passed, lines);
	}
}

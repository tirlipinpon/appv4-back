package com.appv4.auth.application.service.question.play;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPair;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

/**
 * Note un Memory enfant : chaque paire trouvée compte ; réussi si toutes le sont.
 *
 * <p><b>Business :</b> l'enfant retourne des cartes ; Valider envoie les paires marquées trouvées.
 * <p><b>Technique :</b> compare {@code matchedPairIds} à l'ensemble des paires de la question.
 * <p><b>Métier / flow :</b> POST attempts → {@link ChildQuestionPlayService} → ce grader.
 */
public final class MemoryChildAnswerGrader {

	public static final String FORM = "memory";
	public static final String BAD_ANSWER = "Réponse invalide pour cette question.";

	private MemoryChildAnswerGrader() {
	}

	public static ChildAnswerGrade grade(StoredQuestion question, List<Long> matchedPairIds) {
		List<StoredPair> pairs = question.pairs();
		if (pairs.isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Set<Long> known = pairs.stream().map(StoredPair::id).collect(Collectors.toSet());
		Set<Long> matched = new HashSet<>(matchedPairIds == null ? List.of() : matchedPairIds);
		if (!known.containsAll(matched)) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		int ok = 0;
		List<AnswerLine> lines = new ArrayList<>();
		for (StoredPair pair : pairs) {
			boolean correct = matched.contains(pair.id());
			if (correct) {
				ok++;
			}
			lines.add(new AnswerLine("pair", pair.id(), null, null, correct));
		}
		int score = (int) Math.round(100.0 * ok / pairs.size());
		boolean passed = ok == pairs.size();
		return new ChildAnswerGrade(score, passed, lines);
	}
}

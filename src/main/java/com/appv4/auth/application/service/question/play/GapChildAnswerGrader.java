package com.appv4.auth.application.service.question.play;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.GapFillAnswer;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredGapWord;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.service.question.CaseVideQuestionForm;
import com.appv4.auth.application.service.question.QuestionLabel;

/**
 * Note une Case vide : chaque trou doit recevoir le mot de même rang.
 *
 * <p><b>Business :</b> score = % de trous justes ; réussi seulement à 100 %.
 * <p><b>Technique :</b> le mot juste du trou {@code i} est {@code answers.get(i)}.
 * Deux jetons de même libellé sont interchangeables. La trace garde l'identifiant du jeton choisi.
 * <p><b>Métier / flow :</b> POST attempts → {@link com.appv4.auth.application.service.ChildQuestionPlayService}.
 */
public final class GapChildAnswerGrader {

	public static final String FORM = "case_vide";
	public static final String BAD_ANSWER = "Réponse invalide pour cette question.";

	private GapChildAnswerGrader() {
	}

	public static ChildAnswerGrade grade(StoredQuestion question, List<GapFillAnswer> fills) {
		List<StoredGapWord> answers = question.answers();
		if (answers.isEmpty() || CaseVideQuestionForm.countHoles(question.sentence()) != answers.size()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Map<Long, String> labels = new HashMap<>();
		for (StoredGapWord word : answers) {
			labels.put(word.id(), QuestionLabel.comparable(word.label()));
		}
		for (StoredGapWord word : question.decoys()) {
			labels.put(word.id(), QuestionLabel.comparable(word.label()));
		}
		List<GapFillAnswer> submitted = fills == null ? List.of() : fills;
		if (submitted.size() != answers.size()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Map<Integer, Long> chosenByHole = new HashMap<>();
		Set<Long> seenWords = new HashSet<>();
		for (GapFillAnswer fill : submitted) {
			if (fill == null
					|| fill.holeIndex() < 0
					|| fill.holeIndex() >= answers.size()
					|| chosenByHole.containsKey(fill.holeIndex())
					|| !labels.containsKey(fill.wordId())
					|| !seenWords.add(fill.wordId())) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
			}
			chosenByHole.put(fill.holeIndex(), fill.wordId());
		}
		int ok = 0;
		List<AnswerLine> lines = new ArrayList<>();
		for (int hole = 0; hole < answers.size(); hole++) {
			long chosen = chosenByHole.get(hole);
			boolean correct = labels.get(chosen).equals(labels.get(answers.get(hole).id()));
			if (correct) {
				ok++;
			}
			lines.add(new AnswerLine("gap", hole, null, chosen, correct));
		}
		int score = (int) Math.round(100.0 * ok / answers.size());
		boolean passed = ok == answers.size();
		return new ChildAnswerGrade(score, passed, lines);
	}

}

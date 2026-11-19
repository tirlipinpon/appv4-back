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
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.LinkMatchAnswer;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

/**
 * Note un Liens enfant : chaque mot doit être relié à sa propre réponse.
 *
 * <p><b>Business :</b> score = % d'associations justes ; réussi seulement à 100 %.
 * <p><b>Technique :</b> une association est juste quand {@code leftId} égale {@code rightId}
 * (les deux côtés d'un même lien portent le même id).
 * <p><b>Métier / flow :</b> POST attempts → {@link com.appv4.auth.application.service.ChildQuestionPlayService}.
 */
public final class LinkChildAnswerGrader {

	public static final String FORM = "liens";
	public static final String BAD_ANSWER = "Réponse invalide pour cette question.";

	private LinkChildAnswerGrader() {
	}

	public static ChildAnswerGrade grade(StoredQuestion question, List<LinkMatchAnswer> linkMatches) {
		List<StoredLink> links = question.links();
		if (links.size() < 2) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Set<Long> known = links.stream().map(StoredLink::id).collect(Collectors.toSet());
		List<LinkMatchAnswer> submitted = linkMatches == null ? List.of() : linkMatches;
		if (submitted.size() != links.size()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Map<Long, Long> chosenByLeft = new HashMap<>();
		Set<Long> seenLeft = new HashSet<>();
		Set<Long> seenRight = new HashSet<>();
		for (LinkMatchAnswer match : submitted) {
			if (match == null
					|| !known.contains(match.leftId())
					|| !known.contains(match.rightId())
					|| !seenLeft.add(match.leftId())
					|| !seenRight.add(match.rightId())) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
			}
			chosenByLeft.put(match.leftId(), match.rightId());
		}
		int ok = 0;
		List<AnswerLine> lines = new ArrayList<>();
		for (StoredLink link : links) {
			long chosen = chosenByLeft.get(link.id());
			boolean correct = chosen == link.id();
			if (correct) {
				ok++;
			}
			lines.add(new AnswerLine("link", link.id(), null, chosen, correct));
		}
		int score = (int) Math.round(100.0 * ok / links.size());
		boolean passed = ok == links.size();
		return new ChildAnswerGrade(score, passed, lines);
	}
}

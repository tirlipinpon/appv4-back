package com.appv4.auth.application.service.question.play;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.LinkMatchAnswer;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.LinkSelection;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.service.question.QuestionLabel;

/**
 * Note un Liens enfant : chaque mot doit être relié à sa propre réponse.
 *
 * <p><b>Business :</b> score = % d'associations justes ; réussi seulement à 100 %.
 * <p><b>Technique :</b> chaque combinaison de libellés attendue peut servir autant de fois qu'elle figure
 * dans la question. Chaque jeton physique ne peut être utilisé qu'une fois.
 * <p><b>Métier / flow :</b> POST attempts → {@link com.appv4.auth.application.service.ChildQuestionPlayService}.
 */
public final class LinkChildAnswerGrader {

	public static final String FORM = "liens";
	public static final String BAD_ANSWER = "Réponse invalide pour cette question.";

	private LinkChildAnswerGrader() {
	}

	public static ChildAnswerGrade gradeSelections(StoredQuestion question, List<LinkSelection> selections) {
		Map<UUID, Long> left = new HashMap<>();
		Map<UUID, Long> right = new HashMap<>();
		for (StoredLink link : question.links()) {
			left.put(AssociationTokens.forSide(FORM, link.id(), true), link.id());
			right.put(AssociationTokens.forSide(FORM, link.id(), false), link.id());
		}
		return grade(question, selections.stream().map(selection -> {
			Long leftId = left.get(selection.leftId());
			Long rightId = right.get(selection.rightId());
			if (leftId == null || rightId == null) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
			}
			return new LinkMatchAnswer(leftId, rightId);
		}).toList());
	}

	public static ChildAnswerGrade grade(StoredQuestion question, List<LinkMatchAnswer> linkMatches) {
		List<StoredLink> links = question.links();
		if (links.size() < 2) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Map<Long, StoredLink> byId = links.stream().collect(Collectors.toMap(StoredLink::id, link -> link));
		List<LinkMatchAnswer> submitted = linkMatches == null ? List.of() : linkMatches;
		if (submitted.size() != links.size()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Map<Long, Long> chosenByLeft = new HashMap<>();
		Set<Long> seenLeft = new HashSet<>();
		Set<Long> seenRight = new HashSet<>();
		for (LinkMatchAnswer match : submitted) {
			if (match == null
					|| !byId.containsKey(match.leftId())
					|| !byId.containsKey(match.rightId())
					|| !seenLeft.add(match.leftId())
					|| !seenRight.add(match.rightId())) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
			}
			chosenByLeft.put(match.leftId(), match.rightId());
		}
		Map<Labels, Integer> remaining = new HashMap<>();
		for (StoredLink link : links) {
			remaining.merge(labels(link, link), 1, Integer::sum);
		}
		int ok = 0;
		List<AnswerLine> lines = new ArrayList<>();
		for (StoredLink link : links) {
			long chosen = chosenByLeft.get(link.id());
			Labels match = labels(link, byId.get(chosen));
			boolean correct = remaining.getOrDefault(match, 0) > 0;
			if (correct) {
				remaining.computeIfPresent(match, (key, count) -> count - 1);
				ok++;
			}
			lines.add(new AnswerLine("link", link.id(), null, chosen, correct));
		}
		int score = (int) Math.round(100.0 * ok / links.size());
		boolean passed = ok == links.size();
		return new ChildAnswerGrade(score, passed, lines);
	}

	private static Labels labels(StoredLink left, StoredLink right) {
		return new Labels(QuestionLabel.comparable(left.leftLabel()), QuestionLabel.comparable(right.rightLabel()));
	}

	private record Labels(String left, String right) {
	}
}

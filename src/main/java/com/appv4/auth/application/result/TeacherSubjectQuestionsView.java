package com.appv4.auth.application.result;

import java.util.List;

import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoices;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPairs;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredItems;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredStatements;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredGaps;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredLinks;

/**
 * Questions déjà préparées dans une matière (SCRUM-174, SCRUM-177).
 *
 * <p><b>Business :</b> le professeur revoit l'énoncé, les propositions d'un
 * choix multiple, les paires d'un Memory, les éléments d'un Ordre,
 * ou les phrases d'un Vrai ou faux.
 * L'ordre enregistré est celui saisi.
 * <p><b>Technique :</b> {@code formCode} est le code choisi dans la liste des formes.
 * <p><b>Métier / flow :</b> résultat de {@code GET/POST /api/int/teacher/subjects/{id}/questions}.
 */
public record TeacherSubjectQuestionsView(List<QuestionLine> questions) {

	public record QuestionLine(
			long id,
			String formCode,
			String formLabel,
			String prompt,
			StoredContent content) {

		public List<ChoiceLine> choices() {
			return content instanceof StoredChoices value
					? value.choices().stream()
							.map(row -> new ChoiceLine(row.id(), row.label(), row.correct()))
							.toList() : List.of();
		}

		public List<PairLine> pairs() {
			return content instanceof StoredPairs value
					? value.pairs().stream()
							.map(row -> new PairLine(row.id(), row.questionSide(), row.answerSide()))
							.toList() : List.of();
		}

		public List<ItemLine> items() {
			return content instanceof StoredItems value
					? value.items().stream().map(row -> new ItemLine(row.id(), row.label())).toList() : List.of();
		}

		public List<StatementLine> statements() {
			return content instanceof StoredStatements value
					? value.statements().stream()
							.map(row -> new StatementLine(row.id(), row.label(), row.truthful()))
							.toList() : List.of();
		}

		public List<LinkLine> links() {
			return content instanceof StoredLinks value
					? value.links().stream()
							.map(row -> new LinkLine(row.id(), row.leftLabel(), row.rightLabel()))
							.toList() : List.of();
		}

		public String sentence() {
			return content instanceof StoredGaps value ? value.sentence() : "";
		}

		public List<GapWordLine> answers() {
			return content instanceof StoredGaps value
					? value.answers().stream().map(row -> new GapWordLine(row.id(), row.label())).toList()
					: List.of();
		}

		public List<GapWordLine> decoys() {
			return content instanceof StoredGaps value
					? value.decoys().stream().map(row -> new GapWordLine(row.id(), row.label())).toList()
					: List.of();
		}
	}

	public record ChoiceLine(long id, String label, boolean correct) {
	}

	public record PairLine(long id, String questionSide, String answerSide) {
	}

	public record ItemLine(long id, String label) {
	}

	public record StatementLine(long id, String label, boolean truthful) {
	}

	public record LinkLine(long id, String leftLabel, String rightLabel) {
	}

	public record GapWordLine(long id, String label) {
	}
}

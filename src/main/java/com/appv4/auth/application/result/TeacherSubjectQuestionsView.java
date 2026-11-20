package com.appv4.auth.application.result;

import java.util.List;

import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickPoint;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickImage;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPuzzle;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredSimon;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoices;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPairs;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredItems;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredStatements;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredFreeText;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClassification;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredGaps;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredLinks;
import com.appv4.auth.application.service.question.QuestionGamification;

/**
 * Questions déjà préparées dans une matière (SCRUM-174, SCRUM-177).
 *
 * <p><b>Business :</b> le professeur revoit l'énoncé, les propositions d'un
 * choix multiple, les paires d'un Memory, les éléments d'un Ordre,
 * les phrases d'un Vrai ou faux, ou le genre, les éléments et la suite d'un Simon.
 * L'ordre enregistré est celui saisi.
 * <p><b>Technique :</b> {@code formCode} est le code choisi dans la liste des formes.
 * <p><b>Métier / flow :</b> résultat de {@code GET/POST /api/int/teacher/subjects/{id}/questions}
 * et des copies renvoyées par {@code POST .../questions/clone}.
 */
public record TeacherSubjectQuestionsView(List<QuestionLine> questions, Long nextCursor) {

	public TeacherSubjectQuestionsView(List<QuestionLine> questions) {
		this(questions, null);
	}

	public record QuestionLine(
			long id,
			String formCode,
			String formLabel,
			String prompt,
			StoredContent content,
			QuestionGamification.Resolved gamification) {

		public QuestionLine(long id, String formCode, String formLabel, String prompt, StoredContent content) {
			this(id, formCode, formLabel, prompt, content, QuestionGamification.defaults());
		}

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
					? value.items().stream()
							.map(row -> new ItemLine(row.id(), row.label(), row.axisLabel()))
							.toList()
					: List.of();
		}

		/** Frise Ordre active. {@code null} hors forme {@code order}. */
		public Boolean timelineEnabled() {
			return content instanceof StoredItems value ? value.timelineEnabled() : null;
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

		public List<ColumnLine> columns() {
			return content instanceof StoredClassification value
					? value.columns().stream()
							.map(row -> new ColumnLine(row.id(), row.label()))
							.toList()
					: List.of();
		}

		public List<CardLine> cards() {
			return content instanceof StoredClassification value
					? value.cards().stream()
							.map(row -> new CardLine(row.id(), row.label(), row.correctColumnId()))
							.toList()
					: List.of();
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

		public String expectedAnswer() {
			return content instanceof StoredFreeText value ? value.expectedAnswer() : "";
		}

		public byte[] imageWebp() {
			if (content instanceof StoredClickImage click) {
				return click.imageWebp();
			}
			if (content instanceof StoredPuzzle puzzle) {
				return puzzle.imageWebp();
			}
			return new byte[0];
		}

		/** Mode de plateau du puzzle. Vide pour les autres formes. */
		public String boardMode() {
			return content instanceof StoredPuzzle puzzle ? puzzle.boardMode() : null;
		}

		/** Fond de jeu du puzzle (mode fond). Vide sinon. */
		public byte[] boardImageWebp() {
			if (!(content instanceof StoredPuzzle puzzle) || puzzle.boardImageWebp() == null) {
				return new byte[0];
			}
			return puzzle.boardImageWebp();
		}

		public Boolean requireAllCorrect() {
			return content instanceof StoredClickImage value ? value.requireAllCorrect() : null;
		}

		/** Clics ordonnés. {@code null} hors image à cliquer. */
		public Boolean clicksOrdered() {
			return content instanceof StoredClickImage value ? value.clicksOrdered() : null;
		}

		public List<ZoneLine> zones() {
			return content instanceof StoredClickImage value
					? value.zones().stream()
							.map(row -> new ZoneLine(
									row.id(),
									row.label(),
									row.correct(),
									row.shape(),
									row.points(),
									row.clickOrder()))
							.toList()
					: List.of();
		}

		public List<PieceLine> pieces() {
			return content instanceof StoredPuzzle value
					? value.pieces().stream()
							.map(row -> new PieceLine(
									row.id(),
									row.label(),
									row.shape(),
									row.points(),
									row.slotX(),
									row.slotY(),
									row.slotWidth()))
							.toList()
					: List.of();
		}

		/** Magnétisme du puzzle, en fraction de la largeur. Vide pour les autres formes. */
		public Double snapDistance() {
			return content instanceof StoredPuzzle value ? value.snapDistance() : null;
		}

		/** Genre des éléments Simon. {@code null} hors forme {@code simon}. */
		public String elementType() {
			return content instanceof StoredSimon value ? value.elementType() : null;
		}

		public List<SimonElementLine> elements() {
			return content instanceof StoredSimon value
					? value.elements().stream()
							.map(row -> new SimonElementLine(row.id(), row.label(), row.color()))
							.toList()
					: List.of();
		}

		/** Suite Simon : position 0-based de l'élément de chaque pas. Vide hors Simon. */
		/** Vide : la suite d'un Simon est tirée à la partie, pas enregistrée par le professeur. */
		public List<Integer> sequence() {
			return List.of();
		}
	}

	public record ChoiceLine(long id, String label, boolean correct) {
	}

	public record PairLine(long id, String questionSide, String answerSide) {
	}

	public record ItemLine(long id, String label, String axisLabel) {
		public ItemLine(long id, String label) {
			this(id, label, null);
		}
	}

	public record StatementLine(long id, String label, boolean truthful) {
	}

	public record LinkLine(long id, String leftLabel, String rightLabel) {
	}

	public record ColumnLine(long id, String label) {
	}

	public record CardLine(long id, String label, long correctColumnId) {
	}

	public record GapWordLine(long id, String label) {
	}

	/**
	 * Zone d'une image à cliquer. Les points sont en proportion de l'image.
	 * {@code clickOrder} est le rang du clic (1, 2, 3…) si la zone est juste, sinon null.
	 */
	public record ZoneLine(
			long id, String label, boolean correct, String shape, List<ClickPoint> points, Integer clickOrder) {
	}

	/**
	 * Pièce d'un puzzle. Les points sont en proportion de l'image. Pas de côté juste ou faux.
	 * {@code slotX}/{@code slotY} : emplacement cible (cadre blanc, fond dédié), sinon null.
	 * {@code slotWidth} : largeur sur le fond dédié, null = taille de la découpe.
	 */
	public record PieceLine(
			long id,
			String label,
			String shape,
			List<ClickPoint> points,
			Double slotX,
			Double slotY,
			Double slotWidth) {
	}

	/** Élément d'un Simon. {@code color} : code {@code #rrggbb} du genre couleurs, sinon null. */
	public record SimonElementLine(long id, String label, String color) {
	}
}

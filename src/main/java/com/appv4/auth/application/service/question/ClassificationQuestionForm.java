package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.CARDS_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.CARD_COLUMN_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.CARD_LABEL_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.CARD_LABEL_TOO_LONG;
import static com.appv4.auth.application.service.TeacherQuestionService.CLASSIFICATION;
import static com.appv4.auth.application.service.TeacherQuestionService.CLASSIFICATION_LABEL;
import static com.appv4.auth.application.service.TeacherQuestionService.CLASSIFICATION_ORDER;
import static com.appv4.auth.application.service.TeacherQuestionService.COLUMNS_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.COLUMN_EMPTY;
import static com.appv4.auth.application.service.TeacherQuestionService.COLUMN_LABEL_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.COLUMN_LABEL_TOO_LONG;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.CardInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ClassificationInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ColumnInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort.CardDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClassificationDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ColumnDraft;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;

/**
 * Règles de saisie de la forme classement (cartes → colonnes).
 *
 * <p><b>Business :</b> le professeur définit au moins deux colonnes et deux cartes ;
 * chaque carte a une colonne juste ; aucune colonne ne reste vide.
 * <p><b>Technique :</b> {@link ClassificationInput} avec {@code columnIndex} 0-based.
 * <p><b>Métier / flow :</b> {@link QuestionFormRegistry} → {@code TeacherQuestionService}.
 */
public final class ClassificationQuestionForm implements QuestionFormHandler {

	private static final int LABEL_MAX = 200;

	@Override
	public FormLine form() {
		return new FormLine(CLASSIFICATION, CLASSIFICATION_LABEL, CLASSIFICATION_ORDER);
	}

	@Override
	public Prepared prepare(String prompt, ContentInput input) {
		if (!(input instanceof ClassificationInput classification)) {
			throw new IllegalArgumentException("Entrée incompatible avec classement");
		}
		List<ColumnDraft> columns = requireColumns(classification.columns());
		List<CardDraft> cards = requireCards(classification.cards(), columns.size());
		return new Prepared(QuestionValidation.optionalPrompt(prompt), new ClassificationDraft(columns, cards));
	}

	private static List<ColumnDraft> requireColumns(List<ColumnInput> columns) {
		List<ColumnDraft> kept = new ArrayList<>();
		if (columns != null) {
			for (ColumnInput column : columns) {
				if (column == null) {
					continue;
				}
				String label = column.label() == null ? "" : column.label().trim();
				if (label.isEmpty()) {
					throw bad(COLUMN_LABEL_REQUIRED);
				}
				if (label.length() > LABEL_MAX) {
					throw bad(COLUMN_LABEL_TOO_LONG);
				}
				kept.add(new ColumnDraft(label));
			}
		}
		if (kept.size() < 2) {
			throw bad(COLUMNS_REQUIRED);
		}
		return List.copyOf(kept);
	}

	private static List<CardDraft> requireCards(List<CardInput> cards, int columnCount) {
		List<CardDraft> kept = new ArrayList<>();
		Set<Integer> usedColumns = new HashSet<>();
		if (cards != null) {
			for (CardInput card : cards) {
				if (card == null) {
					continue;
				}
				PieceFaces.Face face = PieceFaces.optional(card.label(), card.imageWebp(), LABEL_MAX, CARD_LABEL_TOO_LONG);
				if (face == null) {
					throw bad(CARD_LABEL_REQUIRED);
				}
				Integer index = card.columnIndex();
				if (index == null || index < 0 || index >= columnCount) {
					throw bad(CARD_COLUMN_REQUIRED);
				}
				usedColumns.add(index);
				kept.add(new CardDraft(face.label(), index, face.imageWebp()));
			}
		}
		if (kept.size() < 2) {
			throw bad(CARDS_REQUIRED);
		}
		if (usedColumns.size() < columnCount) {
			throw bad(COLUMN_EMPTY);
		}
		return List.copyOf(kept);
	}
}

package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PuzzleDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PuzzlePieceDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPuzzle;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPuzzlePiece;
import com.appv4.auth.application.service.TeacherQuestionService;

/**
 * Stockage du puzzle (SCRUM-183).
 *
 * <p><b>Business :</b> image de découpe, mode de plateau, fond optionnel, pièces.
 * <p><b>Technique :</b> lecture groupée image + pièces ; points via {@link ClickZonePoints}.
 * <p><b>Métier / flow :</b> le port des questions délègue ici dès que le code est {@code puzzle}.
 */
@Component
@Profile("!test")
public class PuzzleQuestionContentStore implements QuestionContentStore {

	private final BackSubjectQuestionPuzzleImageJpaRepository images;
	private final BackSubjectQuestionPuzzlePieceJpaRepository pieces;

	public PuzzleQuestionContentStore(
			BackSubjectQuestionPuzzleImageJpaRepository images,
			BackSubjectQuestionPuzzlePieceJpaRepository pieces) {
		this.images = images;
		this.pieces = pieces;
	}

	@Override
	public String formCode() {
		return TeacherQuestionService.PUZZLE;
	}

	@Override
	public Map<Long, StoredContent> load(List<Long> questionIds) {
		Map<Long, BackSubjectQuestionPuzzleImageEntity> imagesById = new HashMap<>();
		for (BackSubjectQuestionPuzzleImageEntity row : images.findByQuestionIdIn(questionIds)) {
			imagesById.put(row.getQuestionId(), row);
		}
		Map<Long, List<StoredPuzzlePiece>> piecesById = new HashMap<>();
		for (long id : questionIds) {
			piecesById.put(id, new ArrayList<>());
		}
		for (BackSubjectQuestionPuzzlePieceEntity row : pieces
				.findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(questionIds)) {
			piecesById.get(row.getQuestionId()).add(new StoredPuzzlePiece(
					row.getId(),
					row.getLabel(),
					row.getShape(),
					ClickZonePoints.decode(row.getPoints()),
					row.getSlotX(),
					row.getSlotY()));
		}
		Map<Long, StoredContent> contents = new HashMap<>();
		for (long id : questionIds) {
			BackSubjectQuestionPuzzleImageEntity image = imagesById.get(id);
			byte[] bytes = image == null ? new byte[0] : image.getImageWebp();
			String mode = image == null ? TeacherQuestionService.BOARD_MODE_DEFAULT : image.getBoardMode();
			byte[] board = image == null ? null : image.getBoardImageWebp();
			double snap = image == null ? TeacherQuestionService.SNAP_DEFAULT : image.getSnapDistance();
			contents.put(id, new StoredPuzzle(bytes, mode, board, snap, piecesById.get(id)));
		}
		return contents;
	}

	@Override
	public StoredContent write(long questionId, DraftContent content) {
		if (!(content instanceof PuzzleDraft draft)) {
			throw new IllegalArgumentException("Contenu incompatible avec le puzzle");
		}
		BackSubjectQuestionPuzzleImageEntity image = images.saveAndFlush(
				new BackSubjectQuestionPuzzleImageEntity(
						questionId,
						draft.imageWebp(),
						draft.boardMode(),
						draft.boardImageWebp(),
						draft.snapDistance()));
		List<BackSubjectQuestionPuzzlePieceEntity> rows = new ArrayList<>();
		for (PuzzlePieceDraft piece : draft.pieces()) {
			rows.add(new BackSubjectQuestionPuzzlePieceEntity(
					questionId,
					piece.label(),
					piece.shape(),
					ClickZonePoints.encode(piece.points()),
					piece.slotX(),
					piece.slotY(),
					rows.size()));
		}
		List<StoredPuzzlePiece> stored = storedPieces(pieces.saveAllAndFlush(rows));
		return new StoredPuzzle(
				image.getImageWebp(),
				image.getBoardMode(),
				image.getBoardImageWebp(),
				image.getSnapDistance(),
				stored);
	}

	@Override
	public void delete(long questionId) {
		List<BackSubjectQuestionPuzzlePieceEntity> current = pieces
				.findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(List.of(questionId));
		pieces.deleteAll(current);
		images.findById(questionId).ifPresent(images::delete);
		images.flush();
	}

	private static List<StoredPuzzlePiece> storedPieces(List<BackSubjectQuestionPuzzlePieceEntity> rows) {
		List<StoredPuzzlePiece> stored = new ArrayList<>();
		for (BackSubjectQuestionPuzzlePieceEntity row : rows) {
			stored.add(new StoredPuzzlePiece(
					row.getId(),
					row.getLabel(),
					row.getShape(),
					ClickZonePoints.decode(row.getPoints()),
					row.getSlotX(),
					row.getSlotY()));
		}
		return List.copyOf(stored);
	}
}

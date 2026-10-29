package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickImageDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickZoneDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickImage;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickZone;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.service.TeacherQuestionService;

/**
 * Stockage de l'image à cliquer (SCRUM-181).
 *
 * <p><b>Business :</b> une question de cette forme a une image et ses zones.
 * <p><b>Technique :</b> une lecture groupée de l'image, une lecture groupée des zones.
 * <p><b>Métier / flow :</b> le port des questions délègue ici dès que le code est {@code image_interactive}.
 */
@Component
@Profile("!test")
public class ClickImageQuestionContentStore implements QuestionContentStore {

	private final BackSubjectQuestionClickImageJpaRepository images;
	private final BackSubjectQuestionClickZoneJpaRepository zones;

	public ClickImageQuestionContentStore(
			BackSubjectQuestionClickImageJpaRepository images,
			BackSubjectQuestionClickZoneJpaRepository zones) {
		this.images = images;
		this.zones = zones;
	}

	@Override
	public String formCode() {
		return TeacherQuestionService.IMAGE_CLICK;
	}

	@Override
	public Map<Long, StoredContent> load(List<Long> questionIds) {
		Map<Long, BackSubjectQuestionClickImageEntity> imagesById = new HashMap<>();
		for (BackSubjectQuestionClickImageEntity row : images.findByQuestionIdIn(questionIds)) {
			imagesById.put(row.getQuestionId(), row);
		}
		Map<Long, List<StoredClickZone>> zonesById = new HashMap<>();
		for (long id : questionIds) {
			zonesById.put(id, new ArrayList<>());
		}
		for (BackSubjectQuestionClickZoneEntity row : zones.findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(questionIds)) {
			zonesById.get(row.getQuestionId()).add(new StoredClickZone(
					row.getId(),
					row.getLabel(),
					row.isCorrect(),
					row.getShape(),
					ClickZonePoints.decode(row.getPoints())));
		}
		Map<Long, StoredContent> contents = new HashMap<>();
		for (long id : questionIds) {
			BackSubjectQuestionClickImageEntity image = imagesById.get(id);
			if (image == null) {
				contents.put(id, new StoredClickImage(new byte[0], false, zonesById.get(id)));
			} else {
				contents.put(id, new StoredClickImage(
						image.getImageWebp(), image.isRequireAllCorrect(), zonesById.get(id)));
			}
		}
		return contents;
	}

	@Override
	public StoredContent write(long questionId, DraftContent content) {
		if (!(content instanceof ClickImageDraft draft)) {
			throw new IllegalArgumentException("Contenu incompatible avec l'image à cliquer");
		}
		BackSubjectQuestionClickImageEntity image = images.saveAndFlush(new BackSubjectQuestionClickImageEntity(
				questionId, draft.imageWebp(), draft.requireAllCorrect()));
		List<BackSubjectQuestionClickZoneEntity> rows = new ArrayList<>();
		for (ClickZoneDraft zone : draft.zones()) {
			rows.add(new BackSubjectQuestionClickZoneEntity(
					questionId,
					zone.label(),
					zone.correct(),
					zone.shape(),
					ClickZonePoints.encode(zone.points()),
					rows.size()));
		}
		List<StoredClickZone> stored = imagesZones(zones.saveAllAndFlush(rows));
		return new StoredClickImage(image.getImageWebp(), image.isRequireAllCorrect(), stored);
	}

	@Override
	public void delete(long questionId) {
		// Les lignes viennent d'être lues : elles sont dans la session.
		// Un DELETE groupé les laisserait en mémoire, et l'insert suivant
		// avec le même id plante (500) au moment de corriger la question.
		List<BackSubjectQuestionClickZoneEntity> current = zones
				.findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(List.of(questionId));
		zones.deleteAll(current);
		images.findById(questionId).ifPresent(images::delete);
		images.flush();
	}

	private static List<StoredClickZone> imagesZones(List<BackSubjectQuestionClickZoneEntity> rows) {
		List<StoredClickZone> stored = new ArrayList<>();
		for (BackSubjectQuestionClickZoneEntity row : rows) {
			stored.add(new StoredClickZone(
					row.getId(),
					row.getLabel(),
					row.isCorrect(),
					row.getShape(),
					ClickZonePoints.decode(row.getPoints())));
		}
		return List.copyOf(stored);
	}
}

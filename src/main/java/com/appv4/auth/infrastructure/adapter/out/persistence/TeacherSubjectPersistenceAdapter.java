package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.service.TeacherSubjectService;

/**
 * Persiste les matières et leurs accès professeurs (SCRUM-161, SCRUM-172).
 *
 * <p><b>Business :</b> une matière n'est lue ou modifiée que si le professeur y a
 * accès. Partager ajoute un accès. Déplacer remplace tous les accès.
 * <p><b>Technique :</b> {@code back_teacher_subjects} + {@code back_teacher_subject_access}.
 * Absent du profil {@code test} : les tests web ne démarrent pas JPA.
 * <p><b>Métier / flow :</b> adapter out du port {@link TeacherSubjectPort}.
 */
@Component
@Profile("!test")
public class TeacherSubjectPersistenceAdapter implements TeacherSubjectPort {

	private final BackTeacherSubjectJpaRepository subjects;
	private final BackTeacherSubjectAccessJpaRepository access;
	private final BackTeacherSubjectTransferRequestJpaRepository transfers;
	private final BackUserJpaRepository users;

	public TeacherSubjectPersistenceAdapter(
			BackTeacherSubjectJpaRepository subjects,
			BackTeacherSubjectAccessJpaRepository access,
			BackTeacherSubjectTransferRequestJpaRepository transfers,
			BackUserJpaRepository users) {
		this.subjects = subjects;
		this.access = access;
		this.transfers = transfers;
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredSubject> findByUser(long userId) {
		List<StoredSubject> rows = new ArrayList<>();
		for (BackTeacherSubjectAccessEntity link : access.findByUserIdOrderByIdAsc(userId)) {
			Optional<BackTeacherSubjectEntity> found = subjects.findById(link.getSubjectId());
			if (found.isEmpty() || found.get().isHidden()) {
				continue;
			}
			rows.add(toStored(found.get()));
		}
		return List.copyOf(rows);
	}

	@Override
	@Transactional
	public StoredSubject insert(long userId, long schoolId, String grade, String name, String nameKey) {
		BackTeacherSubjectEntity entity = new BackTeacherSubjectEntity(userId, schoolId, grade, name, nameKey);
		try {
			BackTeacherSubjectEntity saved = subjects.saveAndFlush(entity);
			access.saveAndFlush(new BackTeacherSubjectAccessEntity(saved.getId(), userId));
			return toStored(saved);
		} catch (DataIntegrityViolationException ex) {
			throw taken();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredSubject> findOwned(long userId, long subjectId) {
		return visible(userId, subjectId).map(TeacherSubjectPersistenceAdapter::toStored);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredSubject> findVisible(long subjectId) {
		Optional<BackTeacherSubjectEntity> found = subjects.findById(subjectId);
		if (found.isEmpty() || found.get().isHidden()) {
			return Optional.empty();
		}
		return found.map(TeacherSubjectPersistenceAdapter::toStored);
	}

	@Override
	@Transactional
	public Optional<StoredSubject> rename(long userId, long subjectId, String name, String nameKey) {
		Optional<BackTeacherSubjectEntity> found = visible(userId, subjectId);
		if (found.isEmpty()) {
			return Optional.empty();
		}
		BackTeacherSubjectEntity entity = found.get();
		entity.rename(name, nameKey);
		try {
			return Optional.of(toStored(subjects.saveAndFlush(entity)));
		} catch (DataIntegrityViolationException ex) {
			throw taken();
		}
	}

	@Override
	@Transactional
	public boolean deactivate(long userId, long subjectId) {
		return changeVisible(userId, subjectId, BackTeacherSubjectEntity::deactivate);
	}

	@Override
	@Transactional
	public boolean reactivate(long userId, long subjectId) {
		return changeVisible(userId, subjectId, BackTeacherSubjectEntity::reactivate);
	}

	@Override
	@Transactional
	public boolean hide(long userId, long subjectId) {
		Optional<BackTeacherSubjectEntity> found = subjects.findById(subjectId);
		if (found.isEmpty() || found.get().isHidden()) {
			return false;
		}
		if (access.deleteBySubjectIdAndUserId(subjectId, userId) == 0) {
			return false;
		}
		if (access.countBySubjectId(subjectId) == 0) {
			found.get().hide();
			subjects.flush();
		}
		return true;
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> otherAccessNames(long subjectId, long viewerUserId) {
		List<String> names = new ArrayList<>();
		Map<Long, String> byId = displayNames(access.findBySubjectIdOrderByIdAsc(subjectId));
		for (Map.Entry<Long, String> entry : byId.entrySet()) {
			if (entry.getKey() != viewerUserId) {
				names.add(entry.getValue());
			}
		}
		return sorted(names);
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> allAccessNames(long subjectId) {
		return sorted(new ArrayList<>(displayNames(access.findBySubjectIdOrderByIdAsc(subjectId)).values()));
	}

	@Override
	@Transactional
	public void addAccess(long subjectId, long userId) {
		if (access.existsBySubjectIdAndUserId(subjectId, userId)) {
			return;
		}
		access.saveAndFlush(new BackTeacherSubjectAccessEntity(subjectId, userId));
	}

	@Override
	@Transactional
	public void replaceAccessWith(long subjectId, long soleUserId) {
		Optional<BackTeacherSubjectEntity> found = subjects.findById(subjectId);
		if (found.isEmpty() || found.get().isHidden()) {
			return;
		}
		access.deleteBySubjectId(subjectId);
		access.saveAndFlush(new BackTeacherSubjectAccessEntity(subjectId, soleUserId));
		found.get().assignTo(soleUserId);
		subjects.flush();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasAccess(long subjectId, long userId) {
		return access.existsBySubjectIdAndUserId(subjectId, userId);
	}

	@Override
	@Transactional
	public StoredTransferRequest insertTransferRequest(
			long subjectId, long fromUserId, long toUserId, String mode) {
		BackTeacherSubjectTransferRequestEntity saved = transfers.saveAndFlush(
				new BackTeacherSubjectTransferRequestEntity(subjectId, fromUserId, toUserId, mode));
		return toTransfer(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasPendingTransfer(long subjectId, long toUserId) {
		return transfers.existsBySubjectIdAndToUserIdAndStatus(subjectId, toUserId, "pending");
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredTransferRequest> findPendingTransfersTo(long toUserId) {
		List<StoredTransferRequest> rows = new ArrayList<>();
		for (BackTeacherSubjectTransferRequestEntity row
				: transfers.findByToUserIdAndStatusOrderByIdAsc(toUserId, "pending")) {
			rows.add(toTransfer(row));
		}
		return List.copyOf(rows);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredTransferRequest> findPendingTransferForTarget(long requestId, long toUserId) {
		return transfers.findByIdAndToUserIdAndStatus(requestId, toUserId, "pending").map(
				TeacherSubjectPersistenceAdapter::toTransfer);
	}

	@Override
	@Transactional
	public void markTransferAccepted(long requestId) {
		transfers.findById(requestId).ifPresent(BackTeacherSubjectTransferRequestEntity::accept);
		transfers.flush();
	}

	@Override
	@Transactional
	public void markTransferRejected(long requestId) {
		transfers.findById(requestId).ifPresent(BackTeacherSubjectTransferRequestEntity::reject);
		transfers.flush();
	}

	@Override
	@Transactional
	public void cancelPendingTransfers(long subjectId, Long exceptRequestId) {
		for (BackTeacherSubjectTransferRequestEntity row
				: transfers.findBySubjectIdAndStatusOrderByIdAsc(subjectId, "pending")) {
			if (exceptRequestId != null && exceptRequestId.equals(row.getId())) {
				continue;
			}
			row.cancel();
		}
		transfers.flush();
	}

	private Optional<BackTeacherSubjectEntity> visible(long userId, long subjectId) {
		if (!access.existsBySubjectIdAndUserId(subjectId, userId)) {
			return Optional.empty();
		}
		Optional<BackTeacherSubjectEntity> found = subjects.findById(subjectId);
		if (found.isEmpty() || found.get().isHidden()) {
			return Optional.empty();
		}
		return found;
	}

	private boolean changeVisible(
			long userId,
			long subjectId,
			Consumer<BackTeacherSubjectEntity> change) {
		Optional<BackTeacherSubjectEntity> found = visible(userId, subjectId);
		if (found.isEmpty()) {
			return false;
		}
		change.accept(found.get());
		subjects.flush();
		return true;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean nameTaken(long schoolId, String grade, String nameKey, Long exceptId) {
		for (BackTeacherSubjectEntity row : subjects.findBySchoolIdAndGradeLabelOrderByIdAsc(schoolId, grade)) {
			if (row.isHidden() || !nameKey.equals(row.getNameKey())) {
				continue;
			}
			if (exceptId != null && exceptId.equals(row.getId())) {
				continue;
			}
			return true;
		}
		return false;
	}

	private Map<Long, String> displayNames(List<BackTeacherSubjectAccessEntity> links) {
		Map<Long, String> names = new HashMap<>();
		if (links.isEmpty()) {
			return names;
		}
		List<Long> ids = links.stream().map(BackTeacherSubjectAccessEntity::getUserId).distinct().toList();
		for (BackUserEntity user : users.findAllById(ids)) {
			names.put(user.getId(), user.getDisplayName() == null ? "" : user.getDisplayName());
		}
		for (Long id : ids) {
			names.putIfAbsent(id, "");
		}
		return names;
	}

	private static List<String> sorted(List<String> names) {
		Collator order = Collator.getInstance(Locale.FRENCH);
		order.setStrength(Collator.PRIMARY);
		names.sort(order);
		return List.copyOf(names);
	}

	private static StoredSubject toStored(BackTeacherSubjectEntity entity) {
		return new StoredSubject(
				entity.getId(),
				entity.getUserId(),
				entity.getSchoolId(),
				entity.getGradeLabel(),
				entity.getName(),
				entity.isActive());
	}

	private static StoredTransferRequest toTransfer(BackTeacherSubjectTransferRequestEntity entity) {
		return new StoredTransferRequest(
				entity.getId(),
				entity.getSubjectId(),
				entity.getFromUserId(),
				entity.getToUserId(),
				entity.getMode(),
				entity.getStatus());
	}

	private static AuthDomainException taken() {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST, TeacherSubjectService.NAME_TAKEN);
	}
}

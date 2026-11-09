package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort.ReusableSubject;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredSubject;
import com.appv4.auth.application.service.TeacherSubjectService;

/**
 * Persiste les matières et leurs accès professeurs (SCRUM-161, SCRUM-172).
 *
 * <p><b>Business :</b> une matière n'est lue ou modifiée que si le professeur y a
 * accès. Partager ajoute un accès. Déplacer remplace tous les accès. La
 * désactivation est par ligne d'accès ; le flag matière suit « au moins un actif ».
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
	private final BackTeacherSubjectDeactivateNoticeJpaRepository notices;
	private final BackUserJpaRepository users;

	public TeacherSubjectPersistenceAdapter(
			BackTeacherSubjectJpaRepository subjects,
			BackTeacherSubjectAccessJpaRepository access,
			BackTeacherSubjectTransferRequestJpaRepository transfers,
			BackTeacherSubjectDeactivateNoticeJpaRepository notices,
			BackUserJpaRepository users) {
		this.subjects = subjects;
		this.access = access;
		this.transfers = transfers;
		this.notices = notices;
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
			rows.add(toStored(found.get(), link.isActive()));
		}
		return List.copyOf(rows);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SubjectListing> findListing(long userId, List<Long> schoolIds, List<String> grades) {
		if (schoolIds.isEmpty() || grades.isEmpty()) {
			return List.of();
		}
		List<BackTeacherSubjectEntity> rows = new ArrayList<>();
		for (int start = 0; start < schoolIds.size(); start += 500) {
			for (int gradeStart = 0; gradeStart < grades.size(); gradeStart += 500) {
				rows.addAll(subjects.findBySchoolIdInAndGradeLabelInAndHiddenFalseOrderByIdAsc(
						schoolIds.subList(start, Math.min(start + 500, schoolIds.size())),
						grades.subList(gradeStart, Math.min(gradeStart + 500, grades.size()))));
			}
		}
		List<Long> ids = rows.stream().map(BackTeacherSubjectEntity::getId).toList();
		List<BackTeacherSubjectAccessEntity> links = new ArrayList<>();
		List<BackTeacherSubjectTransferRequestEntity> asks = new ArrayList<>();
		for (int start = 0; start < ids.size(); start += 500) {
			List<Long> batch = ids.subList(start, Math.min(start + 500, ids.size()));
			links.addAll(access.findBySubjectIdInOrderByIdAsc(batch));
			asks.addAll(transfers.findBySubjectIdInAndFromUserIdAndModeAndStatusOrderByIdAsc(
					batch, userId, "ask", "pending"));
		}
		Map<Long, List<BackTeacherSubjectAccessEntity>> linksBySubject = links.stream()
				.collect(Collectors.groupingBy(BackTeacherSubjectAccessEntity::getSubjectId));
		Map<Long, List<BackTeacherSubjectTransferRequestEntity>> asksBySubject = asks.stream()
				.collect(Collectors.groupingBy(BackTeacherSubjectTransferRequestEntity::getSubjectId));
		Set<Long> userIds = new HashSet<>();
		rows.forEach(row -> userIds.add(row.getUserId()));
		links.forEach(link -> userIds.add(link.getUserId()));
		asks.forEach(ask -> userIds.add(ask.getToUserId()));
		Map<Long, String> names = new HashMap<>();
		List<Long> people = new ArrayList<>(userIds);
		for (int start = 0; start < people.size(); start += 500) {
			users.findAllById(people.subList(start, Math.min(start + 500, people.size())))
					.forEach(user -> names.put(user.getId(),
							user.getDisplayName() == null ? "" : user.getDisplayName()));
		}
		Collator order = Collator.getInstance(Locale.FRENCH);
		order.setStrength(Collator.PRIMARY);
		List<SubjectListing> result = new ArrayList<>();
		for (BackTeacherSubjectEntity row : rows) {
			List<BackTeacherSubjectAccessEntity> holders = linksBySubject.getOrDefault(row.getId(), List.of());
			if (holders.isEmpty()) {
				continue;
			}
			BackTeacherSubjectAccessEntity own = holders.stream().filter(link -> link.getUserId() == userId)
					.findFirst().orElse(null);
			List<AccessHolder> others = holders.stream().filter(link -> link.getUserId() != userId)
					.map(link -> new AccessHolder(link.getUserId(), names.getOrDefault(link.getUserId(), "")))
					.sorted((a, b) -> order.compare(a.displayName(), b.displayName())).toList();
			List<AccessHolder> asked = asksBySubject.getOrDefault(row.getId(), List.of()).stream()
					.map(ask -> new AccessHolder(ask.getToUserId(),
							names.getOrDefault(ask.getToUserId(), ""))).toList();
			result.add(new SubjectListing(toStored(row, own == null ? row.isActive() : own.isActive()), own != null,
					others, asked, names.getOrDefault(row.getUserId(), "")));
		}
		return List.copyOf(result);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ReusableSubject> findReusable() {
		List<ReusableSubject> rows = new ArrayList<>();
		for (BackTeacherSubjectEntity row : subjects.findAllByOrderByIdAsc()) {
			rows.add(new ReusableSubject(
					row.getSchoolId(),
					row.getGradeLabel(),
					row.getName(),
					row.isHidden()));
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
			return toStored(saved, true);
		} catch (DataIntegrityViolationException ex) {
			throw taken();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredSubject> findOwned(long userId, long subjectId) {
		Optional<BackTeacherSubjectAccessEntity> link = access.findBySubjectIdAndUserId(subjectId, userId);
		if (link.isEmpty()) {
			return Optional.empty();
		}
		Optional<BackTeacherSubjectEntity> found = subjects.findById(subjectId);
		if (found.isEmpty() || found.get().isHidden()) {
			return Optional.empty();
		}
		return Optional.of(toStored(found.get(), link.get().isActive()));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredSubject> findVisible(long subjectId) {
		Optional<BackTeacherSubjectEntity> found = subjects.findById(subjectId);
		if (found.isEmpty() || found.get().isHidden()) {
			return Optional.empty();
		}
		return found.map(entity -> toStored(entity, entity.isActive()));
	}

	@Override
	@Transactional
	public Optional<StoredSubject> rename(long userId, long subjectId, String name, String nameKey) {
		Optional<StoredSubject> owned = findOwned(userId, subjectId);
		if (owned.isEmpty()) {
			return Optional.empty();
		}
		BackTeacherSubjectEntity entity = subjects.findById(subjectId).orElseThrow();
		entity.rename(name, nameKey);
		try {
			BackTeacherSubjectEntity saved = subjects.saveAndFlush(entity);
			return Optional.of(toStored(saved, owned.get().active()));
		} catch (DataIntegrityViolationException ex) {
			throw taken();
		}
	}

	@Override
	@Transactional
	public boolean deactivateAccesses(long actorUserId, long subjectId, List<Long> userIds) {
		if (findOwned(actorUserId, subjectId).isEmpty()) {
			return false;
		}
		List<Long> targets = userIds == null ? List.of() : userIds;
		for (Long target : targets) {
			if (target == null) {
				continue;
			}
			access.findBySubjectIdAndUserId(subjectId, target).ifPresent(link -> {
				link.deactivate();
				access.save(link);
			});
		}
		access.flush();
		syncSubjectActive(subjectId);
		return true;
	}

	@Override
	@Transactional
	public boolean reactivate(long userId, long subjectId) {
		Optional<BackTeacherSubjectAccessEntity> link = access.findBySubjectIdAndUserId(subjectId, userId);
		if (link.isEmpty()) {
			return false;
		}
		Optional<BackTeacherSubjectEntity> found = subjects.findById(subjectId);
		if (found.isEmpty() || found.get().isHidden()) {
			return false;
		}
		link.get().reactivate();
		access.flush();
		syncSubjectActive(subjectId);
		return true;
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
		// Le delete vide le contexte JPA : l'entité lue avant est détachée.
		// On relit, sinon hidden ne serait pas enregistré et le nom resterait pris.
		if (access.countBySubjectId(subjectId) == 0) {
			Optional<BackTeacherSubjectEntity> again = subjects.findById(subjectId);
			if (again.isEmpty()) {
				return false;
			}
			again.get().hide();
			subjects.saveAndFlush(again.get());
		} else {
			// Invariant : le propriétaire a toujours un accès. Sinon → orphelin (QA #163).
			Optional<BackTeacherSubjectEntity> again = subjects.findById(subjectId);
			if (again.isPresent()) {
				BackTeacherSubjectEntity subject = again.get();
				if (!access.existsBySubjectIdAndUserId(subjectId, subject.getUserId())) {
					List<BackTeacherSubjectAccessEntity> remaining =
							access.findBySubjectIdOrderByIdAsc(subjectId);
					if (!remaining.isEmpty()) {
						subject.assignTo(remaining.get(0).getUserId());
						subjects.flush();
					}
				}
			}
			syncSubjectActive(subjectId);
		}
		return true;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredSubject> findRemovedByName(long schoolId, String grade, String nameKey) {
		return subjects.findRemovedByName(schoolId, grade, nameKey, PageRequest.of(0, 1)).stream()
				.findFirst().map(row -> toStored(row, false));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredSubject> findVisibleByName(long schoolId, String grade, String nameKey) {
		return subjects.findFirstBySchoolIdAndGradeLabelAndNameKeyAndHiddenFalseOrderByIdAsc(schoolId, grade, nameKey)
				.map(row -> toStored(row, row.isActive()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredSubject> findVisibleInClass(long schoolId, String grade) {
		List<StoredSubject> rows = new ArrayList<>();
		for (BackTeacherSubjectEntity row : subjects.findBySchoolIdAndGradeLabelOrderByIdAsc(schoolId, grade)) {
			if (row.isHidden()) {
				continue;
			}
			rows.add(toStored(row, row.isActive()));
		}
		return List.copyOf(rows);
	}

	@Override
	@Transactional
	public Optional<StoredSubject> restore(long userId, long subjectId) {
		Optional<BackTeacherSubjectEntity> found = subjects.findById(subjectId);
		if (found.isEmpty()) {
			return Optional.empty();
		}
		BackTeacherSubjectEntity entity = found.get();
		boolean noAccess = access.countBySubjectId(subjectId) == 0;
		if (!entity.isHidden() && !noAccess) {
			return Optional.empty();
		}
		if (entity.isHidden()) {
			entity.unhide();
		}
		entity.reactivate();
		subjects.saveAndFlush(entity);
		Optional<BackTeacherSubjectAccessEntity> link = access.findBySubjectIdAndUserId(subjectId, userId);
		if (link.isEmpty()) {
			access.saveAndFlush(new BackTeacherSubjectAccessEntity(subjectId, userId));
		} else if (!link.get().isActive()) {
			link.get().reactivate();
			access.flush();
		}
		if (entity.getUserId() != userId) {
			entity.assignTo(userId);
			subjects.flush();
		}
		syncSubjectActive(subjectId);
		return Optional.of(toStored(entity, true));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Long> accessUserIds(long subjectId) {
		return access.findBySubjectIdOrderByIdAsc(subjectId).stream()
				.map(BackTeacherSubjectAccessEntity::getUserId)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<AccessHolder> otherAccessHolders(long subjectId, long viewerUserId) {
		List<AccessHolder> holders = new ArrayList<>();
		Map<Long, String> byId = displayNames(access.findBySubjectIdOrderByIdAsc(subjectId));
		for (Map.Entry<Long, String> entry : byId.entrySet()) {
			if (entry.getKey() != viewerUserId) {
				holders.add(new AccessHolder(entry.getKey(), entry.getValue()));
			}
		}
		Collator order = Collator.getInstance(Locale.FRENCH);
		order.setStrength(Collator.PRIMARY);
		holders.sort((left, right) -> order.compare(left.displayName(), right.displayName()));
		return List.copyOf(holders);
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> otherAccessNames(long subjectId, long viewerUserId) {
		return otherAccessHolders(subjectId, viewerUserId).stream()
				.map(AccessHolder::displayName)
				.toList();
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
		BackTeacherSubjectEntity subject = subjects.findById(subjectId).orElseThrow();
		subject.assignTo(soleUserId);
		if (!subject.isActive()) {
			subject.reactivate();
		}
		subjects.flush();
	}

	@Override
	@Transactional
	public void assignOwnership(long subjectId, long newOwnerUserId) {
		Optional<BackTeacherSubjectEntity> found = subjects.findById(subjectId);
		if (found.isEmpty() || found.get().isHidden()) {
			return;
		}
		found.get().assignTo(newOwnerUserId);
		subjects.flush();
	}

	@Override
	@Transactional
	public void hideCompletely(long subjectId) {
		Optional<BackTeacherSubjectEntity> found = subjects.findById(subjectId);
		if (found.isEmpty() || found.get().isHidden()) {
			return;
		}
		access.deleteBySubjectId(subjectId);
		access.flush();
		BackTeacherSubjectEntity entity = subjects.findById(subjectId).orElseThrow();
		entity.hide();
		subjects.saveAndFlush(entity);
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
	public boolean hasPendingTransferBetween(long subjectId, long fromUserId, long toUserId) {
		return transfers.existsBySubjectIdAndFromUserIdAndToUserIdAndStatus(
				subjectId, fromUserId, toUserId, "pending");
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredTransferRequest> findPendingAsksFrom(long subjectId, long fromUserId) {
		List<StoredTransferRequest> rows = new ArrayList<>();
		for (BackTeacherSubjectTransferRequestEntity row : transfers
				.findBySubjectIdAndFromUserIdAndModeAndStatusOrderByIdAsc(
						subjectId, fromUserId, "ask", "pending")) {
			rows.add(toTransfer(row));
		}
		return List.copyOf(rows);
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

	@Override
	@Transactional
	public void insertDeactivateNotices(
			long subjectId, long fromUserId, List<Long> toUserIds, List<Long> deactivatedUserIds) {
		String encoded = encodeIds(deactivatedUserIds);
		for (Long toUserId : toUserIds) {
			if (toUserId == null || toUserId == fromUserId) {
				continue;
			}
			notices.save(new BackTeacherSubjectDeactivateNoticeEntity(
					subjectId,
					fromUserId,
					toUserId,
					encoded,
					BackTeacherSubjectDeactivateNoticeEntity.KIND_DEACTIVATE,
					null));
		}
		notices.flush();
	}

	@Override
	@Transactional
	public void insertSubjectNotice(
			long subjectId, long fromUserId, long toUserId, String kind, String message) {
		notices.save(new BackTeacherSubjectDeactivateNoticeEntity(
				subjectId, fromUserId, toUserId, "", kind, message));
		notices.flush();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Long> rejectedHandoverToUserIds(long subjectId, long fromUserId) {
		List<Long> ids = new ArrayList<>();
		for (BackTeacherSubjectTransferRequestEntity row : transfers
				.findBySubjectIdAndFromUserIdAndModeAndStatusOrderByIdAsc(
						subjectId, fromUserId, "handover", "rejected")) {
			ids.add(row.getToUserId());
		}
		return List.copyOf(ids);
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredDeactivateNotice> findOpenDeactivateNoticesTo(long toUserId) {
		List<StoredDeactivateNotice> rows = new ArrayList<>();
		for (BackTeacherSubjectDeactivateNoticeEntity row
				: notices.findByToUserIdAndDismissedAtIsNullOrderByIdAsc(toUserId)) {
			rows.add(toNotice(row));
		}
		return List.copyOf(rows);
	}

	@Override
	@Transactional
	public boolean dismissDeactivateNotice(long noticeId, long toUserId) {
		Optional<BackTeacherSubjectDeactivateNoticeEntity> found =
				notices.findByIdAndToUserIdAndDismissedAtIsNull(noticeId, toUserId);
		if (found.isEmpty()) {
			return false;
		}
		found.get().dismiss();
		notices.flush();
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

	private void syncSubjectActive(long subjectId) {
		Optional<BackTeacherSubjectEntity> found = subjects.findById(subjectId);
		if (found.isEmpty()) {
			return;
		}
		boolean anyActive = false;
		for (BackTeacherSubjectAccessEntity link : access.findBySubjectIdOrderByIdAsc(subjectId)) {
			if (link.isActive()) {
				anyActive = true;
				break;
			}
		}
		BackTeacherSubjectEntity entity = found.get();
		if (anyActive) {
			entity.reactivate();
		} else {
			entity.deactivate();
		}
		subjects.flush();
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

	private static StoredSubject toStored(BackTeacherSubjectEntity entity, boolean active) {
		return new StoredSubject(
				entity.getId(),
				entity.getUserId(),
				entity.getSchoolId(),
				entity.getGradeLabel(),
				entity.getName(),
				active);
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

	private static StoredDeactivateNotice toNotice(BackTeacherSubjectDeactivateNoticeEntity entity) {
		String kind = entity.getKind();
		if (kind == null || kind.isBlank()) {
			kind = BackTeacherSubjectDeactivateNoticeEntity.KIND_DEACTIVATE;
		}
		String message = entity.getMessage();
		return new StoredDeactivateNotice(
				entity.getId(),
				entity.getSubjectId(),
				entity.getFromUserId(),
				entity.getToUserId(),
				kind,
				message == null ? "" : message,
				decodeIds(entity.getDeactivatedUserIds()));
	}

	private static String encodeIds(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return "";
		}
		return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
	}

	private static List<Long> decodeIds(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		List<Long> ids = new ArrayList<>();
		for (String part : raw.split(",")) {
			String trimmed = part.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			ids.add(Long.parseLong(trimmed));
		}
		return List.copyOf(ids);
	}

	private static AuthDomainException taken() {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST, TeacherSubjectService.NAME_TAKEN);
	}
}

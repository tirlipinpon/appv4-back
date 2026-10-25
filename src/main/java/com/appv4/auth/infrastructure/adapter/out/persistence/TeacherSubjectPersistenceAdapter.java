package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.service.TeacherSubjectService;

/**
 * Persiste les matières du professeur (SCRUM-161).
 *
 * <p><b>Business :</b> seules les matières de ce professeur sont lues ou modifiées.
 * Un nom déjà pris dans la même classe ne crée pas une seconde ligne.
 * <p><b>Technique :</b> {@code back_teacher_subjects}. {@code origin_subject_id} n'est
 * pas rempli. Absent du profil {@code test} : les tests web ne démarrent pas JPA.
 * <p><b>Métier / flow :</b> adapter out du port {@link TeacherSubjectPort}.
 */
@Component
@Profile("!test")
public class TeacherSubjectPersistenceAdapter implements TeacherSubjectPort {

	private final BackTeacherSubjectJpaRepository repository;

	public TeacherSubjectPersistenceAdapter(BackTeacherSubjectJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredSubject> findByUser(long userId) {
		List<StoredSubject> rows = new ArrayList<>();
		for (BackTeacherSubjectEntity entity : repository.findByUserIdOrderByIdAsc(userId)) {
			rows.add(toStored(entity));
		}
		return List.copyOf(rows);
	}

	@Override
	@Transactional
	public StoredSubject insert(long userId, long schoolId, String grade, String name, String nameKey) {
		BackTeacherSubjectEntity entity = new BackTeacherSubjectEntity(userId, schoolId, grade, name, nameKey);
		try {
			return toStored(repository.saveAndFlush(entity));
		} catch (DataIntegrityViolationException ex) {
			throw taken();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredSubject> findOwned(long userId, long subjectId) {
		return repository.findByIdAndUserId(subjectId, userId).map(TeacherSubjectPersistenceAdapter::toStored);
	}

	@Override
	@Transactional
	public Optional<StoredSubject> rename(long userId, long subjectId, String name, String nameKey) {
		Optional<BackTeacherSubjectEntity> found = repository.findByIdAndUserId(subjectId, userId);
		if (found.isEmpty()) {
			return Optional.empty();
		}
		BackTeacherSubjectEntity entity = found.get();
		entity.rename(name, nameKey);
		try {
			return Optional.of(toStored(repository.saveAndFlush(entity)));
		} catch (DataIntegrityViolationException ex) {
			throw taken();
		}
	}

	@Override
	@Transactional
	public boolean delete(long userId, long subjectId) {
		Optional<BackTeacherSubjectEntity> found = repository.findByIdAndUserId(subjectId, userId);
		if (found.isEmpty()) {
			return false;
		}
		repository.delete(found.get());
		repository.flush();
		return true;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean nameTaken(long userId, long schoolId, String grade, String nameKey, Long exceptId) {
		for (BackTeacherSubjectEntity row : repository.findByUserIdOrderByIdAsc(userId)) {
			if (row.getSchoolId() != schoolId || !grade.equals(row.getGradeLabel())) {
				continue;
			}
			if (!nameKey.equals(row.getNameKey())) {
				continue;
			}
			if (exceptId != null && exceptId.equals(row.getId())) {
				continue;
			}
			return true;
		}
		return false;
	}

	private static StoredSubject toStored(BackTeacherSubjectEntity entity) {
		return new StoredSubject(
				entity.getId(),
				entity.getUserId(),
				entity.getSchoolId(),
				entity.getGradeLabel(),
				entity.getName());
	}

	private static AuthDomainException taken() {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST, TeacherSubjectService.NAME_TAKEN);
	}
}

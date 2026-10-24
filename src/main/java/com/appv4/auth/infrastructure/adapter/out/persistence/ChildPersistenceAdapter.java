package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.ChildPort;

/**
 * Persiste les enfants du parent (SCRUM-156).
 *
 * <p><b>Business :</b> la fiche est liée au parent connecté. Le PIN reste hashé.
 * <p><b>Technique :</b> {@code back_children}. Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out du port {@link ChildPort}.
 */
@Component
@Profile("!test")
public class ChildPersistenceAdapter implements ChildPort {

	private final BackChildJpaRepository childRepository;

	public ChildPersistenceAdapter(BackChildJpaRepository childRepository) {
		this.childRepository = childRepository;
	}

	@Override
	@Transactional
	public ChildSaved insert(NewChild child) {
		BackChildEntity saved = childRepository.save(new BackChildEntity(
				child.parentUserId(),
				child.firstName(),
				child.lastName(),
				child.age(),
				child.curriculum(),
				child.grade(),
				child.schoolId(),
				child.avatarCode(),
				child.photo(),
				child.photoContentType(),
				child.pinHash()));
		return toSaved(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ChildSaved> findByParent(long parentUserId) {
		return childRepository.findByParentUserIdOrderByIdAsc(parentUserId).stream()
				.map(ChildPersistenceAdapter::toSaved)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ChildPhoto> findPhoto(long parentUserId, long childId) {
		return childRepository.findByIdAndParentUserId(childId, parentUserId)
				.filter(child -> child.getPhoto() != null)
				.map(child -> new ChildPhoto(child.getPhoto(), child.getPhotoContentType()));
	}

	private static ChildSaved toSaved(BackChildEntity child) {
		return new ChildSaved(
				child.getId(),
				child.getParentUserId(),
				child.getFirstName(),
				child.getLastName(),
				child.getAge(),
				child.getCurriculum(),
				child.getGradeLabel(),
				child.getSchoolId(),
				child.getAvatarCode(),
				child.getPhoto() != null);
	}
}

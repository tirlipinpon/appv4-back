package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.ChildPort;

/**
 * Persiste les enfants du parent (SCRUM-156).
 *
 * <p><b>Business :</b> la fiche est liée au parent connecté. Le PIN reste hashé.
 * La liste montre les fiches encore affichées, actives ou désactivées.
 * Masquer une fiche ne supprime pas la ligne.
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
		return childRepository.findByParentUserIdAndHiddenFalseOrderByIdAsc(parentUserId).stream()
				.map(ChildPersistenceAdapter::toSaved)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ChildSaved> findOwned(long parentUserId, long childId) {
		return childRepository.findByIdAndParentUserIdAndHiddenFalse(childId, parentUserId)
				.map(ChildPersistenceAdapter::toSaved);
	}

	@Override
	@Transactional
	public Optional<ChildSaved> update(long parentUserId, long childId, ChildChanges changes) {
		return childRepository.findByIdAndParentUserIdAndHiddenFalse(childId, parentUserId)
				.map(entity -> {
					entity.applyChanges(
							changes.firstName(),
							changes.lastName(),
							changes.age(),
							changes.curriculum(),
							changes.grade(),
							changes.schoolId(),
							changes.replaceImage(),
							changes.avatarCode(),
							changes.photo(),
							changes.photoContentType(),
							changes.pinHash());
					return toSaved(entity);
				});
	}

	@Override
	@Transactional
	public boolean deactivate(long parentUserId, long childId) {
		return changeListed(parentUserId, childId, BackChildEntity::deactivate);
	}

	@Override
	@Transactional
	public boolean reactivate(long parentUserId, long childId) {
		return changeListed(parentUserId, childId, BackChildEntity::reactivate);
	}

	@Override
	@Transactional
	public boolean hide(long parentUserId, long childId) {
		return changeListed(parentUserId, childId, BackChildEntity::hide);
	}

	private boolean changeListed(long parentUserId, long childId, Consumer<BackChildEntity> change) {
		Optional<BackChildEntity> owned = childRepository.findByIdAndParentUserIdAndHiddenFalse(childId, parentUserId);
		if (owned.isEmpty()) {
			return false;
		}
		change.accept(owned.get());
		return true;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ChildPhoto> findPhoto(long parentUserId, long childId) {
		return childRepository.findByIdAndParentUserIdAndHiddenFalse(childId, parentUserId)
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
				child.getPhoto() != null,
				child.isActive());
	}
}

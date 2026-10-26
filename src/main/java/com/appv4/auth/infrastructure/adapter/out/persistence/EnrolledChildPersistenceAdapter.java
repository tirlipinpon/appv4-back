package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.EnrolledChildPort;

/**
 * Lit les enfants encore proposés, pour une classe de professeur (SCRUM-160).
 *
 * <p><b>Business :</b> une fiche masquée ou désactivée ne s'affiche pas.
 * Le code PIN et le parent ne sont pas recopiés.
 * <p><b>Technique :</b> {@code back_children}. Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out du port {@link EnrolledChildPort}.
 */
@Component
@Profile("!test")
public class EnrolledChildPersistenceAdapter implements EnrolledChildPort {

	private final BackChildJpaRepository childRepository;

	public EnrolledChildPersistenceAdapter(BackChildJpaRepository childRepository) {
		this.childRepository = childRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<EnrolledChild> findActiveInSchools(Collection<Long> schoolIds) {
		if (schoolIds == null || schoolIds.isEmpty()) {
			return List.of();
		}
		return childRepository.findByHiddenFalseAndActiveTrueAndSchoolIdIn(schoolIds).stream()
				.map(EnrolledChildPersistenceAdapter::toEnrolled)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<EnrolledChild> findActive(long childId) {
		return childRepository.findByIdAndHiddenFalseAndActiveTrueAndSchoolIdIsNotNull(childId)
				.map(EnrolledChildPersistenceAdapter::toEnrolled);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<EnrolledPhoto> findPhoto(long childId) {
		return childRepository.findByIdAndHiddenFalseAndActiveTrueAndSchoolIdIsNotNull(childId)
				.filter(child -> child.getPhoto() != null)
				.map(child -> new EnrolledPhoto(child.getPhoto(), child.getPhotoContentType()));
	}

	private static EnrolledChild toEnrolled(BackChildEntity child) {
		return new EnrolledChild(
				child.getId(),
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

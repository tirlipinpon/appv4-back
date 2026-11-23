package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.service.TeacherProfileService;

/**
 * Persiste le nom et les écoles du professeur (SCRUM-159).
 *
 * <p><b>Business :</b> seul le nom affiché du compte change. L'email, le mot de passe,
 * les rôles et la session restent. Les écoles d'un autre professeur ne sont pas lues.
 * <p><b>Technique :</b> {@code back_users.display_name} sans incrémenter
 * {@code security_version}. Rattachements dans {@code back_teacher_schools}.
 * Absent du profil {@code test} : les tests web ne démarrent pas JPA.
 * <p><b>Métier / flow :</b> adapter out du port {@link TeacherProfilePort}.
 */
@Component
@Profile("!test")
public class TeacherProfilePersistenceAdapter implements TeacherProfilePort {

	private final BackUserJpaRepository userRepository;
	private final BackTeacherSchoolJpaRepository schoolRepository;
	private final BackTeacherSchoolGradeJpaRepository gradeRepository;

	public TeacherProfilePersistenceAdapter(
			BackUserJpaRepository userRepository,
			BackTeacherSchoolJpaRepository schoolRepository,
			BackTeacherSchoolGradeJpaRepository gradeRepository) {
		this.userRepository = userRepository;
		this.schoolRepository = schoolRepository;
		this.gradeRepository = gradeRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<TeacherAccount> findAccount(long userId) {
		return userRepository.findById(userId)
				.map(user -> new TeacherAccount(
						user.getEmail(),
						user.getDisplayName(),
						user.getContactEmail(),
						user.getPhone(),
						user.getAvatarCode()));
	}

	@Override
	@Transactional
	public void updateContact(
			long userId,
			String displayName,
			String contactEmail,
			String phone,
			String avatarCode) {
		BackUserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new AuthDomainException(
						AuthErrorCode.NOT_FOUND, TeacherProfileService.ACCOUNT_MISSING));
		user.setDisplayName(displayName);
		user.setContactEmail(contactEmail);
		user.setPhone(phone);
		user.setAvatarCode(avatarCode);
	}

	@Override
	@Transactional
	public void updateDisplayName(long userId, String displayName) {
		BackUserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new AuthDomainException(
						AuthErrorCode.NOT_FOUND, TeacherProfileService.ACCOUNT_MISSING));
		user.setDisplayName(displayName);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TeacherSchoolLink> findSchools(long userId) {
		List<BackTeacherSchoolEntity> rows = schoolRepository.findByUserIdOrderByIdAsc(userId);
		if (rows.isEmpty()) {
			return List.of();
		}
		List<Long> ids = rows.stream().map(BackTeacherSchoolEntity::getId).toList();
		Map<Long, List<String>> gradesByLink = new LinkedHashMap<>();
		for (BackTeacherSchoolGradeEntity grade : gradeRepository.findByTeacherSchoolIdInOrderByIdAsc(ids)) {
			gradesByLink.computeIfAbsent(grade.getTeacherSchoolId(), ignored -> new ArrayList<>())
					.add(grade.getGradeLabel());
		}
		List<TeacherSchoolLink> links = new ArrayList<>();
		for (BackTeacherSchoolEntity row : rows) {
			links.add(new TeacherSchoolLink(
					row.getSchoolId(),
					row.getCurriculum(),
					gradesByLink.getOrDefault(row.getId(), List.of())));
		}
		return List.copyOf(links);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Long> findUserIdsWithClass(long schoolId, String grade) {
		return List.copyOf(schoolRepository.findUserIdsWithClass(schoolId, grade));
	}

	@Override
	@Transactional
	public void replaceSchools(long userId, List<TeacherSchoolLink> schools) {
		List<BackTeacherSchoolEntity> existing = schoolRepository.findByUserIdOrderByIdAsc(userId);
		if (!existing.isEmpty()) {
			List<Long> ids = existing.stream().map(BackTeacherSchoolEntity::getId).toList();
			gradeRepository.deleteByTeacherSchoolIdIn(ids);
			schoolRepository.deleteByUserId(userId);
		}
		if (schools == null || schools.isEmpty()) {
			return;
		}
		for (TeacherSchoolLink link : schools) {
			BackTeacherSchoolEntity saved = schoolRepository.saveAndFlush(
					new BackTeacherSchoolEntity(userId, link.schoolId(), link.curriculum()));
			for (String grade : link.grades()) {
				gradeRepository.save(new BackTeacherSchoolGradeEntity(saved.getId(), grade));
			}
		}
	}
}

package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.ChildSubjectPort;

/**
 * Persiste les matières choisies pour un enfant (SCRUM-162, SCRUM-172).
 *
 * <p><b>Business :</b> seules les matières actives et non cachées sont lues.
 * Le nom de professeur énumère tous ceux qui y ont accès. Retirer un lien ne
 * fait pas un delete de {@code back_teacher_subjects}.
 * <p><b>Technique :</b> {@code back_child_subject_links} + accès matières.
 * Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out du port {@link ChildSubjectPort}.
 */
@Component
@Profile("!test")
public class ChildSubjectPersistenceAdapter implements ChildSubjectPort {

	private final BackChildSubjectLinkJpaRepository links;
	private final BackTeacherSubjectJpaRepository subjects;
	private final BackTeacherSubjectAccessJpaRepository access;
	private final BackUserJpaRepository users;

	public ChildSubjectPersistenceAdapter(
			BackChildSubjectLinkJpaRepository links,
			BackTeacherSubjectJpaRepository subjects,
			BackTeacherSubjectAccessJpaRepository access,
			BackUserJpaRepository users) {
		this.links = links;
		this.subjects = subjects;
		this.access = access;
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public List<CatalogSubject> visibleSubjects() {
		List<BackTeacherSubjectEntity> rows = subjects.findByHiddenFalseAndActiveTrueOrderByIdAsc();
		Map<Long, String> teacherNames = teacherNamesBySubject(rows);
		List<CatalogSubject> catalog = new ArrayList<>();
		for (BackTeacherSubjectEntity row : rows) {
			catalog.add(new CatalogSubject(
					row.getId(),
					row.getName(),
					teacherNames.getOrDefault(row.getId(), ""),
					row.getSchoolId(),
					row.getGradeLabel()));
		}
		return List.copyOf(catalog);
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredLink> linksOf(long childId) {
		List<StoredLink> rows = new ArrayList<>();
		for (BackChildSubjectLinkEntity link : links.findByChildIdOrderByIdAsc(childId)) {
			rows.add(new StoredLink(link.getSubjectId(), link.isExcluded()));
		}
		return List.copyOf(rows);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CountedLink> visibleLinksForParent(long parentUserId) {
		List<BackChildSubjectLinkEntity> rows = links.findForParent(parentUserId);
		Map<Long, BackTeacherSubjectEntity> byId = subjectsById(rows);
		List<CountedLink> counted = new ArrayList<>();
		for (BackChildSubjectLinkEntity link : rows) {
			BackTeacherSubjectEntity subject = byId.get(link.getSubjectId());
			if (link.isExcluded() || subject == null || subject.isHidden() || !subject.isActive()) {
				continue;
			}
			counted.add(new CountedLink(link.getChildId(), subject.getSchoolId(), subject.getGradeLabel()));
		}
		return List.copyOf(counted);
	}

	@Override
	@Transactional
	public void save(long childId, long subjectId, boolean excluded) {
		Optional<BackChildSubjectLinkEntity> found = links.findByChildIdAndSubjectId(childId, subjectId);
		if (found.isPresent()) {
			found.get().setExcluded(excluded);
			links.saveAndFlush(found.get());
			return;
		}
		links.saveAndFlush(new BackChildSubjectLinkEntity(childId, subjectId, excluded));
	}

	@Override
	@Transactional
	public boolean deleteLink(long childId, long subjectId) {
		return links.deleteLink(childId, subjectId) > 0;
	}

	/**
	 * Noms affichés des accès, tri français, joints par « , ».
	 */
	private Map<Long, String> teacherNamesBySubject(List<BackTeacherSubjectEntity> rows) {
		Map<Long, String> bySubject = new HashMap<>();
		if (rows.isEmpty()) {
			return bySubject;
		}
		Collator order = Collator.getInstance(Locale.FRENCH);
		order.setStrength(Collator.PRIMARY);
		Map<Long, String> userNames = new HashMap<>();
		for (BackTeacherSubjectEntity row : rows) {
			List<String> names = new ArrayList<>();
			for (BackTeacherSubjectAccessEntity link : access.findBySubjectIdOrderByIdAsc(row.getId())) {
				names.add(displayName(link.getUserId(), userNames));
			}
			names.sort(order);
			bySubject.put(row.getId(), String.join(", ", names));
		}
		return bySubject;
	}

	private String displayName(long userId, Map<Long, String> cache) {
		if (cache.containsKey(userId)) {
			return cache.get(userId);
		}
		String name = users.findById(userId)
				.map(user -> user.getDisplayName() == null ? "" : user.getDisplayName())
				.orElse("");
		cache.put(userId, name);
		return name;
	}

	private Map<Long, BackTeacherSubjectEntity> subjectsById(List<BackChildSubjectLinkEntity> rows) {
		List<Long> ids = rows.stream().map(BackChildSubjectLinkEntity::getSubjectId).distinct().toList();
		Map<Long, BackTeacherSubjectEntity> byId = new HashMap<>();
		if (ids.isEmpty()) {
			return byId;
		}
		for (BackTeacherSubjectEntity subject : subjects.findAllById(ids)) {
			byId.put(subject.getId(), subject);
		}
		return byId;
	}
}

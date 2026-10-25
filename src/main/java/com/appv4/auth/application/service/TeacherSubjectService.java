package com.appv4.auth.application.service;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.TeacherSubjectUseCase;
import com.appv4.auth.application.port.out.SchoolCatalogPort;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherSchoolLink;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredSubject;
import com.appv4.auth.application.result.TeacherSubjectsView;
import com.appv4.auth.application.result.TeacherSubjectsView.GradeGroup;
import com.appv4.auth.application.result.TeacherSubjectsView.SchoolGroup;
import com.appv4.auth.application.result.TeacherSubjectsView.SubjectLine;

/**
 * Matières d'un professeur, classe par classe (SCRUM-161).
 *
 * <p><b>Business :</b> il crée, voit, renomme et retire un nom dans une année
 * déjà cochée. Deux classes peuvent porter le même nom : ce sont deux matières.
 * Un autre professeur ne les voit pas. Pas d'exercice dans cette story.
 * <p><b>Technique :</b> le nom affiché est coupé des espaces autour, 100 caractères
 * maximum. La comparaison ignore la casse. Les formes de questions et le clone
 * vers un collègue ne sont pas encore exposés.
 * <p><b>Métier / flow :</b> port in → profil (écoles et années) + port matières.
 * Retirer une année du profil est fait par {@link TeacherProfileService}, qui
 * efface les matières de cette année.
 */
public class TeacherSubjectService implements TeacherSubjectUseCase {

	public static final int NAME_MAX = 100;
	public static final String NAME_REQUIRED = "Le nom est obligatoire.";
	public static final String NAME_TOO_LONG = "Le nom dépasse 100 caractères.";
	public static final String NAME_TAKEN = "Une matière de ce nom existe déjà dans cette classe.";
	public static final String CLASS_NOT_ON_PROFILE = "Cette classe n'est pas sur ton profil.";
	public static final String SUBJECT_MISSING = "Cette matière n'existe pas.";

	private final TeacherSubjectPort teacherSubjectPort;
	private final TeacherProfilePort teacherProfilePort;
	private final SchoolCatalogPort schoolCatalogPort;

	public TeacherSubjectService(
			TeacherSubjectPort teacherSubjectPort,
			TeacherProfilePort teacherProfilePort,
			SchoolCatalogPort schoolCatalogPort) {
		this.teacherSubjectPort = teacherSubjectPort;
		this.teacherProfilePort = teacherProfilePort;
		this.schoolCatalogPort = schoolCatalogPort;
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherSubjectsView list(long userId, String activeRole) {
		requireTeacher(activeRole);
		requireAccount(userId);
		return grouped(userId);
	}

	@Override
	@Transactional
	public SubjectLine create(long userId, String activeRole, Long schoolId, String grade, String name) {
		requireTeacher(activeRole);
		requireAccount(userId);
		if (schoolId == null || grade == null || grade.isBlank()) {
			throw bad(CLASS_NOT_ON_PROFILE);
		}
		requireClass(userId, schoolId, grade);
		String trimmed = requiredName(name);
		String key = nameKey(trimmed);
		if (teacherSubjectPort.nameTaken(userId, schoolId, grade, key, null)) {
			throw bad(NAME_TAKEN);
		}
		StoredSubject saved = teacherSubjectPort.insert(userId, schoolId, grade, trimmed, key);
		return new SubjectLine(saved.id(), saved.name());
	}

	@Override
	@Transactional
	public SubjectLine rename(long userId, String activeRole, long subjectId, String name) {
		requireTeacher(activeRole);
		requireAccount(userId);
		StoredSubject current = requireOwned(userId, subjectId);
		requireClass(userId, current.schoolId(), current.grade());
		String trimmed = requiredName(name);
		String key = nameKey(trimmed);
		if (teacherSubjectPort.nameTaken(userId, current.schoolId(), current.grade(), key, subjectId)) {
			throw bad(NAME_TAKEN);
		}
		StoredSubject saved = teacherSubjectPort
				.rename(userId, subjectId, trimmed, key)
				.orElseThrow(() -> missing());
		return new SubjectLine(saved.id(), saved.name());
	}

	@Override
	@Transactional
	public void remove(long userId, String activeRole, long subjectId) {
		requireTeacher(activeRole);
		requireAccount(userId);
		if (!teacherSubjectPort.delete(userId, subjectId)) {
			throw missing();
		}
	}

	private TeacherSubjectsView grouped(long userId) {
		List<StoredSubject> owned = teacherSubjectPort.findByUser(userId);
		if (owned == null) {
			owned = List.of();
		}
		List<CatalogSchool> catalog = schoolCatalogPort.list();
		List<SchoolGroup> schools = new ArrayList<>();
		for (TeacherSchoolLink link : teacherProfilePort.findSchools(userId)) {
			List<String> grades = SchoolCurricula.ordered(link.curriculum(), link.grades());
			if (grades.isEmpty()) {
				grades = link.grades();
			}
			List<GradeGroup> groups = new ArrayList<>();
			for (String grade : grades) {
				groups.add(new GradeGroup(grade, linesFor(link.schoolId(), grade, owned)));
			}
			schools.add(new SchoolGroup(link.schoolId(), schoolName(link.schoolId(), catalog), groups));
		}
		return new TeacherSubjectsView(schools);
	}

	private static List<SubjectLine> linesFor(long schoolId, String grade, List<StoredSubject> owned) {
		List<StoredSubject> matches = new ArrayList<>();
		for (StoredSubject subject : owned) {
			if (subject.schoolId() == schoolId && grade.equals(subject.grade())) {
				matches.add(subject);
			}
		}
		Collator order = Collator.getInstance(Locale.FRENCH);
		order.setStrength(Collator.PRIMARY);
		matches.sort((left, right) -> order.compare(left.name(), right.name()));
		List<SubjectLine> lines = new ArrayList<>();
		for (StoredSubject subject : matches) {
			lines.add(new SubjectLine(subject.id(), subject.name()));
		}
		return List.copyOf(lines);
	}

	private static String schoolName(long schoolId, List<CatalogSchool> catalog) {
		if (catalog == null) {
			return "";
		}
		for (CatalogSchool school : catalog) {
			if (school.id() == schoolId) {
				return school.name() == null ? "" : school.name();
			}
		}
		return "";
	}

	private void requireClass(long userId, long schoolId, String grade) {
		for (TeacherSchoolLink link : teacherProfilePort.findSchools(userId)) {
			if (link.schoolId() == schoolId && link.grades().contains(grade)) {
				return;
			}
		}
		throw bad(CLASS_NOT_ON_PROFILE);
	}

	private StoredSubject requireOwned(long userId, long subjectId) {
		Optional<StoredSubject> found = teacherSubjectPort.findOwned(userId, subjectId);
		if (found == null || found.isEmpty()) {
			throw missing();
		}
		return found.get();
	}

	private void requireAccount(long userId) {
		teacherProfilePort.findAccount(userId)
				.orElseThrow(() -> new AuthDomainException(
						AuthErrorCode.NOT_FOUND, TeacherProfileService.ACCOUNT_MISSING));
	}

	private static String requiredName(String name) {
		if (name == null || name.isBlank()) {
			throw bad(NAME_REQUIRED);
		}
		String trimmed = name.trim();
		if (trimmed.length() > NAME_MAX) {
			throw bad(NAME_TOO_LONG);
		}
		return trimmed;
	}

	private static String nameKey(String trimmed) {
		return trimmed.toLowerCase(Locale.ROOT);
	}

	private static void requireTeacher(String activeRole) {
		if (!ActiveRoles.TEACHER.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceHello.DENIED);
		}
	}

	private static AuthDomainException bad(String message) {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST, message);
	}

	private static AuthDomainException missing() {
		return new AuthDomainException(AuthErrorCode.NOT_FOUND, SUBJECT_MISSING);
	}
}

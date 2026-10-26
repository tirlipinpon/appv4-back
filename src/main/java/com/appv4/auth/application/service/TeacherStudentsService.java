package com.appv4.auth.application.service;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.TeacherStudentsUseCase;
import com.appv4.auth.application.port.out.EnrolledChildPort;
import com.appv4.auth.application.port.out.EnrolledChildPort.EnrolledChild;
import com.appv4.auth.application.port.out.EnrolledChildPort.EnrolledPhoto;
import com.appv4.auth.application.port.out.SchoolCatalogPort;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherSchoolLink;
import com.appv4.auth.application.result.TeacherStudentsView;
import com.appv4.auth.application.result.TeacherStudentsView.GradeGroup;
import com.appv4.auth.application.result.TeacherStudentsView.SchoolGroup;
import com.appv4.auth.application.result.TeacherStudentsView.StudentLine;

/**
 * Élèves inscrits dans les classes du professeur (SCRUM-160).
 *
 * <p><b>Business :</b> un enfant apparaît seulement si son école, sa norme et son année
 * sont exactement une classe cochée sur le profil. Sans école, il n'est dans aucun groupe.
 * Désactivé ou masqué, il n'est plus proposé. Retirer une année retire le groupe,
 * pas la fiche. Le code PIN, l'email du parent et son nom de compte ne sortent pas.
 * <p><b>Technique :</b> lecture seule. Le tri des noms suit le français, puis l'identifiant.
 * Les années suivent l'ordre de {@link SchoolCurricula}.
 * <p><b>Métier / flow :</b> port in → profil (écoles et années) + port des fiches encore proposées.
 */
public class TeacherStudentsService implements TeacherStudentsUseCase {

	public static final String NO_CLASS =
			"Aucune classe à afficher tant que le profil n'a pas d'école.";
	public static final String EMPTY_GRADE = "Aucun élève inscrit dans cette classe.";
	public static final String NOT_IN_CLASS = "Cet élève n'est pas dans tes classes.";
	public static final String NO_PHOTO = "Pas de photo pour cet élève.";

	private final EnrolledChildPort enrolledChildPort;
	private final TeacherProfilePort teacherProfilePort;
	private final SchoolCatalogPort schoolCatalogPort;

	public TeacherStudentsService(
			EnrolledChildPort enrolledChildPort,
			TeacherProfilePort teacherProfilePort,
			SchoolCatalogPort schoolCatalogPort) {
		this.enrolledChildPort = enrolledChildPort;
		this.teacherProfilePort = teacherProfilePort;
		this.schoolCatalogPort = schoolCatalogPort;
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherStudentsView list(long userId, String activeRole) {
		requireTeacher(activeRole);
		requireAccount(userId);
		return grouped(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public StudentPhoto photo(long userId, String activeRole, long childId) {
		requireTeacher(activeRole);
		requireAccount(userId);
		EnrolledChild child = enrolledChildPort.findActive(childId).orElse(null);
		if (!inClass(userId, child)) {
			throw missing(NOT_IN_CLASS);
		}
		EnrolledPhoto photo = enrolledChildPort.findPhoto(childId).orElse(null);
		if (photo == null || photo.bytes() == null) {
			throw missing(NO_PHOTO);
		}
		return new StudentPhoto(photo.bytes(), photo.contentType());
	}

	private TeacherStudentsView grouped(long userId) {
		List<TeacherSchoolLink> links = teacherProfilePort.findSchools(userId);
		if (links == null) {
			links = List.of();
		}
		List<Long> schoolIds = new ArrayList<>();
		for (TeacherSchoolLink link : links) {
			schoolIds.add(link.schoolId());
		}
		List<EnrolledChild> roster = List.of();
		if (!schoolIds.isEmpty()) {
			List<EnrolledChild> found = enrolledChildPort.findActiveInSchools(schoolIds);
			roster = found == null ? List.of() : found;
		}
		List<CatalogSchool> catalog = schoolCatalogPort.list();
		List<SchoolGroup> schools = new ArrayList<>();
		for (TeacherSchoolLink link : links) {
			List<GradeGroup> groups = new ArrayList<>();
			for (String grade : gradesOf(link)) {
				List<StudentLine> students = studentsFor(link, grade, roster);
				groups.add(new GradeGroup(grade, countLabel(students.size()), EMPTY_GRADE, students));
			}
			schools.add(new SchoolGroup(
					link.schoolId(),
					schoolName(link.schoolId(), catalog),
					SchoolCurricula.label(link.curriculum()),
					groups));
		}
		return new TeacherStudentsView(NO_CLASS, List.copyOf(schools));
	}

	private boolean inClass(long userId, EnrolledChild child) {
		if (child == null || !child.active() || child.schoolId() == null) {
			return false;
		}
		List<TeacherSchoolLink> links = teacherProfilePort.findSchools(userId);
		if (links == null) {
			return false;
		}
		for (TeacherSchoolLink link : links) {
			if (child.schoolId() != link.schoolId()) {
				continue;
			}
			if (!Objects.equals(link.curriculum(), child.curriculum())) {
				continue;
			}
			if (gradesOf(link).contains(child.grade())) {
				return true;
			}
		}
		return false;
	}

	private static List<StudentLine> studentsFor(
			TeacherSchoolLink link, String grade, List<EnrolledChild> roster) {
		List<EnrolledChild> matches = new ArrayList<>();
		for (EnrolledChild child : roster) {
			if (child == null || !child.active() || child.schoolId() == null) {
				continue;
			}
			if (child.schoolId() == link.schoolId()
					&& Objects.equals(link.curriculum(), child.curriculum())
					&& grade.equals(child.grade())) {
				matches.add(child);
			}
		}
		Collator order = Collator.getInstance(Locale.FRENCH);
		order.setStrength(Collator.PRIMARY);
		matches.sort((left, right) -> {
			int byLast = order.compare(text(left.lastName()), text(right.lastName()));
			if (byLast != 0) {
				return byLast;
			}
			int byFirst = order.compare(text(left.firstName()), text(right.firstName()));
			if (byFirst != 0) {
				return byFirst;
			}
			return Long.compare(left.id(), right.id());
		});
		List<StudentLine> lines = new ArrayList<>();
		for (EnrolledChild child : matches) {
			lines.add(new StudentLine(
					child.id(),
					child.firstName(),
					child.lastName(),
					child.age(),
					child.avatarCode(),
					child.hasPhoto()));
		}
		return List.copyOf(lines);
	}

	private static List<String> gradesOf(TeacherSchoolLink link) {
		List<String> grades = SchoolCurricula.ordered(link.curriculum(), link.grades());
		if (grades.isEmpty()) {
			return link.grades();
		}
		return grades;
	}

	private static String countLabel(int count) {
		if (count > 1) {
			return count + " élèves";
		}
		return count + " élève";
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

	private static String text(String value) {
		return value == null ? "" : value;
	}

	private void requireAccount(long userId) {
		teacherProfilePort.findAccount(userId)
				.orElseThrow(() -> new AuthDomainException(
						AuthErrorCode.NOT_FOUND, TeacherProfileService.ACCOUNT_MISSING));
	}

	private static void requireTeacher(String activeRole) {
		if (!ActiveRoles.TEACHER.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceHello.DENIED);
		}
	}

	private static AuthDomainException missing(String message) {
		return new AuthDomainException(AuthErrorCode.NOT_FOUND, message);
	}
}

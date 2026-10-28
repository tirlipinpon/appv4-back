package com.appv4.auth.application.service;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.TeacherProfileUseCase;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.EnrolledChildPort;
import com.appv4.auth.application.port.out.EnrolledChildPort.EnrolledChild;
import com.appv4.auth.application.port.out.SchoolCatalogPort;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherAccount;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherSchoolLink;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredSubject;
import com.appv4.auth.application.result.TeacherProfileView;
import com.appv4.auth.application.result.TeacherRemovalPreviewView;
import com.appv4.auth.application.result.TeacherRemovalPreviewView.StudentLine;
import com.appv4.auth.application.result.TeacherRemovalPreviewView.SubjectLine;
import com.appv4.auth.application.result.TeacherProfileView.AssignedSchool;
import com.appv4.auth.application.result.TeacherProfileView.AvailableSchool;

/**
 * Met à jour le nom du professeur et ses écoles (SCRUM-159).
 *
 * <p><b>Business :</b> sans rôle teacher actif, pas d'accès. Un nom vide ou trop
 * long ne remplace pas l'ancien. Une école absente du référentiel n'est pas créée.
 * Une école sans année, ou en double, n'est pas enregistrée. Retirer une école
 * ou une année ne retire pas les matières ni les questions : la page ne les
 * montre plus tant que l'année n'est pas recochée, puis elles reviennent.
 * Avant de retirer, l'aperçu dit quels élèves et quelles matières sont concernés
 * (SCRUM-201). Cette lecture n'écrit rien.
 * <p><b>Technique :</b> une seule transaction : si une école est refusée, le nom
 * déjà saisi dans la même requête n'est pas écrit. Le jeton reste valable.
 * <p><b>Métier / flow :</b> port in → port compte + port référentiel d'écoles.
 * SCRUM-163 remplira le référentiel ; ce service n'invente pas d'école.
 */
public class TeacherProfileService implements TeacherProfileUseCase {

	public static final String NAME_REQUIRED = "Le nom est obligatoire.";
	public static final String NAME_TOO_LONG = "Le nom est trop long (maximum 255).";
	public static final String SCHOOL_REQUIRED = "Choisis une école dans la liste, ou retire-la.";
	public static final String SCHOOL_UNKNOWN = "Cette école n'est pas proposable.";
	public static final String SCHOOL_DUPLICATE = "Cette école est déjà sur le profil.";
	public static final String GRADE_REQUIRED = "Choisis une classe pour cette école, ou retire-la.";
	public static final String GRADE_NOT_OFFERED = "Cette classe n'est pas proposée par cette école.";
	public static final String COUNTRY_WITHOUT_GRADES = "Ce pays n'a pas de liste de classes.";
	public static final String ACCOUNT_MISSING = "Compte introuvable.";
	public static final String SCHOOL_NOT_ON_PROFILE = "Cette école n'est pas sur le profil.";
	public static final String GRADE_NOT_ON_PROFILE = "Cette année n'est pas cochée sur le profil.";
	public static final String EMPTY_SCHOOL_STUDENTS = "Aucun élève inscrit dans les classes de cette école.";
	public static final String EMPTY_GRADE_SUBJECTS = "Aucune matière dans cette année.";
	public static final String EMPTY_SCHOOL_SUBJECTS = "Aucune matière dans cette école.";

	private final TeacherProfilePort teacherProfilePort;
	private final SchoolCatalogPort schoolCatalogPort;
	private final TeacherSubjectPort teacherSubjectPort;
	private final EnrolledChildPort enrolledChildPort;
	private final ChildSubjectPort childSubjectPort;

	public TeacherProfileService(
			TeacherProfilePort teacherProfilePort,
			SchoolCatalogPort schoolCatalogPort,
			TeacherSubjectPort teacherSubjectPort,
			EnrolledChildPort enrolledChildPort,
			ChildSubjectPort childSubjectPort) {
		this.teacherProfilePort = teacherProfilePort;
		this.schoolCatalogPort = schoolCatalogPort;
		this.teacherSubjectPort = teacherSubjectPort;
		this.enrolledChildPort = enrolledChildPort;
		this.childSubjectPort = childSubjectPort;
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherProfileView load(long userId, String activeRole) {
		requireTeacher(activeRole);
		TeacherAccount account = requireAccount(userId);
		List<CatalogSchool> available = schoolCatalogPort.list();
		return toView(account.email(), account.displayName(), teacherProfilePort.findSchools(userId), available);
	}

	@Override
	@Transactional
	public TeacherProfileView update(
			long userId,
			String activeRole,
			String displayName,
			List<TeacherSchoolCommand> schools) {
		requireTeacher(activeRole);
		TeacherAccount account = requireAccount(userId);
		String name = normalizeName(displayName);
		List<CatalogSchool> available = schoolCatalogPort.list();
		List<TeacherSchoolLink> links = normalizeSchools(schools, available);
		teacherProfilePort.updateDisplayName(userId, name);
		teacherProfilePort.replaceSchools(userId, links);
		return toView(account.email(), name, links, available);
	}

	/**
	 * Aperçu avant de retirer une année, ou l'école entière si {@code grade} est vide.
	 *
	 * <p><b>Business :</b> le professeur voit les élèves de ces classes, combien de
	 * matières il y a encore, et qui suit chacune. Un élève masqué n'apparaît pas.
	 * Une matière d'un collègue, sans accès pour lui, n'est pas comptée.
	 * <p><b>Technique :</b> lecture seule. L'année absente du profil est un 404.
	 * <p><b>Métier / flow :</b> port in → écoles du profil, fiches enfants, matières
	 * accessibles, puis les suiveurs de chaque matière.
	 */
	@Override
	@Transactional(readOnly = true)
	public TeacherRemovalPreviewView removalPreview(long userId, String activeRole, long schoolId, String grade) {
		requireTeacher(activeRole);
		requireAccount(userId);
		TeacherSchoolLink link = requireLink(userId, schoolId);
		String year = grade == null || grade.isBlank() ? null : grade;
		if (year != null && (link.grades() == null || !link.grades().contains(year))) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, GRADE_NOT_ON_PROFILE);
		}
		List<String> grades = year == null ? gradesOf(link) : List.of(year);
		List<EnrolledChild> roster = List.of();
		List<EnrolledChild> found = enrolledChildPort.findActiveInSchools(List.of(schoolId));
		if (found != null) {
			roster = found;
		}
		List<StudentLine> students = studentsOf(link, grades, roster);
		List<SubjectLine> subjects = subjectsOf(userId, link.schoolId(), grades);
		boolean wholeSchool = year == null;
		return new TeacherRemovalPreviewView(
				link.schoolId(),
				schoolName(link.schoolId()),
				year,
				students.size(),
				countLabel(students.size(), "élève", "élèves"),
				wholeSchool ? EMPTY_SCHOOL_STUDENTS : TeacherStudentsService.EMPTY_GRADE,
				students,
				subjects.size(),
				countLabel(subjects.size(), "matière", "matières"),
				wholeSchool ? EMPTY_SCHOOL_SUBJECTS : EMPTY_GRADE_SUBJECTS,
				subjects);
	}

	private TeacherAccount requireAccount(long userId) {
		return teacherProfilePort.findAccount(userId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, ACCOUNT_MISSING));
	}

	private static void requireTeacher(String activeRole) {
		if (!ActiveRoles.TEACHER.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, SpaceHello.DENIED);
		}
	}

	private static String normalizeName(String displayName) {
		if (displayName == null || displayName.isBlank()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, NAME_REQUIRED);
		}
		String trimmed = displayName.trim();
		if (trimmed.length() > DisplayNames.MAX_LENGTH) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, NAME_TOO_LONG);
		}
		return trimmed;
	}

	private static List<TeacherSchoolLink> normalizeSchools(
			List<TeacherSchoolCommand> schools,
			List<CatalogSchool> available) {
		if (schools == null || schools.isEmpty()) {
			return List.of();
		}
		Map<Long, CatalogSchool> byId = new LinkedHashMap<>();
		for (CatalogSchool school : available) {
			byId.putIfAbsent(school.id(), school);
		}
		Set<Long> seen = new HashSet<>();
		List<TeacherSchoolLink> links = new ArrayList<>();
		for (TeacherSchoolCommand command : schools) {
			links.add(oneSchool(command, byId, seen));
		}
		return List.copyOf(links);
	}

	private static TeacherSchoolLink oneSchool(
			TeacherSchoolCommand command,
			Map<Long, CatalogSchool> byId,
			Set<Long> seen) {
		if (command == null || command.schoolId() == null) {
			throw bad(SCHOOL_REQUIRED);
		}
		CatalogSchool school = byId.get(command.schoolId());
		if (school == null) {
			throw bad(SCHOOL_UNKNOWN);
		}
		if (!seen.add(command.schoolId())) {
			throw bad(SCHOOL_DUPLICATE);
		}
		String curriculum = SchoolCurricula.forCountry(school.country());
		if (curriculum == null) {
			throw bad(COUNTRY_WITHOUT_GRADES);
		}
		List<String> offered = SchoolCurricula.offeredBy(school.country(), school.teachingTypes());
		List<String> grades = command.grades();
		if (grades == null || grades.isEmpty()) {
			throw bad(GRADE_REQUIRED);
		}
		for (String grade : grades) {
			if (!offered.contains(grade)) {
				throw bad(GRADE_NOT_OFFERED);
			}
		}
		List<String> ordered = SchoolCurricula.ordered(curriculum, grades);
		if (ordered.isEmpty()) {
			throw bad(GRADE_REQUIRED);
		}
		return new TeacherSchoolLink(command.schoolId(), curriculum, ordered);
	}

	private TeacherSchoolLink requireLink(long userId, long schoolId) {
		List<TeacherSchoolLink> links = teacherProfilePort.findSchools(userId);
		if (links == null) {
			links = List.of();
		}
		for (TeacherSchoolLink link : links) {
			if (link != null && link.schoolId() == schoolId) {
				return link;
			}
		}
		throw new AuthDomainException(AuthErrorCode.NOT_FOUND, SCHOOL_NOT_ON_PROFILE);
	}

	private List<StudentLine> studentsOf(
			TeacherSchoolLink link, List<String> grades, List<EnrolledChild> roster) {
		List<EnrolledChild> matches = new ArrayList<>();
		for (EnrolledChild child : roster) {
			if (child == null || !child.active() || child.schoolId() == null) {
				continue;
			}
			if (child.schoolId() != link.schoolId()) {
				continue;
			}
			if (!Objects.equals(link.curriculum(), child.curriculum())) {
				continue;
			}
			if (child.grade() == null || !grades.contains(child.grade())) {
				continue;
			}
			matches.add(child);
		}
		Collator order = french();
		matches.sort(Comparator
				.comparingInt((EnrolledChild child) -> gradeIndex(grades, child.grade()))
				.thenComparing(child -> text(child.lastName()), order)
				.thenComparing(child -> text(child.firstName()), order)
				.thenComparingLong(EnrolledChild::id));
		List<StudentLine> lines = new ArrayList<>();
		for (EnrolledChild child : matches) {
			lines.add(new StudentLine(child.id(), child.firstName(), child.lastName(), child.grade()));
		}
		return List.copyOf(lines);
	}

	private List<SubjectLine> subjectsOf(long userId, long schoolId, List<String> grades) {
		List<StoredSubject> owned = teacherSubjectPort.findByUser(userId);
		if (owned == null) {
			owned = List.of();
		}
		List<StoredSubject> matches = new ArrayList<>();
		for (StoredSubject subject : owned) {
			if (subject == null || subject.schoolId() != schoolId) {
				continue;
			}
			if (subject.grade() == null || !grades.contains(subject.grade())) {
				continue;
			}
			matches.add(subject);
		}
		Collator order = french();
		matches.sort(Comparator
				.comparingInt((StoredSubject subject) -> gradeIndex(grades, subject.grade()))
				.thenComparing(subject -> text(subject.name()), order)
				.thenComparingLong(StoredSubject::id));
		List<SubjectLine> lines = new ArrayList<>();
		for (StoredSubject subject : matches) {
			List<String> names = SubjectFollowers.names(
					childSubjectPort, subject.schoolId(), subject.grade(), subject.id());
			lines.add(new SubjectLine(
					subject.id(),
					subject.name(),
					subject.grade(),
					names.size(),
					TeacherQuestionService.followersWarning(names.size()),
					names));
		}
		return List.copyOf(lines);
	}

	private String schoolName(long schoolId) {
		List<CatalogSchool> catalog = schoolCatalogPort.list();
		if (catalog == null) {
			return "";
		}
		for (CatalogSchool school : catalog) {
			if (school != null && school.id() == schoolId) {
				return school.name() == null ? "" : school.name();
			}
		}
		return "";
	}

	private static List<String> gradesOf(TeacherSchoolLink link) {
		List<String> grades = link.grades() == null ? List.of() : link.grades();
		List<String> ordered = SchoolCurricula.ordered(link.curriculum(), grades);
		if (ordered.isEmpty()) {
			return grades;
		}
		return ordered;
	}

	private static int gradeIndex(List<String> grades, String grade) {
		int index = grades.indexOf(grade);
		return index < 0 ? grades.size() : index;
	}

	private static String countLabel(int count, String singular, String plural) {
		if (count > 1) {
			return count + " " + plural;
		}
		return count + " " + singular;
	}

	private static Collator french() {
		Collator order = Collator.getInstance(Locale.FRENCH);
		order.setStrength(Collator.PRIMARY);
		return order;
	}

	private static String text(String value) {
		return value == null ? "" : value;
	}

	private static TeacherProfileView toView(
			String email,
			String displayName,
			List<TeacherSchoolLink> links,
			List<CatalogSchool> available) {
		Map<Long, CatalogSchool> byId = new LinkedHashMap<>();
		for (CatalogSchool school : available) {
			byId.putIfAbsent(school.id(), school);
		}
		List<AssignedSchool> assigned = new ArrayList<>();
		for (TeacherSchoolLink link : links) {
			CatalogSchool school = byId.get(link.schoolId());
			assigned.add(new AssignedSchool(
					link.schoolId(),
					school == null ? "" : school.name(),
					school == null ? "" : school.country(),
					link.curriculum(),
					link.grades()));
		}
		List<AvailableSchool> offered = available.stream()
				.map(school -> new AvailableSchool(
						school.id(),
						school.name(),
						school.country(),
						SchoolCurricula.offeredBy(school.country(), school.teachingTypes())))
				.toList();
		return new TeacherProfileView(email, displayName, assigned, offered, SchoolCurricula.options());
	}

	private static AuthDomainException bad(String message) {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST, message);
	}
}

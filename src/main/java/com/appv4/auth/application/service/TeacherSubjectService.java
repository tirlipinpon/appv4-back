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
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
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
 * Matières d'un professeur, classe par classe (SCRUM-161, SCRUM-172).
 *
 * <p><b>Business :</b> il crée, voit, renomme et retire un nom dans une année
 * déjà cochée. Il peut déplacer ou partager la même matière avec un collègue
 * accepté qui a cette classe. Pas de copie : les enfants et les formes futures
 * restent sur le même id.
 * <p><b>Technique :</b> le nom affiché est coupé des espaces autour, 100 caractères
 * maximum. La comparaison ignore la casse. L'accès se lit dans
 * {@code back_teacher_subject_access}.
 * <p><b>Métier / flow :</b> port in → profil + comptes + rôles + port matières.
 * Retirer une année du profil est fait par {@link TeacherProfileService}, qui
 * retire l'accès sans effacer la ligne de matière.
 */
public class TeacherSubjectService implements TeacherSubjectUseCase {

	public static final int NAME_MAX = 100;
	public static final String NAME_REQUIRED = "Le nom est obligatoire.";
	public static final String NAME_TOO_LONG = "Le nom dépasse 100 caractères.";
	public static final String NAME_TAKEN = "Une matière de ce nom existe déjà dans cette classe.";
	public static final String CLASS_NOT_ON_PROFILE = "Cette classe n'est pas sur ton profil.";
	public static final String SUBJECT_MISSING = "Cette matière n'existe pas.";
	public static final String EMAIL_REQUIRED = "Indique l'email du professeur.";
	public static final String MODE_REQUIRED = "Choisis de déplacer ou de partager.";
	public static final String TEACHER_UNKNOWN = "Aucun professeur accepté avec cet email.";
	public static final String COLLEAGUE_CLASS_MISSING = "Ce professeur n'a pas cette classe sur son profil.";
	public static final String ALREADY_HAS = "Ce professeur a déjà cette matière.";
	public static final String MODE_MOVE = "move";
	public static final String MODE_SHARE = "share";

	private final TeacherSubjectPort teacherSubjectPort;
	private final TeacherProfilePort teacherProfilePort;
	private final SchoolCatalogPort schoolCatalogPort;
	private final AuthUserAccountPort authUserAccountPort;
	private final ActiveRolePort activeRolePort;

	public TeacherSubjectService(
			TeacherSubjectPort teacherSubjectPort,
			TeacherProfilePort teacherProfilePort,
			SchoolCatalogPort schoolCatalogPort,
			AuthUserAccountPort authUserAccountPort,
			ActiveRolePort activeRolePort) {
		this.teacherSubjectPort = teacherSubjectPort;
		this.teacherProfilePort = teacherProfilePort;
		this.schoolCatalogPort = schoolCatalogPort;
		this.authUserAccountPort = authUserAccountPort;
		this.activeRolePort = activeRolePort;
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
		if (teacherSubjectPort.nameTaken(schoolId, grade, key, null)) {
			throw bad(NAME_TAKEN);
		}
		StoredSubject saved = teacherSubjectPort.insert(userId, schoolId, grade, trimmed, key);
		return line(userId, saved);
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
		if (teacherSubjectPort.nameTaken(current.schoolId(), current.grade(), key, subjectId)) {
			throw bad(NAME_TAKEN);
		}
		StoredSubject saved = teacherSubjectPort
				.rename(userId, subjectId, trimmed, key)
				.orElseThrow(() -> missing());
		return line(userId, saved);
	}

	@Override
	@Transactional
	public void deactivate(long userId, String activeRole, long subjectId) {
		requireTeacher(activeRole);
		requireAccount(userId);
		if (!teacherSubjectPort.deactivate(userId, subjectId)) {
			throw missing();
		}
	}

	@Override
	@Transactional
	public void reactivate(long userId, String activeRole, long subjectId) {
		requireTeacher(activeRole);
		requireAccount(userId);
		if (!teacherSubjectPort.reactivate(userId, subjectId)) {
			throw missing();
		}
	}

	@Override
	@Transactional
	public void remove(long userId, String activeRole, long subjectId) {
		requireTeacher(activeRole);
		requireAccount(userId);
		if (!teacherSubjectPort.hide(userId, subjectId)) {
			throw missing();
		}
	}

	@Override
	@Transactional
	public void transfer(long userId, String activeRole, long subjectId, String email, String mode) {
		requireTeacher(activeRole);
		requireAccount(userId);
		StoredSubject subject = requireOwned(userId, subjectId);
		requireClass(userId, subject.schoolId(), subject.grade());
		String transferMode = requireMode(mode);
		String normalized = requireColleagueEmail(email);
		AuthUserAccount colleague = findAcceptedTeacher(normalized);
		if (colleague.id() == userId) {
			throw bad(ALREADY_HAS);
		}
		boolean alreadyHas = teacherSubjectPort.hasAccess(subjectId, colleague.id());
		// Partager vers quelqu'un qui l'a déjà : refus. Déplacer vers un co-prof : OK (il reste seul).
		if (MODE_SHARE.equals(transferMode) && alreadyHas) {
			throw bad(ALREADY_HAS);
		}
		if (!hasClass(colleague.id(), subject.schoolId(), subject.grade())) {
			throw bad(COLLEAGUE_CLASS_MISSING);
		}
		if (MODE_MOVE.equals(transferMode)) {
			teacherSubjectPort.replaceAccessWith(subjectId, colleague.id());
		} else {
			teacherSubjectPort.addAccess(subjectId, colleague.id());
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
				groups.add(new GradeGroup(grade, linesFor(userId, link.schoolId(), grade, owned)));
			}
			schools.add(new SchoolGroup(link.schoolId(), schoolName(link.schoolId(), catalog), groups));
		}
		return new TeacherSubjectsView(schools);
	}

	private List<SubjectLine> linesFor(
			long userId, long schoolId, String grade, List<StoredSubject> owned) {
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
			lines.add(line(userId, subject));
		}
		return List.copyOf(lines);
	}

	private SubjectLine line(long userId, StoredSubject subject) {
		List<String> shared = teacherSubjectPort.otherAccessNames(subject.id(), userId);
		if (shared == null) {
			shared = List.of();
		}
		return new SubjectLine(subject.id(), subject.name(), subject.active(), shared);
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
		if (!hasClass(userId, schoolId, grade)) {
			throw bad(CLASS_NOT_ON_PROFILE);
		}
	}

	private boolean hasClass(long userId, long schoolId, String grade) {
		for (TeacherSchoolLink link : teacherProfilePort.findSchools(userId)) {
			if (link.schoolId() == schoolId && link.grades().contains(grade)) {
				return true;
			}
		}
		return false;
	}

	private StoredSubject requireOwned(long userId, long subjectId) {
		Optional<StoredSubject> found = teacherSubjectPort.findOwned(userId, subjectId);
		if (found == null || found.isEmpty()) {
			throw missing();
		}
		return found.get();
	}

	private AuthUserAccount findAcceptedTeacher(String normalizedEmail) {
		Optional<AuthUserAccount> found = authUserAccountPort.findByEmail(normalizedEmail);
		if (found == null || found.isEmpty() || !found.get().active()) {
			throw bad(TEACHER_UNKNOWN);
		}
		AuthUserAccount account = found.get();
		if (!activeRolePort.findSpaceRoles(account.id()).contains(ActiveRoles.TEACHER)) {
			throw bad(TEACHER_UNKNOWN);
		}
		return account;
	}

	private static String requireColleagueEmail(String email) {
		if (email == null || email.isBlank()) {
			throw bad(EMAIL_REQUIRED);
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static String requireMode(String mode) {
		if (mode == null || mode.isBlank()) {
			throw bad(MODE_REQUIRED);
		}
		String trimmed = mode.trim().toLowerCase(Locale.ROOT);
		if (!MODE_MOVE.equals(trimmed) && !MODE_SHARE.equals(trimmed)) {
			throw bad(MODE_REQUIRED);
		}
		return trimmed;
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

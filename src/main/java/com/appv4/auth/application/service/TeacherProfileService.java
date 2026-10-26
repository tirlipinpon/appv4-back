package com.appv4.auth.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.TeacherProfileUseCase;
import com.appv4.auth.application.port.out.SchoolCatalogPort;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherAccount;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherSchoolLink;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredSubject;
import com.appv4.auth.application.port.out.TeacherSubjectPort.SubjectClassKey;
import com.appv4.auth.application.result.TeacherProfileView;
import com.appv4.auth.application.result.TeacherProfileView.AssignedSchool;
import com.appv4.auth.application.result.TeacherProfileView.AvailableSchool;

/**
 * Met à jour le nom du professeur et ses écoles (SCRUM-159).
 *
 * <p><b>Business :</b> sans rôle teacher actif, pas d'accès. Un nom vide ou trop
 * long ne remplace pas l'ancien. Une école absente du référentiel n'est pas créée.
 * Une école sans année, ou en double, n'est pas enregistrée. Une année retirée
 * emporte les matières de cette année (SCRUM-161). Les recocher plus tard ne les rend pas.
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

	private final TeacherProfilePort teacherProfilePort;
	private final SchoolCatalogPort schoolCatalogPort;
	private final TeacherSubjectPort teacherSubjectPort;

	public TeacherProfileService(
			TeacherProfilePort teacherProfilePort,
			SchoolCatalogPort schoolCatalogPort,
			TeacherSubjectPort teacherSubjectPort) {
		this.teacherProfilePort = teacherProfilePort;
		this.schoolCatalogPort = schoolCatalogPort;
		this.teacherSubjectPort = teacherSubjectPort;
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
		dropSubjectsOutside(userId, links);
		return toView(account.email(), name, links, available);
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

	/**
	 * Les matières suivent les années cochées. Une année absente de {@code links}
	 * retire l'accès de ce professeur. S'il était le dernier, la matière est cachée.
	 * Les matières d'un collègue seules ne sont pas lues ici.
	 */
	private void dropSubjectsOutside(long userId, List<TeacherSchoolLink> links) {
		Set<SubjectClassKey> kept = new HashSet<>();
		for (TeacherSchoolLink link : links) {
			for (String grade : link.grades()) {
				kept.add(new SubjectClassKey(link.schoolId(), grade));
			}
		}
		List<StoredSubject> owned = teacherSubjectPort.findByUser(userId);
		if (owned == null || owned.isEmpty()) {
			return;
		}
		for (StoredSubject subject : owned) {
			if (!kept.contains(new SubjectClassKey(subject.schoolId(), subject.grade()))) {
				teacherSubjectPort.hide(userId, subject.id());
			}
		}
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

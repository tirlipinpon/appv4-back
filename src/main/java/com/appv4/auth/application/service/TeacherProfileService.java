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
import com.appv4.auth.application.result.TeacherProfileView;
import com.appv4.auth.application.result.TeacherProfileView.AssignedSchool;
import com.appv4.auth.application.result.TeacherProfileView.AvailableSchool;

/**
 * Met à jour le nom du professeur et ses écoles (SCRUM-159).
 *
 * <p><b>Business :</b> sans rôle teacher actif, pas d'accès. Un nom vide ou trop
 * long ne remplace pas l'ancien. Une école absente du référentiel n'est pas créée.
 * Une école sans année, ou en double, n'est pas enregistrée.
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
	public static final String GRADE_REQUIRED = "Choisis une année pour cette école, ou retire-la.";
	public static final String GRADE_MISMATCH = "Cette année n'appartient pas à la norme choisie.";
	public static final String CURRICULUM_UNKNOWN = "La norme choisie n'est pas reconnue.";
	public static final String ACCOUNT_MISSING = "Compte introuvable.";

	private final TeacherProfilePort teacherProfilePort;
	private final SchoolCatalogPort schoolCatalogPort;

	public TeacherProfileService(TeacherProfilePort teacherProfilePort, SchoolCatalogPort schoolCatalogPort) {
		this.teacherProfilePort = teacherProfilePort;
		this.schoolCatalogPort = schoolCatalogPort;
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
		if (!byId.containsKey(command.schoolId())) {
			throw bad(SCHOOL_UNKNOWN);
		}
		if (!seen.add(command.schoolId())) {
			throw bad(SCHOOL_DUPLICATE);
		}
		if (!SchoolCurricula.known(command.curriculum())) {
			throw bad(CURRICULUM_UNKNOWN);
		}
		List<String> grades = command.grades();
		if (grades == null || grades.isEmpty()) {
			throw bad(GRADE_REQUIRED);
		}
		for (String grade : grades) {
			if (!SchoolCurricula.accepts(command.curriculum(), grade)) {
				throw bad(GRADE_MISMATCH);
			}
		}
		List<String> ordered = SchoolCurricula.ordered(command.curriculum(), grades);
		if (ordered.isEmpty()) {
			throw bad(GRADE_REQUIRED);
		}
		return new TeacherSchoolLink(command.schoolId(), command.curriculum(), ordered);
	}

	private static TeacherProfileView toView(
			String email,
			String displayName,
			List<TeacherSchoolLink> links,
			List<CatalogSchool> available) {
		Map<Long, String> names = new LinkedHashMap<>();
		for (CatalogSchool school : available) {
			names.putIfAbsent(school.id(), school.name());
		}
		List<AssignedSchool> assigned = new ArrayList<>();
		for (TeacherSchoolLink link : links) {
			assigned.add(new AssignedSchool(
					link.schoolId(),
					names.getOrDefault(link.schoolId(), ""),
					link.curriculum(),
					link.grades()));
		}
		List<AvailableSchool> offered = available.stream()
				.map(school -> new AvailableSchool(school.id(), school.name()))
				.toList();
		return new TeacherProfileView(email, displayName, assigned, offered, SchoolCurricula.options());
	}

	private static AuthDomainException bad(String message) {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST, message);
	}
}

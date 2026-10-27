package com.appv4.auth.application.service;

import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.TeacherSubjectUseCase;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.SchoolCatalogPort;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherAccount;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherSchoolLink;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort.AccessHolder;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredDeactivateNotice;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredSubject;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredTransferRequest;
import com.appv4.auth.application.result.TeacherDeactivateNoticesView;
import com.appv4.auth.application.result.TeacherDeactivateNoticesView.NoticeLine;
import com.appv4.auth.application.result.TeacherSubjectColleaguesView;
import com.appv4.auth.application.result.TeacherSubjectColleaguesView.ColleagueLine;
import com.appv4.auth.application.result.TeacherSubjectsView;
import com.appv4.auth.application.result.TeacherSubjectsView.GradeGroup;
import com.appv4.auth.application.result.TeacherSubjectsView.SchoolGroup;
import com.appv4.auth.application.result.TeacherSubjectsView.SharedHolder;
import com.appv4.auth.application.result.TeacherSubjectsView.SubjectLine;
import com.appv4.auth.application.result.TeacherTransferInvitesView;
import com.appv4.auth.application.result.TeacherTransferInvitesView.InviteLine;

/**
 * Matières d'un professeur, classe par classe (SCRUM-161, SCRUM-172, SCRUM-189).
 *
 * <p><b>Business :</b> il crée, voit, renomme et retire un nom dans une année
 * déjà cochée. S'il retape le nom d'une matière retirée, on lui propose de
 * la réactiver (même enregistrement). Il propose un déplacement ou un partage
 * à un collègue de la liste ; le collègue accepte ou refuse. La désactivation est par accès :
 * le non-créateur seulement pour lui ; le créateur peut cibler des co-profs et
 * les autres détenteurs reçoivent une alerte. Pas de copie : même id.
 * <p><b>Technique :</b> le nom affiché est coupé des espaces autour, 100 caractères
 * maximum. Les demandes vivent dans {@code back_teacher_subject_transfer_requests}.
 * <p><b>Métier / flow :</b> port in → profil + rôles + port matières.
 */
public class TeacherSubjectService implements TeacherSubjectUseCase {

	public static final int NAME_MAX = 100;
	public static final String NAME_REQUIRED = "Le nom est obligatoire.";
	public static final String NAME_TOO_LONG = "Le nom dépasse 100 caractères.";
	public static final String NAME_TAKEN = "Une matière de ce nom existe déjà dans cette classe.";
	public static final String REMOVED_RESTORABLE =
			"Cette matière a été retirée. Tu peux la réactiver.";
	public static final String CLASS_NOT_ON_PROFILE = "Cette classe n'est pas sur ton profil.";
	public static final String SUBJECT_MISSING = "Cette matière n'existe pas.";
	public static final String MODE_REQUIRED = "Choisis de déplacer ou de partager.";
	public static final String COLLEAGUE_REQUIRED = "Choisis un collègue.";
	public static final String COLLEAGUE_NOT_ELIGIBLE = "Ce professeur n'est pas dans la liste.";
	public static final String ALREADY_HAS = "Ce professeur a déjà cette matière.";
	public static final String INVITE_PENDING = "Une demande est déjà en attente pour ce collègue.";
	public static final String INVITE_MISSING = "Cette demande n'existe pas.";
	public static final String INVITE_STALE = "Cette demande n'est plus valable.";
	public static final String ALSO_NOT_ALLOWED =
			"Seul le créateur peut désactiver pour d'autres professeurs.";
	public static final String ALSO_NOT_HOLDER = "Ce professeur n'a pas cette matière.";
	public static final String NOTICE_MISSING = "Cette alerte n'existe pas.";
	public static final String MODE_MOVE = "move";
	public static final String MODE_SHARE = "share";

	private final TeacherSubjectPort teacherSubjectPort;
	private final TeacherProfilePort teacherProfilePort;
	private final SchoolCatalogPort schoolCatalogPort;
	private final ActiveRolePort activeRolePort;

	public TeacherSubjectService(
			TeacherSubjectPort teacherSubjectPort,
			TeacherProfilePort teacherProfilePort,
			SchoolCatalogPort schoolCatalogPort,
			ActiveRolePort activeRolePort) {
		this.teacherSubjectPort = teacherSubjectPort;
		this.teacherProfilePort = teacherProfilePort;
		this.schoolCatalogPort = schoolCatalogPort;
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
		if (removedAndFree(schoolId, grade, key)) {
			throw bad(REMOVED_RESTORABLE);
		}
		if (teacherSubjectPort.nameTaken(schoolId, grade, key, null)) {
			throw bad(NAME_TAKEN);
		}
		StoredSubject saved = teacherSubjectPort.insert(userId, schoolId, grade, trimmed, key);
		return line(userId, saved);
	}

	@Override
	@Transactional
	public SubjectLine restore(long userId, String activeRole, Long schoolId, String grade, String name) {
		requireTeacher(activeRole);
		requireAccount(userId);
		if (schoolId == null || grade == null || grade.isBlank()) {
			throw bad(CLASS_NOT_ON_PROFILE);
		}
		requireClass(userId, schoolId, grade);
		String trimmed = requiredName(name);
		String key = nameKey(trimmed);
		Optional<StoredSubject> removed = teacherSubjectPort.findRemovedByName(schoolId, grade, key);
		if (removed == null || removed.isEmpty()) {
			if (teacherSubjectPort.nameTaken(schoolId, grade, key, null)) {
				throw bad(NAME_TAKEN);
			}
			throw missing();
		}
		if (teacherSubjectPort.nameTaken(schoolId, grade, key, removed.get().id())) {
			throw bad(NAME_TAKEN);
		}
		Optional<StoredSubject> saved = teacherSubjectPort.restore(userId, removed.get().id());
		if (saved == null || saved.isEmpty()) {
			throw missing();
		}
		return line(userId, saved.get());
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
	public void deactivate(long userId, String activeRole, long subjectId, List<Long> alsoUserIds) {
		requireTeacher(activeRole);
		requireAccount(userId);
		StoredSubject subject = requireOwned(userId, subjectId);
		boolean owner = subject.userId() == userId;
		List<Long> also = normalizeAlso(alsoUserIds);
		if (!owner && !also.isEmpty()) {
			throw bad(ALSO_NOT_ALLOWED);
		}
		Set<Long> holders = new LinkedHashSet<>(teacherSubjectPort.accessUserIds(subjectId));
		if (owner) {
			for (Long other : also) {
				if (other == userId) {
					continue;
				}
				if (!holders.contains(other)) {
					throw bad(ALSO_NOT_HOLDER);
				}
			}
		}
		LinkedHashSet<Long> targets = new LinkedHashSet<>();
		targets.add(userId);
		if (owner) {
			for (Long other : also) {
				if (other != userId) {
					targets.add(other);
				}
			}
		}
		if (!teacherSubjectPort.deactivateAccesses(userId, subjectId, List.copyOf(targets))) {
			throw missing();
		}
		if (owner && holders.size() > 1) {
			List<Long> recipients = new ArrayList<>();
			for (Long holder : holders) {
				if (holder != userId) {
					recipients.add(holder);
				}
			}
			teacherSubjectPort.insertDeactivateNotices(
					subjectId, userId, recipients, List.copyOf(targets));
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
	@Transactional(readOnly = true)
	public TeacherSubjectColleaguesView listColleagues(long userId, String activeRole, long subjectId) {
		requireTeacher(activeRole);
		requireAccount(userId);
		StoredSubject subject = requireOwned(userId, subjectId);
		requireClass(userId, subject.schoolId(), subject.grade());
		// Liste commune : le refus « déjà cette matière » se joue au moment du partage.
		return new TeacherSubjectColleaguesView(eligibleColleagues(userId, subject, MODE_MOVE));
	}

	@Override
	@Transactional
	public void requestTransfer(
			long userId, String activeRole, long subjectId, Long colleagueUserId, String mode) {
		requireTeacher(activeRole);
		requireAccount(userId);
		StoredSubject subject = requireOwned(userId, subjectId);
		requireClass(userId, subject.schoolId(), subject.grade());
		String transferMode = requireMode(mode);
		if (colleagueUserId == null) {
			throw bad(COLLEAGUE_REQUIRED);
		}
		if (colleagueUserId == userId) {
			throw bad(ALREADY_HAS);
		}
		if (!isEligibleColleague(userId, subject, colleagueUserId, transferMode)) {
			if (MODE_SHARE.equals(transferMode) && teacherSubjectPort.hasAccess(subjectId, colleagueUserId)) {
				throw bad(ALREADY_HAS);
			}
			throw bad(COLLEAGUE_NOT_ELIGIBLE);
		}
		if (teacherSubjectPort.hasPendingTransfer(subjectId, colleagueUserId)) {
			throw bad(INVITE_PENDING);
		}
		teacherSubjectPort.insertTransferRequest(subjectId, userId, colleagueUserId, transferMode);
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherTransferInvitesView listTransferInvites(long userId, String activeRole) {
		requireTeacher(activeRole);
		requireAccount(userId);
		List<CatalogSchool> catalog = schoolCatalogPort.list();
		List<InviteLine> lines = new ArrayList<>();
		for (StoredTransferRequest request : teacherSubjectPort.findPendingTransfersTo(userId)) {
			Optional<StoredSubject> subject = teacherSubjectPort.findVisible(request.subjectId());
			if (subject.isEmpty()) {
				continue;
			}
			StoredSubject row = subject.get();
			String fromName = teacherProfilePort.findAccount(request.fromUserId())
					.map(TeacherAccount::displayName)
					.orElse("");
			lines.add(new InviteLine(
					request.id(),
					row.id(),
					row.name(),
					row.schoolId(),
					schoolName(row.schoolId(), catalog),
					row.grade(),
					request.mode(),
					fromName == null ? "" : fromName));
		}
		return new TeacherTransferInvitesView(lines);
	}

	@Override
	@Transactional
	public void acceptTransfer(long userId, String activeRole, long inviteId) {
		requireTeacher(activeRole);
		requireAccount(userId);
		StoredTransferRequest request = teacherSubjectPort
				.findPendingTransferForTarget(inviteId, userId)
				.orElseThrow(() -> inviteMissing());
		StoredSubject subject = teacherSubjectPort.findVisible(request.subjectId())
				.orElseThrow(() -> bad(INVITE_STALE));
		if (!teacherSubjectPort.hasAccess(request.subjectId(), request.fromUserId())) {
			throw bad(INVITE_STALE);
		}
		if (!hasClass(userId, subject.schoolId(), subject.grade())) {
			throw bad(INVITE_STALE);
		}
		if (MODE_SHARE.equals(request.mode()) && teacherSubjectPort.hasAccess(request.subjectId(), userId)) {
			teacherSubjectPort.markTransferAccepted(inviteId);
			return;
		}
		if (MODE_MOVE.equals(request.mode())) {
			teacherSubjectPort.replaceAccessWith(request.subjectId(), userId);
			teacherSubjectPort.markTransferAccepted(inviteId);
			teacherSubjectPort.cancelPendingTransfers(request.subjectId(), inviteId);
		} else {
			teacherSubjectPort.addAccess(request.subjectId(), userId);
			teacherSubjectPort.markTransferAccepted(inviteId);
		}
	}

	@Override
	@Transactional
	public void rejectTransfer(long userId, String activeRole, long inviteId) {
		requireTeacher(activeRole);
		requireAccount(userId);
		StoredTransferRequest request = teacherSubjectPort
				.findPendingTransferForTarget(inviteId, userId)
				.orElseThrow(() -> inviteMissing());
		teacherSubjectPort.markTransferRejected(request.id());
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherDeactivateNoticesView listDeactivateNotices(long userId, String activeRole) {
		requireTeacher(activeRole);
		requireAccount(userId);
		List<NoticeLine> lines = new ArrayList<>();
		for (StoredDeactivateNotice notice : teacherSubjectPort.findOpenDeactivateNoticesTo(userId)) {
			Optional<StoredSubject> subject = teacherSubjectPort.findVisible(notice.subjectId());
			if (subject.isEmpty()) {
				continue;
			}
			String fromName = teacherProfilePort.findAccount(notice.fromUserId())
					.map(TeacherAccount::displayName)
					.orElse("");
			lines.add(new NoticeLine(
					notice.id(),
					subject.get().id(),
					subject.get().name(),
					deactivateNoticeMessage(
							fromName == null ? "" : fromName,
							subject.get().name(),
							notice.deactivatedUserIds())));
		}
		return new TeacherDeactivateNoticesView(lines);
	}

	@Override
	@Transactional
	public void dismissDeactivateNotice(long userId, String activeRole, long noticeId) {
		requireTeacher(activeRole);
		requireAccount(userId);
		if (!teacherSubjectPort.dismissDeactivateNotice(noticeId, userId)) {
			throw new AuthDomainException(AuthErrorCode.NOT_FOUND, NOTICE_MISSING);
		}
	}

	private List<ColleagueLine> eligibleColleagues(long userId, StoredSubject subject, String mode) {
		List<ColleagueLine> lines = new ArrayList<>();
		for (Long candidate : teacherProfilePort.findUserIdsWithClass(subject.schoolId(), subject.grade())) {
			if (candidate == null || candidate == userId) {
				continue;
			}
			if (!isAcceptedTeacher(candidate)) {
				continue;
			}
			if (MODE_SHARE.equals(mode) && teacherSubjectPort.hasAccess(subject.id(), candidate)) {
				continue;
			}
			String name = teacherProfilePort.findAccount(candidate)
					.map(TeacherAccount::displayName)
					.orElse("");
			lines.add(new ColleagueLine(candidate, name == null ? "" : name));
		}
		Collator order = Collator.getInstance(Locale.FRENCH);
		order.setStrength(Collator.PRIMARY);
		lines.sort((left, right) -> order.compare(left.displayName(), right.displayName()));
		return List.copyOf(lines);
	}

	private boolean isEligibleColleague(
			long userId, StoredSubject subject, long colleagueUserId, String mode) {
		if (colleagueUserId == userId || !isAcceptedTeacher(colleagueUserId)) {
			return false;
		}
		if (!hasClass(colleagueUserId, subject.schoolId(), subject.grade())) {
			return false;
		}
		if (MODE_SHARE.equals(mode) && teacherSubjectPort.hasAccess(subject.id(), colleagueUserId)) {
			return false;
		}
		return true;
	}

	private boolean isAcceptedTeacher(long userId) {
		return activeRolePort.findSpaceRoles(userId).contains(ActiveRoles.TEACHER);
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
		List<AccessHolder> holders = teacherSubjectPort.otherAccessHolders(subject.id(), userId);
		if (holders == null) {
			holders = List.of();
		}
		List<SharedHolder> shared = holders.stream()
				.map(holder -> new SharedHolder(holder.userId(), holder.displayName()))
				.toList();
		return new SubjectLine(
				subject.id(),
				subject.name(),
				subject.active(),
				subject.userId() == userId,
				shared);
	}

	private String deactivateNoticeMessage(
			String fromName, String subjectName, List<Long> deactivatedUserIds) {
		String actor = fromName == null || fromName.isBlank() ? "Un professeur" : fromName;
		String subject = subjectName == null || subjectName.isBlank() ? "une matière" : subjectName;
		List<Long> ids = deactivatedUserIds == null ? List.of() : deactivatedUserIds;
		List<String> others = new ArrayList<>();
		for (Long id : ids) {
			if (id == null) {
				continue;
			}
			String name = teacherProfilePort.findAccount(id)
					.map(TeacherAccount::displayName)
					.orElse("");
			if (name == null || name.isBlank() || name.equals(fromName)) {
				continue;
			}
			others.add(name);
		}
		Collator order = Collator.getInstance(Locale.FRENCH);
		order.setStrength(Collator.PRIMARY);
		others.sort(order);
		if (others.isEmpty()) {
			return actor + " a désactivé " + subject + " pour soi.";
		}
		return actor + " a désactivé " + subject + " pour soi et " + String.join(", ", others) + ".";
	}

	private static List<Long> normalizeAlso(List<Long> alsoUserIds) {
		if (alsoUserIds == null || alsoUserIds.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<Long> unique = new LinkedHashSet<>();
		for (Long id : alsoUserIds) {
			if (id != null) {
				unique.add(id);
			}
		}
		return List.copyOf(unique);
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

	/**
	 * Vrai si une matière retirée porte déjà ce nom et qu'aucune matière encore
	 * affichée ne le prend. Le professeur peut alors la réactiver, le même id.
	 */
	private boolean removedAndFree(long schoolId, String grade, String key) {
		Optional<StoredSubject> removed = teacherSubjectPort.findRemovedByName(schoolId, grade, key);
		if (removed == null || removed.isEmpty()) {
			return false;
		}
		return !teacherSubjectPort.nameTaken(schoolId, grade, key, removed.get().id());
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

	private static AuthDomainException inviteMissing() {
		return new AuthDomainException(AuthErrorCode.NOT_FOUND, INVITE_MISSING);
	}
}


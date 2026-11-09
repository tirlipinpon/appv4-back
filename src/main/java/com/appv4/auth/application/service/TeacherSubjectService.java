package com.appv4.auth.application.service;

import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import com.appv4.auth.application.port.out.TeacherQuestionPort;
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
import com.appv4.auth.application.result.TeacherSubjectsView.AddableCourse;
import com.appv4.auth.application.result.TeacherSubjectsView.GradeGroup;
import com.appv4.auth.application.result.TeacherSubjectsView.SchoolGroup;
import com.appv4.auth.application.result.TeacherSubjectsView.SharedHolder;
import com.appv4.auth.application.result.TeacherSubjectsView.SubjectLine;
import com.appv4.auth.application.result.TeacherTransferInvitesView;
import com.appv4.auth.application.result.TeacherTransferInvitesView.InviteLine;

/**
 * Matières d'un professeur, classe par classe (SCRUM-161, SCRUM-172, SCRUM-189).
 *
 * <p><b>Business :</b> il crée, voit et retire un nom dans une année déjà cochée.
 * Seul le créateur renomme, même si la matière est partagée. Après un déplacement
 * ou un handover accepté, le collègue devient le créateur et peut renommer. Dans cette année,
 * il voit aussi les matières déjà suivies par un collègue, et le nom de ce
 * collègue, sans pouvoir les modifier tant qu'il ne les suit pas.
 * S'il retape le nom d'une matière retirée, on lui propose de
 * la réactiver (même enregistrement) : le restaurateur en devient le propriétaire.
 * Si le nom est déjà pris, ou s'il
 * ressemble fortement à une matière encore visible, il peut demander au
 * créateur qui la possède encore de la partager. Il propose aussi un déplacement
 * ou un partage à un collègue qui n'a pas encore la matière ; le collègue accepte ou refuse.
 * S'il veut partir alors que d'autres suivent, il désigne un successeur (handover) ;
 * acceptation / refus avec alertes ; si tous refusent, la matière est retirée.
 * La désactivation est par accès :
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
	/**
	 * Début du message quand un autre professeur a déjà créé ce nom.
	 * La suite est son nom affiché, puis {@link #NAME_CAN_ASK_SUFFIX}.
	 */
	public static final String NAME_CAN_ASK_PREFIX =
			"Une matière de ce nom existe déjà. Tu peux demander à ";
	public static final String NAME_CAN_ASK_SUFFIX = " de la partager.";
	public static final String ASK_PENDING =
			"Une demande de partage est déjà en attente auprès du professeur qui a créé cette matière.";
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
	public static final String RENAME_ONLY_CREATOR =
			"Seul le professeur qui a créé cette matière peut la renommer.";
	public static final String ALSO_NOT_HOLDER = "Ce professeur n'a pas cette matière.";
	public static final String NOTICE_MISSING = "Cette alerte n'existe pas.";
	public static final String MODE_MOVE = "move";
	public static final String MODE_SHARE = "share";
	/** Le collègue demande au créateur de partager. L'accès ne change qu'après acceptation. */
	public static final String MODE_ASK = "ask";
	/**
	 * Le créateur demande à un co-détenteur de reprendre la propriété pour pouvoir partir.
	 */
	public static final String MODE_HANDOVER = "handover";
	public static final String HANDOVER_REQUIRED =
			"Choisis un collègue qui suit déjà cette matière pour lui en laisser la responsabilité.";
	public static final String HANDOVER_NOT_HOLDER =
			"Ce professeur ne suit pas cette matière.";
	public static final String HANDOVER_ALREADY_REFUSED =
			"Ce collègue a déjà refusé de reprendre cette matière.";
	public static final String HANDOVER_NOT_OWNER =
			"Seul le propriétaire peut proposer de laisser la matière.";
	public static final String OWNER_WITHOUT_ACCESS =
			"Cette matière n'a plus de propriétaire joignable. Elle ne peut pas être demandée ainsi.";
	public static final String NOTICE_KIND_DEACTIVATE = "deactivate";
	public static final String NOTICE_KIND_HANDOVER_ACCEPTED = "handover_accepted";
	public static final String NOTICE_KIND_HANDOVER_REJECTED = "handover_rejected";
	/** Refus d'une invite move / share / ask (pas handover). */
	public static final String NOTICE_KIND_INVITE_REJECTED = "invite_rejected";

	/**
	 * Message affiché quand le nom est déjà pris et que le professeur peut
	 * demander au créateur de partager.
	 *
	 * @param creatorDisplayName nom affiché du créateur ; « son créateur » s'il est vide
	 */
	public static String nameCanAsk(String creatorDisplayName) {
		String who = creatorDisplayName == null || creatorDisplayName.isBlank()
				? "son créateur"
				: creatorDisplayName.trim();
		return NAME_CAN_ASK_PREFIX + who + NAME_CAN_ASK_SUFFIX;
	}

	/**
	 * Message quand le nom tapé n'est pas le même, mais très proche d'une matière
	 * encore visible dont le créateur a toujours l'accès.
	 */
	public static String nameSimilarCanAsk(String existingName, String creatorDisplayName) {
		String subject = existingName == null || existingName.isBlank() ? "une matière" : existingName.trim();
		String who = creatorDisplayName == null || creatorDisplayName.isBlank()
				? "son créateur"
				: creatorDisplayName.trim();
		return "Ce nom ressemble à " + subject + ". Tu peux demander à " + who + NAME_CAN_ASK_SUFFIX;
	}

	/** Le professeur a déjà une matière très proche : pas de deuxième création. */
	public static String similarOwn(String existingName) {
		String subject = existingName == null || existingName.isBlank() ? "une matière" : existingName.trim();
		return "Tu as déjà une matière proche : " + subject + ".";
	}

	private final TeacherSubjectPort teacherSubjectPort;
	private final TeacherProfilePort teacherProfilePort;
	private final SchoolCatalogPort schoolCatalogPort;
	private final ActiveRolePort activeRolePort;
	private final TeacherQuestionPort teacherQuestionPort;

	public TeacherSubjectService(
			TeacherSubjectPort teacherSubjectPort,
			TeacherProfilePort teacherProfilePort,
			SchoolCatalogPort schoolCatalogPort,
			ActiveRolePort activeRolePort,
			TeacherQuestionPort teacherQuestionPort) {
		this.teacherSubjectPort = teacherSubjectPort;
		this.teacherProfilePort = teacherProfilePort;
		this.schoolCatalogPort = schoolCatalogPort;
		this.activeRolePort = activeRolePort;
		this.teacherQuestionPort = teacherQuestionPort;
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
			throw new AuthDomainException(AuthErrorCode.SUBJECT_RESTORABLE, REMOVED_RESTORABLE);
		}
		if (teacherSubjectPort.nameTaken(schoolId, grade, key, null)) {
			throw bad(takenMessage(userId, schoolId, grade, key));
		}
		String near = nearMessage(userId, schoolId, grade, key);
		if (near != null) {
			throw bad(near);
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
	public void requestShare(long userId, String activeRole, Long schoolId, String grade, String name) {
		requireTeacher(activeRole);
		requireAccount(userId);
		if (schoolId == null || grade == null || grade.isBlank()) {
			throw bad(CLASS_NOT_ON_PROFILE);
		}
		requireClass(userId, schoolId, grade);
		String key = nameKey(requiredName(name));
		StoredSubject subject = shareable(userId, schoolId, grade, key);
		if (teacherSubjectPort.hasPendingTransferBetween(subject.id(), userId, subject.userId())) {
			throw bad(ASK_PENDING);
		}
		teacherSubjectPort.insertTransferRequest(subject.id(), userId, subject.userId(), MODE_ASK);
	}

	@Override
	@Transactional
	public SubjectLine rename(long userId, String activeRole, long subjectId, String name) {
		requireTeacher(activeRole);
		requireAccount(userId);
		StoredSubject current = requireOwned(userId, subjectId);
		if (current.userId() != userId) {
			throw bad(RENAME_ONLY_CREATOR);
		}
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
		StoredSubject subject = requireOwned(userId, subjectId);
		if (subject.userId() == userId) {
			List<Long> holders = teacherSubjectPort.accessUserIds(subjectId);
			if (holders != null && holders.size() > 1) {
				throw bad(HANDOVER_REQUIRED);
			}
		}
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
		return new TeacherSubjectColleaguesView(eligibleColleagues(userId, subject));
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
		if (MODE_HANDOVER.equals(transferMode)) {
			requestHandover(userId, subject, colleagueUserId);
			return;
		}
		if (colleagueUserId == userId || teacherSubjectPort.hasAccess(subjectId, colleagueUserId)) {
			throw bad(ALREADY_HAS);
		}
		if (!isEligibleColleague(userId, subject, colleagueUserId)) {
			throw bad(COLLEAGUE_NOT_ELIGIBLE);
		}
		if (teacherSubjectPort.hasPendingTransfer(subjectId, colleagueUserId)) {
			throw bad(INVITE_PENDING);
		}
		teacherSubjectPort.insertTransferRequest(subjectId, userId, colleagueUserId, transferMode);
	}

	/**
	 * Le propriétaire désigne un co-détenteur pour reprendre la matière avant de partir.
	 */
	private void requestHandover(long userId, StoredSubject subject, long colleagueUserId) {
		if (subject.userId() != userId) {
			throw bad(HANDOVER_NOT_OWNER);
		}
		if (colleagueUserId == userId) {
			throw bad(COLLEAGUE_REQUIRED);
		}
		if (!teacherSubjectPort.hasAccess(subject.id(), colleagueUserId)) {
			throw bad(HANDOVER_NOT_HOLDER);
		}
		if (teacherSubjectPort.rejectedHandoverToUserIds(subject.id(), userId).contains(colleagueUserId)) {
			throw bad(HANDOVER_ALREADY_REFUSED);
		}
		if (teacherSubjectPort.hasPendingTransfer(subject.id(), colleagueUserId)
				|| teacherSubjectPort.hasPendingTransferBetween(subject.id(), userId, colleagueUserId)) {
			throw bad(INVITE_PENDING);
		}
		teacherSubjectPort.insertTransferRequest(subject.id(), userId, colleagueUserId, MODE_HANDOVER);
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
			String from = fromName == null ? "" : fromName;
			lines.add(new InviteLine(
					request.id(),
					row.id(),
					row.name(),
					row.schoolId(),
					schoolName(row.schoolId(), catalog),
					row.grade(),
					request.mode(),
					from,
					inviteMessage(request.mode(), from, row.name(), schoolName(row.schoolId(), catalog), row.grade())));
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
		if (MODE_ASK.equals(request.mode())) {
			acceptShareAsk(userId, request, subject);
			return;
		}
		if (MODE_HANDOVER.equals(request.mode())) {
			acceptHandover(userId, request, subject);
			return;
		}
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
			if (teacherSubjectPort.hasAccess(request.subjectId(), userId)) {
				throw bad(INVITE_STALE);
			}
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
		if (MODE_HANDOVER.equals(request.mode())) {
			afterHandoverRejected(request);
			return;
		}
		notifyInviteRejected(request);
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherDeactivateNoticesView listDeactivateNotices(long userId, String activeRole) {
		requireTeacher(activeRole);
		requireAccount(userId);
		List<NoticeLine> lines = new ArrayList<>();
		for (StoredDeactivateNotice notice : teacherSubjectPort.findOpenDeactivateNoticesTo(userId)) {
			Optional<StoredSubject> subject = teacherSubjectPort.findVisible(notice.subjectId());
			String subjectName = subject.map(StoredSubject::name).orElse("cette matière");
			long subjectId = subject.map(StoredSubject::id).orElse(notice.subjectId());
			String kind = notice.kind() == null || notice.kind().isBlank()
					? NOTICE_KIND_DEACTIVATE
					: notice.kind();
			String message = notice.message();
			if (message == null || message.isBlank()) {
				String fromName = teacherProfilePort.findAccount(notice.fromUserId())
						.map(TeacherAccount::displayName)
						.orElse("");
				message = deactivateNoticeMessage(
						fromName == null ? "" : fromName,
						subjectName,
						notice.deactivatedUserIds());
			}
			lines.add(new NoticeLine(notice.id(), subjectId, subjectName, kind, message));
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

	private List<ColleagueLine> eligibleColleagues(long userId, StoredSubject subject) {
		List<ColleagueLine> lines = new ArrayList<>();
		for (Long candidate : teacherProfilePort.findUserIdsWithClass(subject.schoolId(), subject.grade())) {
			if (candidate == null || candidate == userId) {
				continue;
			}
			if (!isAcceptedTeacher(candidate)) {
				continue;
			}
			if (teacherSubjectPort.hasAccess(subject.id(), candidate)) {
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

	private boolean isEligibleColleague(long userId, StoredSubject subject, long colleagueUserId) {
		if (colleagueUserId == userId || !isAcceptedTeacher(colleagueUserId)) {
			return false;
		}
		if (!hasClass(colleagueUserId, subject.schoolId(), subject.grade())) {
			return false;
		}
		return !teacherSubjectPort.hasAccess(subject.id(), colleagueUserId);
	}

	private boolean isAcceptedTeacher(long userId) {
		return activeRolePort.findSpaceRoles(userId).contains(ActiveRoles.TEACHER);
	}

	private TeacherSubjectsView grouped(long userId) {
		List<TeacherSchoolLink> links = teacherProfilePort.findSchools(userId);
		List<TeacherSubjectPort.SubjectListing> listing = teacherSubjectPort.findListing(userId,
				links.stream().map(TeacherSchoolLink::schoolId).distinct().toList(),
				links.stream().flatMap(link -> link.grades().stream()).distinct().toList());
		Map<Long, Long> questionCounts = teacherQuestionPort.countBySubjectIds(
				listing.stream().map(row -> row.subject().id()).distinct().toList());
		List<CatalogSchool> catalog = schoolCatalogPort.list();
		List<SchoolGroup> schools = new ArrayList<>();
		for (TeacherSchoolLink link : links) {
			List<String> grades = SchoolCurricula.ordered(link.curriculum(), link.grades());
			if (grades.isEmpty()) {
				grades = link.grades();
			}
			List<GradeGroup> groups = new ArrayList<>();
			for (String grade : grades) {
				groups.add(new GradeGroup(
						grade,
						listingLines(userId, link.schoolId(), grade, listing, questionCounts),
						addableCourses(link.schoolId(), grade, links, listing, catalog)));
			}
			schools.add(new SchoolGroup(link.schoolId(), schoolName(link.schoolId(), catalog), groups));
		}
		return new TeacherSubjectsView(schools);
	}

	private List<SubjectLine> listingLines(long userId, long schoolId, String grade,
			List<TeacherSubjectPort.SubjectListing> listing, Map<Long, Long> questionCounts) {
		Collator order = Collator.getInstance(Locale.FRENCH);
		order.setStrength(Collator.PRIMARY);
		return listing.stream().filter(row -> row.subject().schoolId() == schoolId && grade.equals(row.subject().grade()))
				.sorted((a, b) -> order.compare(a.subject().name(), b.subject().name()))
				.map(row -> new SubjectLine(row.subject().id(), row.subject().name(), row.subject().active(),
						row.subject().userId() == userId, row.followed(),
						row.others().stream()
								.map(TeacherSubjectService::sharedHolder).toList(),
						row.asked().stream()
								.map(TeacherSubjectService::sharedHolder).toList(),
						new SharedHolder(row.subject().userId(), row.ownerName()),
						questionCounts.getOrDefault(row.subject().id(), 0L)))
				.toList();
	}

	private static SharedHolder sharedHolder(AccessHolder holder) {
		return new SharedHolder(holder.userId(), holder.displayName());
	}

	private SubjectLine line(long userId, StoredSubject subject) {
		boolean followedByMe = teacherSubjectPort.hasAccess(subject.id(), userId);
		List<AccessHolder> holders = teacherSubjectPort.otherAccessHolders(subject.id(), userId);
		if (holders == null) {
			holders = List.of();
		}
		List<SharedHolder> shared = holders.stream()
				.map(holder -> new SharedHolder(holder.userId(), holder.displayName()))
				.toList();
		long questionCount = teacherQuestionPort.countBySubjectIds(List.of(subject.id()))
				.getOrDefault(subject.id(), 0L);
		return new SubjectLine(
				subject.id(),
				subject.name(),
				subject.active(),
				subject.userId() == userId,
				followedByMe,
				shared,
				shareAskedTo(userId, subject.id()),
				ownerOf(subject),
				questionCount);
	}

	/** Propriétaire enregistré : le professeur qui peut renommer. */
	private SharedHolder ownerOf(StoredSubject subject) {
		String name = "";
		Optional<TeacherAccount> account = teacherProfilePort.findAccount(subject.userId());
		if (account != null && account.isPresent()) {
			String found = account.get().displayName();
			name = found == null ? "" : found;
		}
		return new SharedHolder(subject.userId(), name);
	}

	/**
	 * Destinataires d'une demande de partage encore en attente, envoyée par ce professeur.
	 */
	private List<SharedHolder> shareAskedTo(long userId, long subjectId) {
		List<StoredTransferRequest> pending = teacherSubjectPort.findPendingAsksFrom(subjectId, userId);
		if (pending == null || pending.isEmpty()) {
			return List.of();
		}
		List<SharedHolder> asked = new ArrayList<>();
		for (StoredTransferRequest request : pending) {
			if (request == null || !MODE_ASK.equals(request.mode())) {
				continue;
			}
			String name = teacherProfilePort.findAccount(request.toUserId())
					.map(TeacherAccount::displayName)
					.orElse("");
			asked.add(new SharedHolder(request.toUserId(), name == null ? "" : name));
		}
		return List.copyOf(asked);
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

	/**
	 * Matières déjà créées dans une autre année, absentes de l'année affichée.
	 *
	 * <p><b>Business :</b> le professeur reconnaît une matière par le code postal,
	 * les années où elle existe et le nom de l'école. Un nom déjà présent dans
	 * l'année en cours n'est pas reproposé.
	 * <p><b>Technique :</b> regroupement par école et nom en minuscules. Les années
	 * suivent l'ordre du profil. Le texte affiché est {@code label}.
	 * <p><b>Métier / flow :</b> champ de {@code GET /api/int/teacher/subjects},
	 * calculé après le regroupement des classes. Choisir une ligne côté écran
	 * remplit le nom ; la création reste {@code POST}.
	 */
	private List<AddableCourse> addableCourses(
			long targetSchoolId,
			String targetGrade,
			List<TeacherSchoolLink> links,
			List<TeacherSubjectPort.SubjectListing> listing,
			List<CatalogSchool> catalog) {
		Set<String> present = new HashSet<>();
		for (TeacherSubjectPort.SubjectListing row : listing) {
			StoredSubject subject = row.subject();
			if (subject.schoolId() == targetSchoolId && targetGrade.equals(subject.grade())) {
				present.add(nameKey(subject.name()));
			}
		}
		Map<String, CourseDraft> drafts = new LinkedHashMap<>();
		for (TeacherSchoolLink link : links) {
			List<String> grades = SchoolCurricula.ordered(link.curriculum(), link.grades());
			if (grades.isEmpty()) {
				grades = link.grades();
			}
			String sourceSchool = schoolName(link.schoolId(), catalog);
			String postal = postalCode(link.schoolId(), catalog);
			for (String grade : grades) {
				for (TeacherSubjectPort.SubjectListing row : listing) {
					StoredSubject subject = row.subject();
					if (subject.schoolId() != link.schoolId() || !grade.equals(subject.grade())) {
						continue;
					}
					String key = nameKey(subject.name());
					if (present.contains(key)) {
						continue;
					}
					String groupKey = link.schoolId() + "\n" + key;
					CourseDraft draft = drafts.get(groupKey);
					if (draft == null) {
						draft = new CourseDraft(subject.name(), link.schoolId(), sourceSchool, postal);
						drafts.put(groupKey, draft);
					}
					draft.addGrade(grade);
				}
			}
		}
		List<AddableCourse> courses = new ArrayList<>();
		for (CourseDraft draft : drafts.values()) {
			courses.add(draft.toCourse());
		}
		Collator order = Collator.getInstance(Locale.FRENCH);
		order.setStrength(Collator.PRIMARY);
		courses.sort((left, right) -> {
			int byName = order.compare(left.name(), right.name());
			if (byName != 0) {
				return byName;
			}
			return order.compare(left.schoolName(), right.schoolName());
		});
		return List.copyOf(courses);
	}

	private static String postalCode(long schoolId, List<CatalogSchool> catalog) {
		if (catalog == null) {
			return "";
		}
		for (CatalogSchool school : catalog) {
			if (school.id() == schoolId) {
				return school.postalCode() == null ? "" : school.postalCode();
			}
		}
		return "";
	}

	private static String courseLabel(String name, String postalCode, List<String> grades, String schoolName) {
		List<String> parts = new ArrayList<>();
		parts.add(name);
		if (postalCode != null && !postalCode.isBlank()) {
			parts.add(postalCode.trim());
		}
		if (!grades.isEmpty()) {
			parts.add(String.join(", ", grades));
		}
		if (schoolName != null && !schoolName.isBlank()) {
			parts.add(schoolName);
		}
		return String.join(" — ", parts);
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
		if (!MODE_MOVE.equals(trimmed)
				&& !MODE_SHARE.equals(trimmed)
				&& !MODE_HANDOVER.equals(trimmed)) {
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
	 * Le créateur accepte : le demandeur reçoit un accès, le créateur garde le sien.
	 */
	private void acceptShareAsk(long userId, StoredTransferRequest request, StoredSubject subject) {
		if (subject.userId() != userId || !teacherSubjectPort.hasAccess(request.subjectId(), userId)) {
			throw bad(INVITE_STALE);
		}
		if (!hasClass(request.fromUserId(), subject.schoolId(), subject.grade())) {
			throw bad(INVITE_STALE);
		}
		if (!teacherSubjectPort.hasAccess(request.subjectId(), request.fromUserId())) {
			teacherSubjectPort.addAccess(request.subjectId(), request.fromUserId());
		}
		teacherSubjectPort.markTransferAccepted(request.id());
	}

	/**
	 * Le co-détenteur accepte : il devient propriétaire, l'ancien propriétaire quitte.
	 */
	private void acceptHandover(long userId, StoredTransferRequest request, StoredSubject subject) {
		if (subject.userId() != request.fromUserId()) {
			throw bad(INVITE_STALE);
		}
		if (!teacherSubjectPort.hasAccess(request.subjectId(), request.fromUserId())
				|| !teacherSubjectPort.hasAccess(request.subjectId(), userId)) {
			throw bad(INVITE_STALE);
		}
		if (!hasClass(userId, subject.schoolId(), subject.grade())) {
			throw bad(INVITE_STALE);
		}
		teacherSubjectPort.assignOwnership(request.subjectId(), userId);
		teacherSubjectPort.markTransferAccepted(request.id());
		teacherSubjectPort.cancelPendingTransfers(request.subjectId(), request.id());
		if (!teacherSubjectPort.hide(request.fromUserId(), request.subjectId())) {
			throw bad(INVITE_STALE);
		}
		String acceptorName = teacherProfilePort.findAccount(userId)
				.map(TeacherAccount::displayName)
				.orElse("Un collègue");
		String who = acceptorName == null || acceptorName.isBlank() ? "Un collègue" : acceptorName.trim();
		teacherSubjectPort.insertSubjectNotice(
				request.subjectId(),
				userId,
				request.fromUserId(),
				NOTICE_KIND_HANDOVER_ACCEPTED,
				who + " a accepté de reprendre " + subject.name() + ". Tu n'y as plus accès.");
	}

	/**
	 * Refus move / share / ask : l'émetteur reçoit une notice dismissible.
	 */
	private void notifyInviteRejected(StoredTransferRequest request) {
		Optional<StoredSubject> found = teacherSubjectPort.findVisible(request.subjectId());
		String subjectName = found.map(StoredSubject::name).orElse("cette matière");
		String rejector = teacherProfilePort.findAccount(request.toUserId())
				.map(TeacherAccount::displayName)
				.orElse("Un collègue");
		String who = rejector == null || rejector.isBlank() ? "Un collègue" : rejector.trim();
		String message;
		if (MODE_MOVE.equals(request.mode())) {
			message = who + " a refusé le déplacement de " + subjectName + ".";
		} else if (MODE_ASK.equals(request.mode())) {
			message = who + " a refusé de partager " + subjectName + " avec toi.";
		} else {
			message = who + " a refusé de partager " + subjectName + ".";
		}
		teacherSubjectPort.insertSubjectNotice(
				request.subjectId(),
				request.toUserId(),
				request.fromUserId(),
				NOTICE_KIND_INVITE_REJECTED,
				message);
	}

	/**
	 * Refus de reprise : alerte à l'émetteur ; si plus personne d'éligible, matière retirée.
	 */
	private void afterHandoverRejected(StoredTransferRequest request) {
		Optional<StoredSubject> found = teacherSubjectPort.findVisible(request.subjectId());
		String subjectName = found.map(StoredSubject::name).orElse("cette matière");
		String rejector = teacherProfilePort.findAccount(request.toUserId())
				.map(TeacherAccount::displayName)
				.orElse("Un collègue");
		String who = rejector == null || rejector.isBlank() ? "Un collègue" : rejector.trim();
		boolean removeAll = false;
		if (found.isPresent() && found.get().userId() == request.fromUserId()) {
			StoredSubject subject = found.get();
			Set<Long> holders = new LinkedHashSet<>(teacherSubjectPort.accessUserIds(subject.id()));
			holders.remove(request.fromUserId());
			Set<Long> rejected = new LinkedHashSet<>(
					teacherSubjectPort.rejectedHandoverToUserIds(subject.id(), request.fromUserId()));
			removeAll = true;
			for (Long holder : holders) {
				if (!rejected.contains(holder)) {
					removeAll = false;
					break;
				}
			}
			if (removeAll) {
				teacherSubjectPort.hideCompletely(subject.id());
				teacherSubjectPort.cancelPendingTransfers(subject.id(), null);
			}
		}
		String message = removeAll
				? who + " a refusé de reprendre " + subjectName
						+ ". Plus aucun collègue n'accepte : la matière a été retirée."
				: who + " a refusé de reprendre " + subjectName + ". Tu peux en proposer un autre.";
		teacherSubjectPort.insertSubjectNotice(
				request.subjectId(),
				request.toUserId(),
				request.fromUserId(),
				NOTICE_KIND_HANDOVER_REJECTED,
				message);
	}

	private static String inviteMessage(
			String mode, String fromDisplayName, String subjectName, String schoolName, String grade) {
		String from = fromDisplayName == null || fromDisplayName.isBlank() ? "Un collègue" : fromDisplayName.trim();
		String place = schoolName == null || schoolName.isBlank()
				? subjectName + " (" + grade + ")"
				: subjectName + " (" + schoolName + ", " + grade + ")";
		if (MODE_ASK.equals(mode)) {
			return from + " demande à voir aussi " + place + ". Si tu acceptes, vous la partagerez.";
		}
		if (MODE_HANDOVER.equals(mode)) {
			return from + " te demande de reprendre " + place
					+ ". Si tu acceptes, tu en seras le propriétaire et il quittera la matière.";
		}
		String action = MODE_MOVE.equals(mode) ? "déplacer" : "partager";
		return from + " propose de " + action + " " + place + ".";
	}

	/**
	 * Message quand le nom est déjà pris. Propose une demande de partage seulement
	 * si un autre professeur, encore détenteur, a créé la matière.
	 */
	private String takenMessage(long userId, long schoolId, String grade, String key) {
		Optional<StoredSubject> existing = teacherSubjectPort.findVisibleByName(schoolId, grade, key);
		if (existing == null || existing.isEmpty()) {
			return NAME_TAKEN;
		}
		StoredSubject subject = existing.get();
		if (subject.userId() == userId || teacherSubjectPort.hasAccess(subject.id(), userId)) {
			return NAME_TAKEN;
		}
		if (!teacherSubjectPort.hasAccess(subject.id(), subject.userId())) {
			return OWNER_WITHOUT_ACCESS;
		}
		if (teacherSubjectPort.hasPendingTransferBetween(subject.id(), userId, subject.userId())) {
			return ASK_PENDING;
		}
		String creator = teacherProfilePort.findAccount(subject.userId())
				.map(TeacherAccount::displayName)
				.orElse("");
		throw new AuthDomainException(AuthErrorCode.SUBJECT_SHARE_AVAILABLE, nameCanAsk(creator));
	}

	/**
	 * Matière visible qu'un autre professeur a créée et qu'il détient encore.
	 * Le demandeur n'y a pas accès.
	 */
	private StoredSubject shareable(long userId, long schoolId, String grade, String key) {
		Optional<StoredSubject> existing = teacherSubjectPort.findVisibleByName(schoolId, grade, key);
		if (existing != null && existing.isPresent()) {
			return requireShareable(userId, existing.get(), false);
		}
		StoredSubject near = closestSimilar(schoolId, grade, key).orElse(null);
		if (near == null) {
			throw missing();
		}
		return requireShareable(userId, near, true);
	}

	/**
	 * Le demandeur n'a pas la matière. Le créateur enregistré la possède encore.
	 *
	 * @param similar vrai si le nom tapé n'est pas exactement celui de la matière
	 */
	private StoredSubject requireShareable(long userId, StoredSubject subject, boolean similar) {
		if (subject.userId() == userId || teacherSubjectPort.hasAccess(subject.id(), userId)) {
			throw bad(similar ? similarOwn(subject.name()) : NAME_TAKEN);
		}
		if (!teacherSubjectPort.hasAccess(subject.id(), subject.userId())) {
			throw bad(OWNER_WITHOUT_ACCESS);
		}
		return subject;
	}

	/**
	 * Bloque la création si un nom très proche est encore visible.
	 * Une matière retirée ne passe pas ici : seul le nom exact propose la réactivation.
	 *
	 * @return message à afficher, ou null pour laisser créer
	 */
	private String nearMessage(long userId, long schoolId, String grade, String key) {
		Optional<StoredSubject> near = closestSimilar(schoolId, grade, key);
		if (near == null || near.isEmpty()) {
			return null;
		}
		StoredSubject subject = near.get();
		if (subject.userId() == userId || teacherSubjectPort.hasAccess(subject.id(), userId)) {
			return similarOwn(subject.name());
		}
		if (!teacherSubjectPort.hasAccess(subject.id(), subject.userId())) {
			return null;
		}
		if (teacherSubjectPort.hasPendingTransferBetween(subject.id(), userId, subject.userId())) {
			return ASK_PENDING;
		}
		String creator = teacherProfilePort.findAccount(subject.userId())
				.map(TeacherAccount::displayName)
				.orElse("");
		throw new AuthDomainException(AuthErrorCode.SUBJECT_SHARE_AVAILABLE, nameSimilarCanAsk(subject.name(), creator));
	}

	/** La matière visible la plus proche, hors nom exactement identique. */
	private Optional<StoredSubject> closestSimilar(long schoolId, String grade, String key) {
		List<StoredSubject> rows = teacherSubjectPort.findVisibleInClass(schoolId, grade);
		if (rows == null || rows.isEmpty()) {
			return Optional.empty();
		}
		StoredSubject best = null;
		int bestScore = Integer.MAX_VALUE;
		for (StoredSubject row : rows) {
			if (row == null || row.name() == null) {
				continue;
			}
			String other = nameKey(row.name());
			if (key.equals(other)) {
				continue;
			}
			Integer score = similarityScore(key, other);
			if (score == null || score >= bestScore) {
				continue;
			}
			best = row;
			bestScore = score;
		}
		return best == null ? Optional.empty() : Optional.of(best);
	}

	/**
	 * Score de proximité, plus petit = plus proche. Null si le nom n'est pas assez proche.
	 * Les accents et la ponctuation ne comptent pas. Un mot trop court ne déclenche rien.
	 */
	private static Integer similarityScore(String typedKey, String existingKey) {
		String typed = fold(typedKey);
		String existing = fold(existingKey);
		if (typed.isEmpty() || existing.isEmpty()) {
			return null;
		}
		if (typed.equals(existing)) {
			return 0;
		}
		if (typed.length() < 4 || existing.length() < 4) {
			return null;
		}
		int distance = levenshtein(typed, existing);
		int shorter = Math.min(typed.length(), existing.length());
		if (distance <= 1 && shorter >= 5) {
			return distance;
		}
		if (distance <= 2 && shorter >= 8) {
			return distance;
		}
		int prefix = commonPrefix(typed, existing);
		if (prefix >= 4 && prefix >= shorter - 1) {
			return Math.max(typed.length(), existing.length()) - prefix;
		}
		return null;
	}

	/** Minuscules, sans accent ni signe, pour comparer deux noms. */
	private static String fold(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
		StringBuilder folded = new StringBuilder(normalized.length());
		for (int i = 0; i < normalized.length(); i++) {
			char c = normalized.charAt(i);
			if (Character.getType(c) == Character.NON_SPACING_MARK) {
				continue;
			}
			if (Character.isLetterOrDigit(c)) {
				folded.append(Character.toLowerCase(c));
			}
		}
		return folded.toString();
	}

	private static int commonPrefix(String left, String right) {
		int limit = Math.min(left.length(), right.length());
		int i = 0;
		while (i < limit && left.charAt(i) == right.charAt(i)) {
			i++;
		}
		return i;
	}

	private static int levenshtein(String left, String right) {
		int n = left.length();
		int m = right.length();
		int[] previous = new int[m + 1];
		int[] current = new int[m + 1];
		for (int j = 0; j <= m; j++) {
			previous[j] = j;
		}
		for (int i = 1; i <= n; i++) {
			current[0] = i;
			char c = left.charAt(i - 1);
			for (int j = 1; j <= m; j++) {
				int cost = c == right.charAt(j - 1) ? 0 : 1;
				current[j] = Math.min(
						Math.min(current[j - 1] + 1, previous[j] + 1),
						previous[j - 1] + cost);
			}
			int[] swap = previous;
			previous = current;
			current = swap;
		}
		return previous[m];
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

	/** Accumulation d'une matière (même école, même nom) sur plusieurs années. */
	private static final class CourseDraft {

		private final String name;
		private final long schoolId;
		private final String schoolName;
		private final String postalCode;
		private final List<String> grades = new ArrayList<>();

		private CourseDraft(String name, long schoolId, String schoolName, String postalCode) {
			this.name = name;
			this.schoolId = schoolId;
			this.schoolName = schoolName;
			this.postalCode = postalCode;
		}

		private void addGrade(String grade) {
			if (!grades.contains(grade)) {
				grades.add(grade);
			}
		}

		private AddableCourse toCourse() {
			List<String> years = List.copyOf(grades);
			return new AddableCourse(
					name,
					schoolId,
					schoolName,
					postalCode,
					years,
					courseLabel(name, postalCode, years, schoolName));
		}
	}
}


package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherAccount;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherSchoolLink;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort.AccessHolder;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredDeactivateNotice;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredSubject;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredTransferRequest;
import com.appv4.auth.application.result.TeacherSubjectsView;
import com.appv4.auth.application.result.TeacherSubjectsView.SharedHolder;
import com.appv4.auth.application.result.TeacherSubjectsView.SubjectLine;
import com.appv4.auth.application.result.TeacherTransferInvitesView.InviteLine;

class TeacherSubjectServiceTest {

	private static final long TEACHER_ID = 4L;
	private static final long OTHER_ID = 10L;
	private static final long THIRD_ID = 11L;
	private static final long PARENT_ID = 20L;
	private static final long FRENCH_SCHOOL = 1L;
	private static final long BELGIAN_SCHOOL = 2L;

	private MemoryProfile profiles;
	private MemorySubjects subjects;
	private MemoryRoles roles;
	private TeacherSubjectService service;

	@BeforeEach
	void setUp() {
		profiles = new MemoryProfile();
		subjects = new MemorySubjects();
		roles = new MemoryRoles();
		profiles.seed(TEACHER_ID, "prof@example.com", "Anne");
		profiles.seed(OTHER_ID, "camille@example.com", "Camille");
		profiles.seed(THIRD_ID, "luc@example.com", "Luc");
		profiles.seed(PARENT_ID, "parent@example.com", "Parent");
		roles.grantTeacher(TEACHER_ID);
		roles.grantTeacher(OTHER_ID);
		roles.grantTeacher(THIRD_ID);
		subjects.nameOf(TEACHER_ID, "Anne");
		subjects.nameOf(OTHER_ID, "Camille");
		subjects.nameOf(THIRD_ID, "Luc");
		profiles.put(TEACHER_ID, List.of(
				new TeacherSchoolLink(FRENCH_SCHOOL, SchoolCurricula.FRANCE, List.of("CE1", "CE2")),
				new TeacherSchoolLink(BELGIAN_SCHOOL, SchoolCurricula.BELGIUM_FR, List.of("3e primaire"))));
		profiles.put(OTHER_ID, List.of(
				new TeacherSchoolLink(FRENCH_SCHOOL, SchoolCurricula.FRANCE, List.of("CE1"))));
		profiles.put(THIRD_ID, List.of(
				new TeacherSchoolLink(FRENCH_SCHOOL, SchoolCurricula.FRANCE, List.of("CE2"))));
		List<CatalogSchool> catalog = List.of(
				new CatalogSchool(FRENCH_SCHOOL, "École française", "France", "Primaire"),
				new CatalogSchool(BELGIAN_SCHOOL, "École belge", "Belgique", "Primaire"));
		service = new TeacherSubjectService(subjects, profiles, () -> catalog, roles);
	}

	@Test
	void emptyProfileHasNoClassAndNoFieldToFill() {
		profiles.put(TEACHER_ID, List.of());

		TeacherSubjectsView view = service.list(TEACHER_ID, "teacher");

		assertThat(view.schools()).isEmpty();
		assertThatThrownBy(() -> service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Français"))
				.hasMessage(TeacherSubjectService.CLASS_NOT_ON_PROFILE);
		assertThat(subjects.all()).isEmpty();
	}

	@Test
	void createsTwoSubjectsInOneGradeAndTheSameNameInAnother() {
		SubjectLine french = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "  Français  ");
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");
		SubjectLine mathsCe2 = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE2", "Mathématiques");

		TeacherSubjectsView view = service.list(TEACHER_ID, "teacher");

		assertThat(view.schools()).hasSize(2);
		assertThat(view.schools().get(0).schoolName()).isEqualTo("École française");
		assertThat(view.schools().get(0).grades()).extracting(grade -> grade.grade()).containsExactly("CE1", "CE2");
		assertThat(view.schools().get(0).grades().get(0).subjects())
				.extracting(SubjectLine::name)
				.containsExactly("Français", "Mathématiques");
		assertThat(view.schools().get(0).grades().get(1).subjects())
				.extracting(SubjectLine::name)
				.containsExactly("Mathématiques");
		assertThat(view.schools().get(1).grades().get(0).subjects()).isEmpty();
		assertThat(french.name()).isEqualTo("Français");
		assertThat(french.sharedWith()).isEmpty();
		assertThat(maths.id()).isNotEqualTo(mathsCe2.id());
		assertThat(subjects.all()).hasSize(3);
	}

	@Test
	void sameNameIsAllowedInAnotherSchoolButNotTwiceInTheSameClass() {
		service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Lecture");

		assertThatThrownBy(() -> service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "   "))
				.hasMessage(TeacherSubjectService.NAME_REQUIRED);
		assertThatThrownBy(() -> service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "x".repeat(101)))
				.hasMessage(TeacherSubjectService.NAME_TOO_LONG);
		assertThatThrownBy(() -> service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "lecture"))
				.hasMessage(TeacherSubjectService.NAME_TAKEN);

		service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Éveil");
		assertThatThrownBy(() -> service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "éveil"))
				.hasMessage(TeacherSubjectService.NAME_TAKEN);

		SubjectLine otherClass = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE2", "Lecture");
		SubjectLine otherSchool = service.create(TEACHER_ID, "teacher", BELGIAN_SCHOOL, "3e primaire", "Lecture");

		assertThat(otherClass.name()).isEqualTo("Lecture");
		assertThat(otherSchool.name()).isEqualTo("Lecture");
		assertThat(names(FRENCH_SCHOOL, "CE1")).containsExactly("Éveil", "Lecture");
	}

	@Test
	void renameStaysInTheSameClass() {
		SubjectLine french = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Français");
		service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE2", "Mathématiques");

		SubjectLine renamed = service.rename(TEACHER_ID, "teacher", french.id(), "  Lecture  ");

		assertThat(renamed.name()).isEqualTo("Lecture");
		assertThat(names(FRENCH_SCHOOL, "CE1")).containsExactly("Lecture");
		assertThat(names(FRENCH_SCHOOL, "CE2")).containsExactly("Mathématiques");
		StoredSubject stored = subjects.find(french.id());
		assertThat(stored.schoolId()).isEqualTo(FRENCH_SCHOOL);
		assertThat(stored.grade()).isEqualTo("CE1");
	}

	@Test
	void renameRefusesANameAlreadyUsedInTheSameClass() {
		SubjectLine french = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Français");
		service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Lecture");

		assertThatThrownBy(() -> service.rename(TEACHER_ID, "teacher", french.id(), "lecture"))
				.hasMessage(TeacherSubjectService.NAME_TAKEN);
		assertThat(names(FRENCH_SCHOOL, "CE1")).containsExactly("Français", "Lecture");
	}

	@Test
	void removeDeletesOnlyThatSubject() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");
		service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Lecture");
		service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE2", "Mathématiques");

		service.remove(TEACHER_ID, "teacher", maths.id());

		assertThat(names(FRENCH_SCHOOL, "CE1")).containsExactly("Lecture");
		assertThat(names(FRENCH_SCHOOL, "CE2")).containsExactly("Mathématiques");
		assertThat(subjects.stored(maths.id())).isTrue();
		assertThat(profiles.schoolsOf(TEACHER_ID).get(0).grades()).containsExactly("CE1", "CE2");
	}

	@Test
	void deactivateStaysListedAndRemovingOffersTheSameSubjectBack() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");

		service.deactivate(TEACHER_ID, "teacher", maths.id(), List.of());
		assertThat(service.list(TEACHER_ID, "teacher").schools().get(0).grades().get(0).subjects())
				.extracting(SubjectLine::name, SubjectLine::active, SubjectLine::ownedByMe)
				.containsExactly(org.assertj.core.groups.Tuple.tuple("Mathématiques", false, true));
		assertThatThrownBy(() -> service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "mathématiques"))
				.hasMessage(TeacherSubjectService.NAME_TAKEN);

		service.reactivate(TEACHER_ID, "teacher", maths.id());
		assertThat(service.list(TEACHER_ID, "teacher").schools().get(0).grades().get(0).subjects().get(0).active())
				.isTrue();

		service.remove(TEACHER_ID, "teacher", maths.id());
		assertThat(names(FRENCH_SCHOOL, "CE1")).isEmpty();
		assertThat(subjects.stored(maths.id())).isTrue();
		assertThat(subjects.hidden(maths.id())).isTrue();
		assertThatThrownBy(() -> service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "mathématiques"))
				.hasMessage(TeacherSubjectService.REMOVED_RESTORABLE);

		SubjectLine again = service.restore(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "  Mathématiques  ");
		assertThat(again.id()).isEqualTo(maths.id());
		assertThat(again.name()).isEqualTo("Mathématiques");
		assertThat(again.active()).isTrue();
		assertThat(again.ownedByMe()).isTrue();
		assertThat(names(FRENCH_SCHOOL, "CE1")).containsExactly("Mathématiques");
	}

	@Test
	void anotherTeacherDoesNotSeeOrChangeTheseSubjects() {
		SubjectLine lecture = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Lecture");

		TeacherSubjectsView other = service.list(OTHER_ID, "teacher");
		assertThat(other.schools()).hasSize(1);
		assertThat(other.schools().get(0).grades().get(0).subjects()).isEmpty();

		service.create(OTHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Calcul");

		assertThat(namesOf(TEACHER_ID, FRENCH_SCHOOL, "CE1")).containsExactly("Lecture");
		assertThat(namesOf(OTHER_ID, FRENCH_SCHOOL, "CE1")).containsExactly("Calcul");
		assertThatThrownBy(() -> service.rename(OTHER_ID, "teacher", lecture.id(), "Autre"))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.NOT_FOUND);
		assertThatThrownBy(() -> service.remove(OTHER_ID, "teacher", lecture.id()))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.NOT_FOUND);
		assertThat(namesOf(TEACHER_ID, FRENCH_SCHOOL, "CE1")).containsExactly("Lecture");
	}

	@Test
	void anotherTeacherCannotCreateTheSameNameInTheSameClass() {
		service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");
		profiles.put(OTHER_ID, List.of(
				new TeacherSchoolLink(FRENCH_SCHOOL, SchoolCurricula.FRANCE, List.of("CE1", "CE2"))));

		assertThatThrownBy(() -> service.create(OTHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "mathématiques"))
				.hasMessage(TeacherSubjectService.NAME_TAKEN);
		assertThat(namesOf(OTHER_ID, FRENCH_SCHOOL, "CE1")).isEmpty();
		assertThat(namesOf(TEACHER_ID, FRENCH_SCHOOL, "CE1")).containsExactly("Mathématiques");

		SubjectLine sciences = service.create(OTHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Sciences");
		SubjectLine otherYear = service.create(OTHER_ID, "teacher", FRENCH_SCHOOL, "CE2", "Mathématiques");

		assertThat(sciences.name()).isEqualTo("Sciences");
		assertThat(otherYear.name()).isEqualTo("Mathématiques");
		assertThat(namesOf(TEACHER_ID, FRENCH_SCHOOL, "CE1")).containsExactly("Mathématiques");
	}

	@Test
	void anotherTeacherCanBringBackARemovedSubject() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");
		service.remove(TEACHER_ID, "teacher", maths.id());
		profiles.put(OTHER_ID, List.of(
				new TeacherSchoolLink(FRENCH_SCHOOL, SchoolCurricula.FRANCE, List.of("CE1", "CE2"))));

		SubjectLine back = service.restore(OTHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");

		assertThat(back.id()).isEqualTo(maths.id());
		assertThat(back.ownedByMe()).isFalse();
		assertThat(namesOf(OTHER_ID, FRENCH_SCHOOL, "CE1")).containsExactly("Mathématiques");
		assertThat(namesOf(TEACHER_ID, FRENCH_SCHOOL, "CE1")).isEmpty();
		assertThatThrownBy(() -> service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques"))
				.hasMessage(TeacherSubjectService.NAME_TAKEN);
	}

	@Test
	void shareInviteNeedsAcceptBeforeAccessChanges() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");

		assertThat(service.listColleagues(TEACHER_ID, "teacher", maths.id()).colleagues())
				.extracting(c -> c.userId(), c -> c.displayName())
				.containsExactly(org.assertj.core.groups.Tuple.tuple(OTHER_ID, "Camille"));

		service.requestTransfer(TEACHER_ID, "teacher", maths.id(), OTHER_ID, "share");

		assertThat(namesOf(OTHER_ID, FRENCH_SCHOOL, "CE1")).isEmpty();
		InviteLine invite = service.listTransferInvites(OTHER_ID, "teacher").invites().get(0);
		assertThat(invite.subjectName()).isEqualTo("Mathématiques");
		assertThat(invite.mode()).isEqualTo("share");
		assertThat(invite.fromDisplayName()).isEqualTo("Anne");
		assertThat(invite.schoolName()).isEqualTo("École française");
		assertThat(invite.grade()).isEqualTo("CE1");

		service.acceptTransfer(OTHER_ID, "teacher", invite.id());

		SubjectLine anne = lineOf(TEACHER_ID, maths.id());
		SubjectLine camille = lineOf(OTHER_ID, maths.id());
		assertThat(anne.id()).isEqualTo(camille.id());
		assertThat(anne.ownedByMe()).isTrue();
		assertThat(camille.ownedByMe()).isFalse();
		assertThat(anne.sharedWith()).extracting(SharedHolder::displayName).containsExactly("Camille");
		assertThat(camille.sharedWith()).extracting(SharedHolder::displayName).containsExactly("Anne");
		assertThat(service.listTransferInvites(OTHER_ID, "teacher").invites()).isEmpty();
	}

	@Test
	void sharedColleagueCanRenameAndDeactivateOnlyForSelf() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");
		service.requestTransfer(TEACHER_ID, "teacher", maths.id(), OTHER_ID, "share");
		long inviteId = service.listTransferInvites(OTHER_ID, "teacher").invites().get(0).id();
		service.acceptTransfer(OTHER_ID, "teacher", inviteId);

		service.rename(OTHER_ID, "teacher", maths.id(), "Maths");
		service.deactivate(OTHER_ID, "teacher", maths.id(), List.of());

		assertThat(lineOf(TEACHER_ID, maths.id()).name()).isEqualTo("Maths");
		assertThat(lineOf(TEACHER_ID, maths.id()).active()).isTrue();
		assertThat(lineOf(OTHER_ID, maths.id()).active()).isFalse();
		assertThat(service.listDeactivateNotices(TEACHER_ID, "teacher").notices()).isEmpty();

		assertThatThrownBy(() -> service.deactivate(OTHER_ID, "teacher", maths.id(), List.of(TEACHER_ID)))
				.hasMessage(TeacherSubjectService.ALSO_NOT_ALLOWED);

		service.reactivate(OTHER_ID, "teacher", maths.id());
		assertThat(lineOf(OTHER_ID, maths.id()).active()).isTrue();
	}

	@Test
	void ownerCanDeactivateSelfAndChosenColleagueWithNotice() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");
		service.requestTransfer(TEACHER_ID, "teacher", maths.id(), OTHER_ID, "share");
		service.acceptTransfer(
				OTHER_ID, "teacher", service.listTransferInvites(OTHER_ID, "teacher").invites().get(0).id());

		service.deactivate(TEACHER_ID, "teacher", maths.id(), List.of(OTHER_ID));

		assertThat(lineOf(TEACHER_ID, maths.id()).active()).isFalse();
		assertThat(lineOf(OTHER_ID, maths.id()).active()).isFalse();
		assertThat(service.listDeactivateNotices(OTHER_ID, "teacher").notices())
				.extracting(n -> n.message())
				.containsExactly("Anne a désactivé Mathématiques pour soi et Camille.");

		service.dismissDeactivateNotice(
				OTHER_ID, "teacher", service.listDeactivateNotices(OTHER_ID, "teacher").notices().get(0).id());
		assertThat(service.listDeactivateNotices(OTHER_ID, "teacher").notices()).isEmpty();
	}

	@Test
	void ownerDeactivateSelfOnlyStillNotifiesColleague() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");
		service.requestTransfer(TEACHER_ID, "teacher", maths.id(), OTHER_ID, "share");
		service.acceptTransfer(
				OTHER_ID, "teacher", service.listTransferInvites(OTHER_ID, "teacher").invites().get(0).id());

		service.deactivate(TEACHER_ID, "teacher", maths.id(), List.of());

		assertThat(lineOf(TEACHER_ID, maths.id()).active()).isFalse();
		assertThat(lineOf(OTHER_ID, maths.id()).active()).isTrue();
		assertThat(service.listDeactivateNotices(OTHER_ID, "teacher").notices())
				.extracting(n -> n.message())
				.containsExactly("Anne a désactivé Mathématiques pour soi.");
	}

	@Test
	void rejectKeepsAccessUnchanged() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");
		service.requestTransfer(TEACHER_ID, "teacher", maths.id(), OTHER_ID, "share");
		long inviteId = service.listTransferInvites(OTHER_ID, "teacher").invites().get(0).id();
		service.rejectTransfer(OTHER_ID, "teacher", inviteId);

		assertThat(namesOf(OTHER_ID, FRENCH_SCHOOL, "CE1")).isEmpty();
		assertThat(lineOf(TEACHER_ID, maths.id()).sharedWith()).isEmpty();
		assertThat(lineOf(TEACHER_ID, maths.id()).ownedByMe()).isTrue();
		assertThat(service.listTransferInvites(OTHER_ID, "teacher").invites()).isEmpty();
	}

	@Test
	void moveInviteGivesSoleAccessAfterAccept() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");
		service.requestTransfer(TEACHER_ID, "teacher", maths.id(), OTHER_ID, "move");
		long inviteId = service.listTransferInvites(OTHER_ID, "teacher").invites().get(0).id();
		service.acceptTransfer(OTHER_ID, "teacher", inviteId);

		assertThat(namesOf(TEACHER_ID, FRENCH_SCHOOL, "CE1")).isEmpty();
		assertThat(lineOf(OTHER_ID, maths.id()).sharedWith()).isEmpty();
		assertThat(subjects.accessCount(maths.id())).isEqualTo(1);
		assertThat(subjects.find(maths.id()).userId()).isEqualTo(OTHER_ID);
	}

	@Test
	void moveAmongThreeLeavesOnlyChosenAfterAccept() {
		profiles.put(THIRD_ID, List.of(
				new TeacherSchoolLink(FRENCH_SCHOOL, SchoolCurricula.FRANCE, List.of("CE1"))));
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");
		service.requestTransfer(TEACHER_ID, "teacher", maths.id(), OTHER_ID, "share");
		service.acceptTransfer(OTHER_ID, "teacher",
				service.listTransferInvites(OTHER_ID, "teacher").invites().get(0).id());
		service.requestTransfer(TEACHER_ID, "teacher", maths.id(), THIRD_ID, "share");
		service.acceptTransfer(THIRD_ID, "teacher",
				service.listTransferInvites(THIRD_ID, "teacher").invites().get(0).id());

		service.requestTransfer(OTHER_ID, "teacher", maths.id(), TEACHER_ID, "move");
		service.acceptTransfer(TEACHER_ID, "teacher",
				service.listTransferInvites(TEACHER_ID, "teacher").invites().get(0).id());

		assertThat(namesOf(TEACHER_ID, FRENCH_SCHOOL, "CE1")).containsExactly("Mathématiques");
		assertThat(lineOf(TEACHER_ID, maths.id()).sharedWith()).isEmpty();
		assertThat(namesOf(OTHER_ID, FRENCH_SCHOOL, "CE1")).isEmpty();
		assertThat(namesOf(THIRD_ID, FRENCH_SCHOOL, "CE1")).isEmpty();
		assertThat(subjects.accessCount(maths.id())).isEqualTo(1);
	}

	@Test
	void transferValidatesColleagueAndPending() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");

		assertThatThrownBy(() -> service.requestTransfer(TEACHER_ID, "teacher", maths.id(), null, "share"))
				.hasMessage(TeacherSubjectService.COLLEAGUE_REQUIRED);
		assertThatThrownBy(() -> service.requestTransfer(TEACHER_ID, "teacher", maths.id(), OTHER_ID, "clone"))
				.hasMessage(TeacherSubjectService.MODE_REQUIRED);
		assertThatThrownBy(() -> service.requestTransfer(TEACHER_ID, "teacher", maths.id(), THIRD_ID, "move"))
				.hasMessage(TeacherSubjectService.COLLEAGUE_NOT_ELIGIBLE);
		assertThatThrownBy(() -> service.requestTransfer(TEACHER_ID, "teacher", maths.id(), TEACHER_ID, "share"))
				.hasMessage(TeacherSubjectService.ALREADY_HAS);

		service.requestTransfer(TEACHER_ID, "teacher", maths.id(), OTHER_ID, "share");
		assertThatThrownBy(() -> service.requestTransfer(TEACHER_ID, "teacher", maths.id(), OTHER_ID, "share"))
				.hasMessage(TeacherSubjectService.INVITE_PENDING);
		service.acceptTransfer(OTHER_ID, "teacher",
				service.listTransferInvites(OTHER_ID, "teacher").invites().get(0).id());
		assertThatThrownBy(() -> service.requestTransfer(TEACHER_ID, "teacher", maths.id(), OTHER_ID, "share"))
				.hasMessage(TeacherSubjectService.ALREADY_HAS);
	}

	@Test
	void gradeMissingFromTheProfileIsRefused() {
		assertThatThrownBy(() -> service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CM1", "Sciences"))
				.hasMessage(TeacherSubjectService.CLASS_NOT_ON_PROFILE);
		assertThatThrownBy(() -> service.create(TEACHER_ID, "teacher", 99L, "CE1", "Sciences"))
				.hasMessage(TeacherSubjectService.CLASS_NOT_ON_PROFILE);
		assertThat(subjects.all()).isEmpty();
	}

	@Test
	void parentCannotOpenThePage() {
		assertThatThrownBy(() -> service.list(TEACHER_ID, "parent"))
				.hasMessage(SpaceHello.DENIED);
		assertThatThrownBy(() -> service.create(TEACHER_ID, null, FRENCH_SCHOOL, "CE1", "Français"))
				.hasMessage(SpaceHello.DENIED);
		assertThat(subjects.all()).isEmpty();
	}

	private SubjectLine lineOf(long userId, long subjectId) {
		return service.list(userId, "teacher").schools().stream()
				.flatMap(school -> school.grades().stream())
				.flatMap(group -> group.subjects().stream())
				.filter(line -> line.id() == subjectId)
				.findFirst()
				.orElseThrow();
	}

	private List<String> names(long schoolId, String grade) {
		return namesOf(TEACHER_ID, schoolId, grade);
	}

	private List<String> namesOf(long userId, long schoolId, String grade) {
		return service.list(userId, "teacher").schools().stream()
				.filter(school -> school.schoolId() == schoolId)
				.flatMap(school -> school.grades().stream())
				.filter(group -> grade.equals(group.grade()))
				.flatMap(group -> group.subjects().stream())
				.map(SubjectLine::name)
				.toList();
	}

	private static final class MemoryProfile implements TeacherProfilePort {

		private final Map<Long, TeacherAccount> accounts = new HashMap<>();
		private final Map<Long, List<TeacherSchoolLink>> schools = new HashMap<>();

		void seed(long userId, String email, String displayName) {
			accounts.put(userId, new TeacherAccount(email, displayName));
		}

		void put(long userId, List<TeacherSchoolLink> links) {
			schools.put(userId, List.copyOf(links));
		}

		List<TeacherSchoolLink> schoolsOf(long userId) {
			return schools.getOrDefault(userId, List.of());
		}

		@Override
		public Optional<TeacherAccount> findAccount(long userId) {
			return Optional.ofNullable(accounts.get(userId));
		}

		@Override
		public void updateDisplayName(long userId, String displayName) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<TeacherSchoolLink> findSchools(long userId) {
			return schools.getOrDefault(userId, List.of());
		}

		@Override
		public void replaceSchools(long userId, List<TeacherSchoolLink> links) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Long> findUserIdsWithClass(long schoolId, String grade) {
			List<Long> ids = new ArrayList<>();
			for (Map.Entry<Long, List<TeacherSchoolLink>> entry : schools.entrySet()) {
				for (TeacherSchoolLink link : entry.getValue()) {
					if (link.schoolId() == schoolId && link.grades().contains(grade)) {
						ids.add(entry.getKey());
					}
				}
			}
			return ids;
		}
	}

	private static final class MemoryRoles implements ActiveRolePort {

		private final Set<Long> teachers = new HashSet<>();

		void grantTeacher(long userId) {
			teachers.add(userId);
		}

		@Override
		public List<String> findSpaceRoles(long userId) {
			List<String> roles = new ArrayList<>();
			if (teachers.contains(userId)) {
				roles.add(ActiveRoles.TEACHER);
			}
			return roles;
		}

		@Override
		public boolean hasPendingTeacherRequest(long userId) {
			return false;
		}

		@Override
		public String teacherRequestStatus(long userId) {
			return "none";
		}

		@Override
		public boolean hasAdminRole(long userId) {
			return false;
		}

		@Override
		public void grantAdmin(long userId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<String> findLastActiveRole(long userId) {
			return Optional.empty();
		}

		@Override
		public void saveLastActiveRole(long userId, String role) {
			throw new UnsupportedOperationException();
		}

		@Override
		public AuthzSnapshot loadAuthzSnapshot(long userId) {
			throw new UnsupportedOperationException();
		}
	}

	private static final class MemorySubjects implements TeacherSubjectPort {

		private final List<StoredSubject> rows = new ArrayList<>();
		private final Set<Long> hidden = new HashSet<>();
		private final Map<Long, Set<Long>> accessBySubject = new HashMap<>();
		private final Map<Long, Map<Long, Boolean>> accessActive = new HashMap<>();
		private final Map<Long, String> displayNames = new HashMap<>();
		private final List<StoredDeactivateNotice> notices = new ArrayList<>();
		private final List<StoredTransferRequest> transfers = new ArrayList<>();
		private long nextId = 1L;
		private long nextTransferId = 1L;
		private long nextNoticeId = 1L;

		void nameOf(long userId, String displayName) {
			displayNames.put(userId, displayName);
		}

		List<StoredSubject> all() {
			return List.copyOf(rows);
		}

		boolean stored(long id) {
			for (StoredSubject row : rows) {
				if (row.id() == id) {
					return true;
				}
			}
			return false;
		}

		boolean hidden(long id) {
			return hidden.contains(id);
		}

		int accessCount(long subjectId) {
			return accessBySubject.getOrDefault(subjectId, Set.of()).size();
		}

		StoredSubject find(long id) {
			for (StoredSubject row : rows) {
				if (row.id() == id) {
					return row;
				}
			}
			throw new AssertionError(id);
		}

		@Override
		public List<StoredSubject> findByUser(long userId) {
			List<StoredSubject> owned = new ArrayList<>();
			for (StoredSubject row : rows) {
				if (!hidden.contains(row.id())
						&& accessBySubject.getOrDefault(row.id(), Set.of()).contains(userId)) {
					owned.add(withAccessActive(row, userId));
				}
			}
			return owned;
		}

		@Override
		public StoredSubject insert(long userId, long schoolId, String grade, String name, String nameKey) {
			StoredSubject saved = new StoredSubject(nextId++, userId, schoolId, grade, name, true);
			rows.add(saved);
			accessBySubject.put(saved.id(), new HashSet<>(Set.of(userId)));
			accessActive.put(saved.id(), new HashMap<>(Map.of(userId, true)));
			return saved;
		}

		@Override
		public Optional<StoredSubject> findOwned(long userId, long subjectId) {
			for (StoredSubject row : rows) {
				if (row.id() == subjectId
						&& !hidden.contains(row.id())
						&& accessBySubject.getOrDefault(row.id(), Set.of()).contains(userId)) {
					return Optional.of(withAccessActive(row, userId));
				}
			}
			return Optional.empty();
		}

		@Override
		public Optional<StoredSubject> rename(long userId, long subjectId, String name, String nameKey) {
			for (int index = 0; index < rows.size(); index++) {
				StoredSubject row = rows.get(index);
				if (row.id() == subjectId
						&& !hidden.contains(row.id())
						&& accessBySubject.getOrDefault(row.id(), Set.of()).contains(userId)) {
					StoredSubject renamed = new StoredSubject(
							row.id(), row.userId(), row.schoolId(), row.grade(), name, row.active());
					rows.set(index, renamed);
					return Optional.of(withAccessActive(renamed, userId));
				}
			}
			return Optional.empty();
		}

		@Override
		public boolean deactivateAccesses(long actorUserId, long subjectId, List<Long> userIds) {
			if (!accessBySubject.getOrDefault(subjectId, Set.of()).contains(actorUserId)
					|| hidden.contains(subjectId)) {
				return false;
			}
			Map<Long, Boolean> actives = accessActive.computeIfAbsent(subjectId, key -> new HashMap<>());
			for (Long target : userIds) {
				if (target != null && accessBySubject.getOrDefault(subjectId, Set.of()).contains(target)) {
					actives.put(target, false);
				}
			}
			syncGlobalActive(subjectId);
			return true;
		}

		@Override
		public boolean reactivate(long userId, long subjectId) {
			if (!accessBySubject.getOrDefault(subjectId, Set.of()).contains(userId)
					|| hidden.contains(subjectId)) {
				return false;
			}
			accessActive.computeIfAbsent(subjectId, key -> new HashMap<>()).put(userId, true);
			syncGlobalActive(subjectId);
			return true;
		}

		@Override
		public Optional<StoredSubject> findRemovedByName(long schoolId, String grade, String nameKey) {
			StoredSubject match = null;
			for (StoredSubject row : rows) {
				if (row.schoolId() != schoolId || !grade.equals(row.grade())) {
					continue;
				}
				if (!nameKey.equals(row.name().toLowerCase(java.util.Locale.ROOT))) {
					continue;
				}
				boolean noAccess = accessBySubject.getOrDefault(row.id(), Set.of()).isEmpty();
				if (!hidden.contains(row.id()) && !noAccess) {
					continue;
				}
				match = row;
			}
			return Optional.ofNullable(match);
		}

		@Override
		public Optional<StoredSubject> restore(long userId, long subjectId) {
			for (int index = 0; index < rows.size(); index++) {
				StoredSubject row = rows.get(index);
				if (row.id() != subjectId) {
					continue;
				}
				boolean noAccess = accessBySubject.getOrDefault(row.id(), Set.of()).isEmpty();
				if (!hidden.contains(row.id()) && !noAccess) {
					return Optional.empty();
				}
				hidden.remove(row.id());
				StoredSubject restored = new StoredSubject(
						row.id(), row.userId(), row.schoolId(), row.grade(), row.name(), true);
				rows.set(index, restored);
				accessBySubject.computeIfAbsent(row.id(), key -> new HashSet<>()).add(userId);
				accessActive.computeIfAbsent(row.id(), key -> new HashMap<>()).put(userId, true);
				return Optional.of(withAccessActive(restored, userId));
			}
			return Optional.empty();
		}

		@Override
		public boolean hide(long userId, long subjectId) {
			Set<Long> holders = accessBySubject.get(subjectId);
			if (holders == null || !holders.remove(userId) || hidden.contains(subjectId)) {
				return false;
			}
			Map<Long, Boolean> actives = accessActive.get(subjectId);
			if (actives != null) {
				actives.remove(userId);
			}
			if (holders.isEmpty()) {
				hidden.add(subjectId);
			} else {
				syncGlobalActive(subjectId);
			}
			return true;
		}

		@Override
		public List<Long> accessUserIds(long subjectId) {
			return List.copyOf(accessBySubject.getOrDefault(subjectId, Set.of()));
		}

		@Override
		public List<AccessHolder> otherAccessHolders(long subjectId, long viewerUserId) {
			List<AccessHolder> holders = new ArrayList<>();
			for (Long holder : accessBySubject.getOrDefault(subjectId, Set.of())) {
				if (holder != viewerUserId) {
					holders.add(new AccessHolder(holder, displayNames.getOrDefault(holder, "")));
				}
			}
			holders.sort((left, right) -> java.text.Collator.getInstance(java.util.Locale.FRENCH)
					.compare(left.displayName(), right.displayName()));
			return List.copyOf(holders);
		}

		@Override
		public List<String> otherAccessNames(long subjectId, long viewerUserId) {
			return otherAccessHolders(subjectId, viewerUserId).stream()
					.map(AccessHolder::displayName)
					.toList();
		}

		@Override
		public List<String> allAccessNames(long subjectId) {
			List<String> names = new ArrayList<>();
			for (Long holder : accessBySubject.getOrDefault(subjectId, Set.of())) {
				names.add(displayNames.getOrDefault(holder, ""));
			}
			names.sort(java.text.Collator.getInstance(java.util.Locale.FRENCH));
			return List.copyOf(names);
		}

		@Override
		public void addAccess(long subjectId, long userId) {
			accessBySubject.computeIfAbsent(subjectId, key -> new HashSet<>()).add(userId);
			accessActive.computeIfAbsent(subjectId, key -> new HashMap<>()).putIfAbsent(userId, true);
		}

		@Override
		public void replaceAccessWith(long subjectId, long soleUserId) {
			accessBySubject.put(subjectId, new HashSet<>(Set.of(soleUserId)));
			accessActive.put(subjectId, new HashMap<>(Map.of(soleUserId, true)));
			for (int index = 0; index < rows.size(); index++) {
				StoredSubject row = rows.get(index);
				if (row.id() == subjectId) {
					rows.set(index, new StoredSubject(
							row.id(), soleUserId, row.schoolId(), row.grade(), row.name(), true));
					return;
				}
			}
		}

		@Override
		public boolean hasAccess(long subjectId, long userId) {
			return accessBySubject.getOrDefault(subjectId, Set.of()).contains(userId);
		}

		@Override
		public Optional<StoredSubject> findVisible(long subjectId) {
			for (StoredSubject row : rows) {
				if (row.id() == subjectId && !hidden.contains(row.id())) {
					return Optional.of(row);
				}
			}
			return Optional.empty();
		}

		@Override
		public StoredTransferRequest insertTransferRequest(
				long subjectId, long fromUserId, long toUserId, String mode) {
			StoredTransferRequest saved = new StoredTransferRequest(
					nextTransferId++, subjectId, fromUserId, toUserId, mode, "pending");
			transfers.add(saved);
			return saved;
		}

		@Override
		public boolean hasPendingTransfer(long subjectId, long toUserId) {
			for (StoredTransferRequest row : transfers) {
				if (row.subjectId() == subjectId && row.toUserId() == toUserId && "pending".equals(row.status())) {
					return true;
				}
			}
			return false;
		}

		@Override
		public List<StoredTransferRequest> findPendingTransfersTo(long toUserId) {
			List<StoredTransferRequest> found = new ArrayList<>();
			for (StoredTransferRequest row : transfers) {
				if (row.toUserId() == toUserId && "pending".equals(row.status())) {
					found.add(row);
				}
			}
			return found;
		}

		@Override
		public Optional<StoredTransferRequest> findPendingTransferForTarget(long requestId, long toUserId) {
			for (StoredTransferRequest row : transfers) {
				if (row.id() == requestId && row.toUserId() == toUserId && "pending".equals(row.status())) {
					return Optional.of(row);
				}
			}
			return Optional.empty();
		}

		@Override
		public void markTransferAccepted(long requestId) {
			replaceTransferStatus(requestId, "accepted");
		}

		@Override
		public void markTransferRejected(long requestId) {
			replaceTransferStatus(requestId, "rejected");
		}

		@Override
		public void cancelPendingTransfers(long subjectId, Long exceptRequestId) {
			for (int i = 0; i < transfers.size(); i++) {
				StoredTransferRequest row = transfers.get(i);
				if (row.subjectId() != subjectId || !"pending".equals(row.status())) {
					continue;
				}
				if (exceptRequestId != null && exceptRequestId == row.id()) {
					continue;
				}
				transfers.set(i, new StoredTransferRequest(
						row.id(), row.subjectId(), row.fromUserId(), row.toUserId(), row.mode(), "cancelled"));
			}
		}

		private void replaceTransferStatus(long requestId, String status) {
			for (int i = 0; i < transfers.size(); i++) {
				StoredTransferRequest row = transfers.get(i);
				if (row.id() == requestId) {
					transfers.set(i, new StoredTransferRequest(
							row.id(), row.subjectId(), row.fromUserId(), row.toUserId(), row.mode(), status));
					return;
				}
			}
		}

		@Override
		public void insertDeactivateNotices(
				long subjectId, long fromUserId, List<Long> toUserIds, List<Long> deactivatedUserIds) {
			for (Long toUserId : toUserIds) {
				if (toUserId == null || toUserId == fromUserId) {
					continue;
				}
				notices.add(new StoredDeactivateNotice(
						nextNoticeId++, subjectId, fromUserId, toUserId, List.copyOf(deactivatedUserIds)));
			}
		}

		@Override
		public List<StoredDeactivateNotice> findOpenDeactivateNoticesTo(long toUserId) {
			List<StoredDeactivateNotice> found = new ArrayList<>();
			for (StoredDeactivateNotice row : notices) {
				if (row.toUserId() == toUserId) {
					found.add(row);
				}
			}
			return found;
		}

		@Override
		public boolean dismissDeactivateNotice(long noticeId, long toUserId) {
			return notices.removeIf(row -> row.id() == noticeId && row.toUserId() == toUserId);
		}

		private StoredSubject withAccessActive(StoredSubject row, long userId) {
			boolean active = accessActive
					.getOrDefault(row.id(), Map.of())
					.getOrDefault(userId, true);
			return new StoredSubject(
					row.id(), row.userId(), row.schoolId(), row.grade(), row.name(), active);
		}

		private void syncGlobalActive(long subjectId) {
			boolean any = false;
			for (Boolean active : accessActive.getOrDefault(subjectId, Map.of()).values()) {
				if (Boolean.TRUE.equals(active)) {
					any = true;
					break;
				}
			}
			for (int index = 0; index < rows.size(); index++) {
				StoredSubject row = rows.get(index);
				if (row.id() == subjectId) {
					rows.set(index, new StoredSubject(
							row.id(), row.userId(), row.schoolId(), row.grade(), row.name(), any));
					return;
				}
			}
		}

		@Override
		public boolean nameTaken(long schoolId, String grade, String nameKey, Long exceptId) {
			for (StoredSubject row : rows) {
				if (row.schoolId() != schoolId || !grade.equals(row.grade())) {
					continue;
				}
				if (hidden.contains(row.id())) {
					continue;
				}
				if (exceptId != null && exceptId == row.id()) {
					continue;
				}
				if (nameKey.equals(row.name().toLowerCase(java.util.Locale.ROOT))) {
					return true;
				}
			}
			return false;
		}
	}
}

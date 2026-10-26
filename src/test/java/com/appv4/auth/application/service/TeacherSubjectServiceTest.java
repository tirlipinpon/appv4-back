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
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserLoginCandidate;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserSignupState;
import com.appv4.auth.application.port.out.AuthUserAccountPort.SessionState;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherAccount;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherSchoolLink;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredSubject;
import com.appv4.auth.application.result.TeacherSubjectsView;
import com.appv4.auth.application.result.TeacherSubjectsView.SubjectLine;

class TeacherSubjectServiceTest {

	private static final long TEACHER_ID = 4L;
	private static final long OTHER_ID = 10L;
	private static final long THIRD_ID = 11L;
	private static final long PARENT_ID = 20L;
	private static final long FRENCH_SCHOOL = 1L;
	private static final long BELGIAN_SCHOOL = 2L;

	private MemoryProfile profiles;
	private MemorySubjects subjects;
	private MemoryAccounts accounts;
	private MemoryRoles roles;
	private TeacherSubjectService service;

	@BeforeEach
	void setUp() {
		profiles = new MemoryProfile();
		subjects = new MemorySubjects();
		accounts = new MemoryAccounts();
		roles = new MemoryRoles();
		profiles.seed(TEACHER_ID, "prof@example.com", "Anne");
		profiles.seed(OTHER_ID, "camille@example.com", "Camille");
		profiles.seed(THIRD_ID, "luc@example.com", "Luc");
		profiles.seed(PARENT_ID, "parent@example.com", "Parent");
		accounts.seed(TEACHER_ID, "prof@example.com", "Anne", true);
		accounts.seed(OTHER_ID, "camille@example.com", "Camille", true);
		accounts.seed(THIRD_ID, "luc@example.com", "Luc", true);
		accounts.seed(PARENT_ID, "parent@example.com", "Parent", true);
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
		service = new TeacherSubjectService(subjects, profiles, () -> catalog, accounts, roles);
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
	void deactivateStaysOnTheTeacherPageAndHidingFreesTheName() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");

		service.deactivate(TEACHER_ID, "teacher", maths.id());
		assertThat(service.list(TEACHER_ID, "teacher").schools().get(0).grades().get(0).subjects())
				.extracting(SubjectLine::name, SubjectLine::active)
				.containsExactly(org.assertj.core.groups.Tuple.tuple("Mathématiques", false));
		assertThatThrownBy(() -> service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "mathématiques"))
				.hasMessage(TeacherSubjectService.NAME_TAKEN);

		service.reactivate(TEACHER_ID, "teacher", maths.id());
		assertThat(service.list(TEACHER_ID, "teacher").schools().get(0).grades().get(0).subjects().get(0).active())
				.isTrue();

		service.remove(TEACHER_ID, "teacher", maths.id());
		assertThat(names(FRENCH_SCHOOL, "CE1")).isEmpty();
		assertThat(subjects.stored(maths.id())).isTrue();
		SubjectLine again = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");
		assertThat(again.name()).isEqualTo("Mathématiques");
		assertThat(again.id()).isNotEqualTo(maths.id());
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
	void shareKeepsSameIdAndShowsSharedWith() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");

		service.transfer(TEACHER_ID, "teacher", maths.id(), "  Camille@Example.com ", "share");

		SubjectLine anne = lineOf(TEACHER_ID, maths.id());
		SubjectLine camille = lineOf(OTHER_ID, maths.id());
		assertThat(anne.id()).isEqualTo(camille.id());
		assertThat(anne.sharedWith()).containsExactly("Camille");
		assertThat(camille.sharedWith()).containsExactly("Anne");
		assertThat(subjects.accessCount(maths.id())).isEqualTo(2);
	}

	@Test
	void sharedColleagueCanRenameAndDeactivate() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");
		service.transfer(TEACHER_ID, "teacher", maths.id(), "camille@example.com", "share");

		service.rename(OTHER_ID, "teacher", maths.id(), "Maths");
		service.deactivate(OTHER_ID, "teacher", maths.id());

		assertThat(lineOf(TEACHER_ID, maths.id()).name()).isEqualTo("Maths");
		assertThat(lineOf(TEACHER_ID, maths.id()).active()).isFalse();
		service.reactivate(TEACHER_ID, "teacher", maths.id());
		assertThat(lineOf(OTHER_ID, maths.id()).active()).isTrue();
	}

	@Test
	void removeSharedAccessKeepsSubjectForColleague() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");
		service.transfer(TEACHER_ID, "teacher", maths.id(), "camille@example.com", "share");

		service.remove(TEACHER_ID, "teacher", maths.id());

		assertThat(namesOf(TEACHER_ID, FRENCH_SCHOOL, "CE1")).isEmpty();
		assertThat(lineOf(OTHER_ID, maths.id()).sharedWith()).isEmpty();
		assertThat(subjects.hidden(maths.id())).isFalse();
		assertThatThrownBy(() -> service.rename(TEACHER_ID, "teacher", maths.id(), "Autre"))
				.hasMessage(TeacherSubjectService.SUBJECT_MISSING);
	}

	@Test
	void moveGivesSoleAccessToColleague() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");

		service.transfer(TEACHER_ID, "teacher", maths.id(), "camille@example.com", "move");

		assertThat(namesOf(TEACHER_ID, FRENCH_SCHOOL, "CE1")).isEmpty();
		assertThat(lineOf(OTHER_ID, maths.id()).sharedWith()).isEmpty();
		assertThat(subjects.accessCount(maths.id())).isEqualTo(1);
		assertThat(subjects.find(maths.id()).userId()).isEqualTo(OTHER_ID);
		assertThatThrownBy(() -> service.transfer(
						TEACHER_ID, "teacher", maths.id(), "luc@example.com", "share"))
				.hasMessage(TeacherSubjectService.SUBJECT_MISSING);
	}

	@Test
	void moveAmongThreeLeavesOnlyTheChosenTeacher() {
		profiles.put(THIRD_ID, List.of(
				new TeacherSchoolLink(FRENCH_SCHOOL, SchoolCurricula.FRANCE, List.of("CE1"))));
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");
		service.transfer(TEACHER_ID, "teacher", maths.id(), "camille@example.com", "share");
		service.transfer(TEACHER_ID, "teacher", maths.id(), "luc@example.com", "share");

		service.transfer(OTHER_ID, "teacher", maths.id(), "prof@example.com", "move");

		assertThat(namesOf(TEACHER_ID, FRENCH_SCHOOL, "CE1")).containsExactly("Mathématiques");
		assertThat(lineOf(TEACHER_ID, maths.id()).sharedWith()).isEmpty();
		assertThat(namesOf(OTHER_ID, FRENCH_SCHOOL, "CE1")).isEmpty();
		assertThat(namesOf(THIRD_ID, FRENCH_SCHOOL, "CE1")).isEmpty();
		assertThat(subjects.accessCount(maths.id())).isEqualTo(1);
	}

	@Test
	void transferValidatesEmailModeAndColleague() {
		SubjectLine maths = service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Mathématiques");

		assertThatThrownBy(() -> service.transfer(TEACHER_ID, "teacher", maths.id(), "  ", "share"))
				.hasMessage(TeacherSubjectService.EMAIL_REQUIRED);
		assertThatThrownBy(() -> service.transfer(TEACHER_ID, "teacher", maths.id(), "a@b.com", "clone"))
				.hasMessage(TeacherSubjectService.MODE_REQUIRED);
		assertThatThrownBy(() -> service.transfer(
						TEACHER_ID, "teacher", maths.id(), "inconnu@example.com", "share"))
				.hasMessage(TeacherSubjectService.TEACHER_UNKNOWN);
		assertThatThrownBy(() -> service.transfer(
						TEACHER_ID, "teacher", maths.id(), "parent@example.com", "share"))
				.hasMessage(TeacherSubjectService.TEACHER_UNKNOWN);
		assertThatThrownBy(() -> service.transfer(
						TEACHER_ID, "teacher", maths.id(), "luc@example.com", "move"))
				.hasMessage(TeacherSubjectService.COLLEAGUE_CLASS_MISSING);
		assertThatThrownBy(() -> service.transfer(
						TEACHER_ID, "teacher", maths.id(), "prof@example.com", "share"))
				.hasMessage(TeacherSubjectService.ALREADY_HAS);

		service.transfer(TEACHER_ID, "teacher", maths.id(), "camille@example.com", "share");
		assertThatThrownBy(() -> service.transfer(
						TEACHER_ID, "teacher", maths.id(), "camille@example.com", "share"))
				.hasMessage(TeacherSubjectService.ALREADY_HAS);
		assertThat(subjects.accessCount(maths.id())).isEqualTo(2);
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
	}

	private static final class MemoryAccounts implements AuthUserAccountPort {

		private final Map<String, AuthUserAccount> byEmail = new HashMap<>();

		void seed(long id, String email, String displayName, boolean active) {
			byEmail.put(email.toLowerCase(java.util.Locale.ROOT),
					new AuthUserAccount(id, email, displayName, active));
		}

		@Override
		public Optional<AuthUserAccount> findByEmail(String email) {
			return Optional.ofNullable(byEmail.get(email));
		}

		@Override
		public Optional<AuthUserAccount> findByProviderSubject(String provider, String providerSubject) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<AuthUserSignupState> findSignupStateByEmail(String email) {
			throw new UnsupportedOperationException();
		}

		@Override
		public AuthUserAccount createWithIdentity(
				String email, String displayName, String provider, String providerSubject, String emailAtProvider) {
			throw new UnsupportedOperationException();
		}

		@Override
		public AuthUserAccount createWithPassword(String email, String displayName, String passwordHash) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void linkIdentity(long userId, String provider, String providerSubject, String emailAtProvider) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clearPasswordAfterVerifiedSocialLink(long userId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void updatePasswordHash(long userId, String passwordHash) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void bumpSecurityVersion(long userId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<SessionState> findSessionState(long userId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<AuthUserLoginCandidate> findLoginCandidateByEmail(String email) {
			throw new UnsupportedOperationException();
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
		private final Map<Long, String> displayNames = new HashMap<>();
		private long nextId = 1L;

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
					owned.add(row);
				}
			}
			return owned;
		}

		@Override
		public StoredSubject insert(long userId, long schoolId, String grade, String name, String nameKey) {
			StoredSubject saved = new StoredSubject(nextId++, userId, schoolId, grade, name, true);
			rows.add(saved);
			accessBySubject.put(saved.id(), new HashSet<>(Set.of(userId)));
			return saved;
		}

		@Override
		public Optional<StoredSubject> findOwned(long userId, long subjectId) {
			for (StoredSubject row : rows) {
				if (row.id() == subjectId
						&& !hidden.contains(row.id())
						&& accessBySubject.getOrDefault(row.id(), Set.of()).contains(userId)) {
					return Optional.of(row);
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
					return Optional.of(renamed);
				}
			}
			return Optional.empty();
		}

		@Override
		public boolean deactivate(long userId, long subjectId) {
			return replaceActive(userId, subjectId, false);
		}

		@Override
		public boolean reactivate(long userId, long subjectId) {
			return replaceActive(userId, subjectId, true);
		}

		@Override
		public boolean hide(long userId, long subjectId) {
			Set<Long> holders = accessBySubject.get(subjectId);
			if (holders == null || !holders.remove(userId) || hidden.contains(subjectId)) {
				return false;
			}
			if (holders.isEmpty()) {
				hidden.add(subjectId);
			}
			return true;
		}

		@Override
		public List<String> otherAccessNames(long subjectId, long viewerUserId) {
			List<String> names = new ArrayList<>();
			for (Long holder : accessBySubject.getOrDefault(subjectId, Set.of())) {
				if (holder != viewerUserId) {
					names.add(displayNames.getOrDefault(holder, ""));
				}
			}
			names.sort(java.text.Collator.getInstance(java.util.Locale.FRENCH));
			return List.copyOf(names);
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
		}

		@Override
		public void replaceAccessWith(long subjectId, long soleUserId) {
			accessBySubject.put(subjectId, new HashSet<>(Set.of(soleUserId)));
			for (int index = 0; index < rows.size(); index++) {
				StoredSubject row = rows.get(index);
				if (row.id() == subjectId) {
					rows.set(index, new StoredSubject(
							row.id(), soleUserId, row.schoolId(), row.grade(), row.name(), row.active()));
					return;
				}
			}
		}

		@Override
		public boolean hasAccess(long subjectId, long userId) {
			return accessBySubject.getOrDefault(subjectId, Set.of()).contains(userId);
		}

		private boolean replaceActive(long userId, long subjectId, boolean active) {
			for (int index = 0; index < rows.size(); index++) {
				StoredSubject row = rows.get(index);
				if (row.id() == subjectId
						&& !hidden.contains(row.id())
						&& accessBySubject.getOrDefault(row.id(), Set.of()).contains(userId)) {
					rows.set(index, new StoredSubject(
							row.id(), row.userId(), row.schoolId(), row.grade(), row.name(), active));
					return true;
				}
			}
			return false;
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

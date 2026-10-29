package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.TeacherProfileUseCase.TeacherSchoolCommand;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.FollowingChild;
import com.appv4.auth.application.port.out.ChildSubjectPort.SubjectChildLink;
import com.appv4.auth.application.port.out.EnrolledChildPort;
import com.appv4.auth.application.port.out.EnrolledChildPort.EnrolledChild;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherAccount;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherSchoolLink;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredSubject;
import com.appv4.auth.application.result.TeacherProfileView;
import com.appv4.auth.application.result.TeacherRemovalPreviewView;

class TeacherProfileServiceTest {

	private static final long TEACHER_ID = 4L;
	private static final long OTHER_ID = 10L;

	private MemoryProfile profiles;
	private List<CatalogSchool> catalog;
	private TeacherSubjectPort subjects;
	private EnrolledChildPort enrolled;
	private ChildSubjectPort links;
	private TeacherProfileService service;

	@BeforeEach
	void setUp() {
		profiles = new MemoryProfile();
		subjects = mock(TeacherSubjectPort.class);
		enrolled = mock(EnrolledChildPort.class);
		links = mock(ChildSubjectPort.class);
		profiles.seed(TEACHER_ID, "prof@example.com", "Ancien nom");
		profiles.seed(OTHER_ID, "autre@example.com", "Autre");
		catalog = new ArrayList<>();
		service = new TeacherProfileService(profiles, () -> catalog, subjects, enrolled, links);
	}

	@Test
	void contactStaysWhenTheSchoolPageOmitsItAndClearsWhenEmpty() {
		service.update(
				TEACHER_ID,
				"teacher",
				"Camille Martin",
				List.of(),
				"camille.contact@example.com",
				"0470000001",
				"fox");

		TeacherProfileView kept = service.update(TEACHER_ID, "teacher", "Camille Martin", List.of());

		assertThat(kept.contactEmail()).isEqualTo("camille.contact@example.com");
		assertThat(kept.phone()).isEqualTo("0470000001");
		assertThat(kept.avatarCode()).isEqualTo("fox");
		assertThat(kept.schools()).isEmpty();
		assertThat(profiles.schoolsOf(TEACHER_ID)).isEmpty();

		TeacherProfileView cleared = service.update(
				TEACHER_ID, "teacher", "Camille Martin", List.of(), "", "", "");
		assertThat(cleared.contactEmail()).isNull();
		assertThat(cleared.phone()).isNull();
		assertThat(cleared.avatarCode()).isNull();
		assertThat(profiles.account(TEACHER_ID).displayName()).isEqualTo("Camille Martin");
	}

	@Test
	void teacherRenamesAndKeepsEmailWhenNoSchoolIsOffered() {
		TeacherProfileView view = service.update(TEACHER_ID, "teacher", "  Professeur Martin  ", List.of());

		assertThat(view.displayName()).isEqualTo("Professeur Martin");
		assertThat(view.email()).isEqualTo("prof@example.com");
		assertThat(view.schools()).isEmpty();
		assertThat(view.availableSchools()).isEmpty();
		assertThat(profiles.account(TEACHER_ID).email()).isEqualTo("prof@example.com");
		assertThat(profiles.schoolsOf(OTHER_ID)).isEmpty();
		assertThat(profiles.account(OTHER_ID).displayName()).isEqualTo("Autre");
	}

	@Test
	void blankOrTooLongNameDoesNotReplaceThePreviousOne() {
		assertThatThrownBy(() -> service.update(TEACHER_ID, "teacher", "   ", List.of()))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherProfileService.NAME_REQUIRED);
		assertThatThrownBy(() -> service.update(TEACHER_ID, "teacher", null, List.of()))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherProfileService.NAME_REQUIRED);
		assertThatThrownBy(() -> service.update(TEACHER_ID, "teacher", "x".repeat(256), List.of()))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherProfileService.NAME_TOO_LONG);

		assertThat(profiles.account(TEACHER_ID).displayName()).isEqualTo("Ancien nom");
		assertThat(service.update(TEACHER_ID, "teacher", "x".repeat(255), List.of()).displayName())
				.hasSize(255);
	}

	@Test
	void parentOrPendingTeacherCannotOpenTheProfile() {
		assertDenied(() -> service.load(TEACHER_ID, "parent"));
		assertDenied(() -> service.update(TEACHER_ID, null, "Nouveau", List.of()));
		assertThat(profiles.account(TEACHER_ID).displayName()).isEqualTo("Ancien nom");
	}

	@Test
	void unknownSchoolIsRefusedWhileTheCatalogIsEmpty() {
		List<TeacherSchoolCommand> schools = List.of(
				new TeacherSchoolCommand(1L, SchoolCurricula.FRANCE, List.of("CE1")));

		assertThatThrownBy(() -> service.update(TEACHER_ID, "teacher", "Professeur Martin", schools))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherProfileService.SCHOOL_UNKNOWN);

		assertThat(profiles.account(TEACHER_ID).displayName()).isEqualTo("Ancien nom");
		assertThat(profiles.schoolsOf(TEACHER_ID)).isEmpty();
	}

	@Test
	void countryChoosesTheNamesAndTheSchoolChoosesTheLevels() {
		catalog.add(school(1L, "École française", SchoolCurricula.COUNTRY_FRANCE, "Primaire"));
		catalog.add(school(2L, "École belge", SchoolCurricula.COUNTRY_BELGIUM,
				"Maternelle ordinaire, Primaire ordinaire"));

		TeacherProfileView saved = service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, SchoolCurricula.BELGIUM_FR, List.of("CE2", "CE1")),
				new TeacherSchoolCommand(2L, null, List.of("4e primaire", "3e primaire", "1re maternelle"))));

		assertThat(saved.schools().get(0).curriculum()).isEqualTo(SchoolCurricula.FRANCE);
		assertThat(saved.schools().get(0).grades()).containsExactly("CE1", "CE2");
		assertThat(saved.availableSchools().get(0).grades()).contains("CP").doesNotContain("1re primaire", "6e");
		assertThat(saved.schools().get(1).curriculum()).isEqualTo(SchoolCurricula.BELGIUM_FR);
		assertThat(saved.schools().get(1).country()).isEqualTo(SchoolCurricula.COUNTRY_BELGIUM);
		assertThat(saved.schools().get(1).grades()).containsExactly("1re maternelle", "3e primaire", "4e primaire");
		assertThat(saved.availableSchools().get(1).grades())
				.contains("1re maternelle", "1re primaire")
				.doesNotContain("CP", "1re secondaire");
		assertThat(profiles.schoolsOf(OTHER_ID)).isEmpty();
	}

	@Test
	void wrongGradeDuplicateOrMissingYearDoesNotEraseThePreviousSchools() {
		catalog.add(school(1L, "École française", SchoolCurricula.COUNTRY_FRANCE, "Primaire"));
		catalog.add(school(2L, "École belge", SchoolCurricula.COUNTRY_BELGIUM, "Primaire"));
		service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of("CE1", "CE2"))));

		assertThatThrownBy(() -> service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(2L, null, List.of("CE1")))))
				.hasMessage(TeacherProfileService.GRADE_NOT_OFFERED);
		assertThatThrownBy(() -> service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of("1re primaire")))))
				.hasMessage(TeacherProfileService.GRADE_NOT_OFFERED);
		assertThatThrownBy(() -> service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(2L, null, List.of("1re secondaire")))))
				.hasMessage(TeacherProfileService.GRADE_NOT_OFFERED);
		assertThatThrownBy(() -> service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of()))))
				.hasMessage(TeacherProfileService.GRADE_REQUIRED);
		assertThatThrownBy(() -> service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of("CE1")),
				new TeacherSchoolCommand(1L, null, List.of("CE2")))))
				.hasMessage(TeacherProfileService.SCHOOL_DUPLICATE);
		assertThatThrownBy(() -> service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(null, null, List.of("CE1")))))
				.hasMessage(TeacherProfileService.SCHOOL_REQUIRED);
		assertThatThrownBy(() -> service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(9L, null, List.of("CE1")))))
				.hasMessage(TeacherProfileService.SCHOOL_UNKNOWN);

		assertThat(profiles.schoolsOf(TEACHER_ID)).containsExactly(
				new TeacherSchoolLink(1L, SchoolCurricula.FRANCE, List.of("CE1", "CE2")));
	}

	@Test
	void removingAGradeThenASchoolKeepsTheOtherSchool() {
		catalog.add(school(1L, "École française", SchoolCurricula.COUNTRY_FRANCE, "Primaire"));
		catalog.add(school(2L, "École belge", SchoolCurricula.COUNTRY_BELGIUM, "Primaire"));
		service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of("CE1")),
				new TeacherSchoolCommand(2L, null, List.of("3e primaire", "4e primaire"))));

		service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of("CE1")),
				new TeacherSchoolCommand(2L, null, List.of("3e primaire"))));
		assertThat(profiles.schoolsOf(TEACHER_ID)).containsExactly(
				new TeacherSchoolLink(1L, SchoolCurricula.FRANCE, List.of("CE1")),
				new TeacherSchoolLink(2L, SchoolCurricula.BELGIUM_FR, List.of("3e primaire")));

		TeacherProfileView left = service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of("CP"))));
		assertThat(left.schools()).containsExactly(new TeacherProfileView.AssignedSchool(
				1L,
				"École française",
				SchoolCurricula.COUNTRY_FRANCE,
				SchoolCurricula.FRANCE,
				List.of("CP")));
	}

	@Test
	void uncheckingAYearThenCheckingItAgainOnlyChangesTheProfile() {
		catalog.add(school(1L, "École française", SchoolCurricula.COUNTRY_FRANCE, "Primaire"));
		catalog.add(school(2L, "École belge", SchoolCurricula.COUNTRY_BELGIUM, "Primaire"));
		service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of("CE1", "CE2")),
				new TeacherSchoolCommand(2L, null, List.of("3e primaire"))));

		service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of("CE1"))));
		assertThat(profiles.schoolsOf(TEACHER_ID)).containsExactly(
				new TeacherSchoolLink(1L, SchoolCurricula.FRANCE, List.of("CE1")));

		assertThatThrownBy(() -> service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of("CE1")),
				new TeacherSchoolCommand(9L, null, List.of("CE1")))))
				.hasMessage(TeacherProfileService.SCHOOL_UNKNOWN);
		assertThat(profiles.schoolsOf(TEACHER_ID)).containsExactly(
				new TeacherSchoolLink(1L, SchoolCurricula.FRANCE, List.of("CE1")));

		service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of("CE1", "CE2")),
				new TeacherSchoolCommand(2L, null, List.of("3e primaire"))));
		assertThat(profiles.schoolsOf(TEACHER_ID)).containsExactly(
				new TeacherSchoolLink(1L, SchoolCurricula.FRANCE, List.of("CE1", "CE2")),
				new TeacherSchoolLink(2L, SchoolCurricula.BELGIUM_FR, List.of("3e primaire")));
	}

	private static CatalogSchool school(long id, String name, String country, String teachingTypes) {
		return new CatalogSchool(id, name, country, teachingTypes);
	}

	@Test
	void missingAccountIsNotFound() {
		assertThatThrownBy(() -> service.load(99L, "teacher"))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.NOT_FOUND);
	}

	@Test
	void previewListsStudentsSubjectsAndWhoFollowsThem() {
		catalog.add(school(1L, "École française", SchoolCurricula.COUNTRY_FRANCE, "Primaire"));
		profiles.replaceSchools(TEACHER_ID, List.of(
				new TeacherSchoolLink(1L, SchoolCurricula.FRANCE, List.of("CE2", "CE1"))));
		when(enrolled.findActiveInSchools(List.of(1L))).thenReturn(List.of(
				child(2L, "Léa", "Martin", SchoolCurricula.FRANCE, "CE1", true),
				child(3L, "Noah", "Bernard", SchoolCurricula.FRANCE, "CE1", true),
				child(4L, "Paul", "Durand", SchoolCurricula.FRANCE, "CE1", true),
				child(5L, "Inès", "Bernard", SchoolCurricula.FRANCE, "CE2", true),
				child(6L, "Masqué", "Absent", SchoolCurricula.FRANCE, "CE1", false),
				child(7L, "Belge", "Autre", SchoolCurricula.BELGIUM_FR, "CE1", true)));
		when(subjects.findByUser(TEACHER_ID)).thenReturn(List.of(
				new StoredSubject(10L, TEACHER_ID, 1L, "CE1", "Mathématiques", true),
				new StoredSubject(11L, TEACHER_ID, 1L, "CE1", "Français", true),
				new StoredSubject(12L, TEACHER_ID, 1L, "CE2", "Calcul", true),
				new StoredSubject(13L, TEACHER_ID, 2L, "CE1", "Autre école", true)));
		when(links.activeInClass(1L, "CE1")).thenReturn(List.of(
				new FollowingChild(3L, "Noah", "Bernard"),
				new FollowingChild(4L, "Paul", "Durand"),
				new FollowingChild(2L, "Léa", "Martin")));
		when(links.linksOfSubject(10L)).thenReturn(List.of(
				new SubjectChildLink(new FollowingChild(4L, "Paul", "Durand"), true),
				new SubjectChildLink(new FollowingChild(5L, "Inès", "Bernard"), false)));
		when(links.linksOfSubject(11L)).thenReturn(List.of(
				new SubjectChildLink(new FollowingChild(3L, "Noah", "Bernard"), true),
				new SubjectChildLink(new FollowingChild(4L, "Paul", "Durand"), true)));
		when(links.activeInClass(1L, "CE2")).thenReturn(List.of(new FollowingChild(5L, "Inès", "Bernard")));
		when(links.linksOfSubject(12L)).thenReturn(List.of());

		TeacherRemovalPreviewView year = service.removalPreview(TEACHER_ID, "teacher", 1L, "CE1");

		assertThat(year.schoolName()).isEqualTo("École française");
		assertThat(year.grade()).isEqualTo("CE1");
		assertThat(year.studentCount()).isEqualTo(3);
		assertThat(year.studentCountLabel()).isEqualTo("3 élèves");
		assertThat(year.students()).extracting(Student -> Student.lastName(), Student -> Student.firstName())
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("Bernard", "Noah"),
						org.assertj.core.groups.Tuple.tuple("Durand", "Paul"),
						org.assertj.core.groups.Tuple.tuple("Martin", "Léa"));
		assertThat(year.subjectCount()).isEqualTo(2);
		assertThat(year.subjectCountLabel()).isEqualTo("2 matières");
		assertThat(year.subjects()).extracting(Subject -> Subject.name()).containsExactly("Français", "Mathématiques");
		assertThat(year.subjects().get(0).followerNames()).containsExactly("Léa Martin");
		assertThat(year.subjects().get(0).followerWarning()).isEqualTo("1 enfant suit ce cours.");
		assertThat(year.subjects().get(1).followerNames())
				.containsExactly("Inès Bernard", "Léa Martin", "Noah Bernard");
		assertThat(year.students()).extracting(Student -> Student.firstName()).doesNotContain("Inès", "Masqué", "Belge");

		TeacherRemovalPreviewView school = service.removalPreview(TEACHER_ID, "teacher", 1L, "  ");

		assertThat(school.grade()).isNull();
		assertThat(school.studentCount()).isEqualTo(4);
		assertThat(school.studentCountLabel()).isEqualTo("4 élèves");
		assertThat(school.students()).extracting(Student -> Student.grade(), Student -> Student.firstName())
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("CE1", "Noah"),
						org.assertj.core.groups.Tuple.tuple("CE1", "Paul"),
						org.assertj.core.groups.Tuple.tuple("CE1", "Léa"),
						org.assertj.core.groups.Tuple.tuple("CE2", "Inès"));
		assertThat(school.subjectCount()).isEqualTo(3);
		assertThat(school.subjectCountLabel()).isEqualTo("3 matières");
		assertThat(school.subjects()).extracting(Subject -> Subject.name())
				.containsExactly("Français", "Mathématiques", "Calcul");
		assertThat(school.subjects().get(2).followerNames()).containsExactly("Inès Bernard");
		assertThat(profiles.schoolsOf(TEACHER_ID)).hasSize(1);
	}

	@Test
	void previewIsEmptyWhenNobodyIsInTheYearAndRefusesAnUnknownClass() {
		profiles.replaceSchools(TEACHER_ID, List.of(
				new TeacherSchoolLink(1L, SchoolCurricula.FRANCE, List.of("CM1"))));
		when(enrolled.findActiveInSchools(List.of(1L))).thenReturn(List.of());
		when(subjects.findByUser(TEACHER_ID)).thenReturn(List.of());

		TeacherRemovalPreviewView empty = service.removalPreview(TEACHER_ID, "teacher", 1L, "CM1");

		assertThat(empty.studentCount()).isZero();
		assertThat(empty.subjectCount()).isZero();
		assertThat(empty.studentCountLabel()).isEqualTo("0 élève");
		assertThat(empty.subjectCountLabel()).isEqualTo("0 matière");
		assertThat(empty.emptyStudentsLabel()).isEqualTo(TeacherStudentsService.EMPTY_GRADE);
		assertThat(empty.emptySubjectsLabel()).isEqualTo(TeacherProfileService.EMPTY_GRADE_SUBJECTS);
		assertThat(empty.students()).isEmpty();
		assertThat(empty.subjects()).isEmpty();

		assertThatThrownBy(() -> service.removalPreview(TEACHER_ID, "teacher", 9L, null))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherProfileService.SCHOOL_NOT_ON_PROFILE);
		assertThatThrownBy(() -> service.removalPreview(TEACHER_ID, "teacher", 1L, "CP"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherProfileService.GRADE_NOT_ON_PROFILE);
		assertDenied(() -> service.removalPreview(TEACHER_ID, "parent", 1L, "CM1"));
	}

	private static EnrolledChild child(
			long id, String firstName, String lastName, String curriculum, String grade, boolean active) {
		return new EnrolledChild(id, firstName, lastName, 8, curriculum, grade, 1L, null, false, active);
	}

	private static void assertDenied(Runnable call) {
		assertThatThrownBy(call::run)
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(SpaceHello.DENIED);
	}

	private static final class MemoryProfile implements TeacherProfilePort {

		private final Map<Long, TeacherAccount> accounts = new HashMap<>();
		private final Map<Long, List<TeacherSchoolLink>> schools = new HashMap<>();

		void seed(long userId, String email, String displayName) {
			accounts.put(userId, new TeacherAccount(email, displayName));
			schools.put(userId, List.of());
		}

		TeacherAccount account(long userId) {
			return accounts.get(userId);
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
			TeacherAccount current = accounts.get(userId);
			accounts.put(userId, new TeacherAccount(
					current.email(),
					displayName,
					current.contactEmail(),
					current.phone(),
					current.avatarCode()));
		}

		@Override
		public void updateContact(
				long userId,
				String displayName,
				String contactEmail,
				String phone,
				String avatarCode) {
			TeacherAccount current = accounts.get(userId);
			accounts.put(userId, new TeacherAccount(
					current.email(), displayName, contactEmail, phone, avatarCode));
		}

		@Override
		public List<TeacherSchoolLink> findSchools(long userId) {
			return schools.getOrDefault(userId, List.of());
		}

		@Override
		public void replaceSchools(long userId, List<TeacherSchoolLink> links) {
			schools.put(userId, List.copyOf(links));
		}

		@Override
		public List<Long> findUserIdsWithClass(long schoolId, String grade) {
			throw new UnsupportedOperationException();
		}
	}

}

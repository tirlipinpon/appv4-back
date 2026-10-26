package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.EnrolledChildPort;
import com.appv4.auth.application.port.out.EnrolledChildPort.EnrolledChild;
import com.appv4.auth.application.port.out.EnrolledChildPort.EnrolledPhoto;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherAccount;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherSchoolLink;
import com.appv4.auth.application.result.TeacherStudentsView;
import com.appv4.auth.application.result.TeacherStudentsView.GradeGroup;
import com.appv4.auth.application.result.TeacherStudentsView.StudentLine;

class TeacherStudentsServiceTest {

	private static final long TEACHER_ID = 4L;
	private static final long OTHER_ID = 10L;
	private static final long FRENCH_SCHOOL = 1L;
	private static final long BELGIAN_SCHOOL = 2L;

	private MemoryProfile profiles;
	private MemoryRoster roster;
	private TeacherStudentsService service;

	@BeforeEach
	void setUp() {
		profiles = new MemoryProfile();
		roster = new MemoryRoster();
		profiles.seed(TEACHER_ID, "prof@example.com", "Martin");
		profiles.seed(OTHER_ID, "autre@example.com", "Autre");
		profiles.put(TEACHER_ID, List.of(
				new TeacherSchoolLink(FRENCH_SCHOOL, SchoolCurricula.FRANCE, List.of("CE2", "CE1")),
				new TeacherSchoolLink(BELGIAN_SCHOOL, SchoolCurricula.BELGIUM_FR, List.of("3e primaire"))));
		List<CatalogSchool> catalog = List.of(
				new CatalogSchool(FRENCH_SCHOOL, "École du parc", "France", "Primaire"),
				new CatalogSchool(BELGIAN_SCHOOL, "École nouvelle", "Belgique", "Primaire"));
		service = new TeacherStudentsService(roster, profiles, () -> catalog);
		roster.add(child(1, "Léa", "Martin", 7, SchoolCurricula.FRANCE, "CE1", FRENCH_SCHOOL, "owl", false, true));
		roster.add(child(2, "Tom", "Martin", 7, SchoolCurricula.FRANCE, "CE1", FRENCH_SCHOOL, "fox", false, true));
		roster.add(child(3, "Noah", "Martin", 8, SchoolCurricula.FRANCE, "CM1", FRENCH_SCHOOL, "bear", false, true));
		roster.add(child(4, "Emma", "Durand", 7, SchoolCurricula.FRANCE, "CE1", BELGIAN_SCHOOL, "cat", false, true));
		roster.add(child(5, "Lina", "Dubois", 8, SchoolCurricula.BELGIUM_FR, "3e primaire", BELGIAN_SCHOOL, "panda", false, true));
		roster.add(child(6, "Nico", "Bernard", 6, SchoolCurricula.FRANCE, "CP", BELGIAN_SCHOOL, "frog", false, true));
		roster.add(child(7, "Sam", "Petit", 6, SchoolCurricula.FRANCE, "CE1", null, "owl", false, true));
		roster.add(child(8, "Inès", "Martin", 7, SchoolCurricula.FRANCE, "CE1", FRENCH_SCHOOL, "owl", false, false));
	}

	@Test
	void noSchoolOnTheProfileListsNobody() {
		profiles.put(TEACHER_ID, List.of());

		TeacherStudentsView view = service.list(TEACHER_ID, "teacher");

		assertThat(view.schools()).isEmpty();
		assertThat(view.noClassMessage()).isEqualTo(TeacherStudentsService.NO_CLASS);
		assertThat(roster.reads).isZero();
		assertThat(roster.snapshot()).hasSize(8);
	}

	@Test
	void listsOnlyTheMatchingClassAndKeepsAnEmptyGrade() {
		TeacherStudentsView view = service.list(TEACHER_ID, "teacher");

		assertThat(view.schools()).extracting(school -> school.schoolName())
				.containsExactly("École du parc", "École nouvelle");
		GradeGroup ce1 = grade(view, FRENCH_SCHOOL, "CE1");
		assertThat(ce1.countLabel()).isEqualTo("2 élèves");
		assertThat(ce1.students()).extracting(StudentLine::firstName).containsExactly("Léa", "Tom");
		assertThat(ce1.students()).extracting(StudentLine::lastName).containsExactly("Martin", "Martin");
		assertThat(ce1.students()).extracting(StudentLine::age).containsExactly(7, 7);
		GradeGroup ce2 = grade(view, FRENCH_SCHOOL, "CE2");
		assertThat(ce2.countLabel()).isEqualTo("0 élève");
		assertThat(ce2.emptyMessage()).isEqualTo(TeacherStudentsService.EMPTY_GRADE);
		assertThat(ce2.students()).isEmpty();
		assertThat(grades(view, FRENCH_SCHOOL)).containsExactly("CE1", "CE2");
		assertThat(names(view)).doesNotContain("Noah", "Emma", "Nico", "Sam", "Inès");
	}

	@Test
	void addingAGradeShowsTheChildAlreadyEnrolledThere() {
		profiles.put(TEACHER_ID, List.of(
				new TeacherSchoolLink(FRENCH_SCHOOL, SchoolCurricula.FRANCE, List.of("CE1", "CM1"))));

		TeacherStudentsView view = service.list(TEACHER_ID, "teacher");

		assertThat(grade(view, FRENCH_SCHOOL, "CM1").students())
				.extracting(StudentLine::firstName)
				.containsExactly("Noah");
		assertThat(grade(view, FRENCH_SCHOOL, "CM1").students().get(0).age()).isEqualTo(8);
		assertThat(grade(view, FRENCH_SCHOOL, "CE1").countLabel()).isEqualTo("2 élèves");
	}

	@Test
	void belgianGradeDoesNotShowAFrenchChildOfTheSameSchool() {
		TeacherStudentsView view = service.list(TEACHER_ID, "teacher");

		GradeGroup belgian = grade(view, BELGIAN_SCHOOL, "3e primaire");
		assertThat(belgian.countLabel()).isEqualTo("1 élève");
		assertThat(belgian.students()).extracting(StudentLine::firstName).containsExactly("Lina");
		assertThat(view.schools().get(1).curriculumLabel()).contains("Belgique");
		assertThat(grades(view, FRENCH_SCHOOL)).doesNotContain("1re primaire");
		assertThat(grades(view, BELGIAN_SCHOOL)).doesNotContain("CP");
		assertThat(names(view)).doesNotContain("Nico", "Emma");
	}

	@Test
	void removingAGradeHidesTheGroupAndLeavesTheChildRecord() {
		List<EnrolledChild> before = roster.snapshot();
		profiles.put(TEACHER_ID, List.of(
				new TeacherSchoolLink(FRENCH_SCHOOL, SchoolCurricula.FRANCE, List.of("CE2"))));

		TeacherStudentsView view = service.list(TEACHER_ID, "teacher");

		assertThat(grades(view, FRENCH_SCHOOL)).containsExactly("CE2");
		assertThat(names(view)).doesNotContain("Léa", "Tom");
		assertThat(roster.snapshot()).isEqualTo(before);
	}

	@Test
	void anotherTeacherSeesOnlyHisOwnGrades() {
		profiles.put(TEACHER_ID, List.of(
				new TeacherSchoolLink(FRENCH_SCHOOL, SchoolCurricula.FRANCE, List.of("CE2"))));
		profiles.put(OTHER_ID, List.of(
				new TeacherSchoolLink(FRENCH_SCHOOL, SchoolCurricula.FRANCE, List.of("CE1"))));

		TeacherStudentsView view = service.list(OTHER_ID, "teacher");

		assertThat(view.schools()).hasSize(1);
		assertThat(grade(view, FRENCH_SCHOOL, "CE1").students())
				.extracting(StudentLine::firstName)
				.containsExactly("Léa", "Tom");
		assertThat(grades(view, FRENCH_SCHOOL)).doesNotContain("CE2");
		assertThat(profiles.schoolsOf(TEACHER_ID).get(0).grades()).containsExactly("CE2");
	}

	@Test
	void twoChildrenWithTheSameNameStayTwoLines() {
		roster.add(child(9, "Léa", "Martin", 7, SchoolCurricula.FRANCE, "CE1", FRENCH_SCHOOL, "cat", false, true));

		GradeGroup ce1 = grade(service.list(TEACHER_ID, "teacher"), FRENCH_SCHOOL, "CE1");

		assertThat(ce1.countLabel()).isEqualTo("3 élèves");
		assertThat(ce1.students()).extracting(StudentLine::id).containsExactly(1L, 9L, 2L);
	}

	@Test
	void photoIsReadableOnlyForAChildInClass() {
		roster.photo(1L, new byte[] {1, 2}, "image/png");

		var photo = service.photo(TEACHER_ID, "teacher", 1L);

		assertThat(photo.bytes()).containsExactly(1, 2);
		assertThat(photo.contentType()).isEqualTo("image/png");
		assertThatThrownBy(() -> service.photo(TEACHER_ID, "teacher", 4L))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherStudentsService.NOT_IN_CLASS)
				.extracting(error -> ((AuthDomainException) error).getErrorCode())
				.isEqualTo(AuthErrorCode.NOT_FOUND);
		assertThatThrownBy(() -> service.photo(TEACHER_ID, "teacher", 2L))
				.hasMessage(TeacherStudentsService.NO_PHOTO);
	}

	@Test
	void parentCannotListOrOpenAPhoto() {
		assertThatThrownBy(() -> service.list(TEACHER_ID, "parent"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(SpaceHello.DENIED);
		assertThatThrownBy(() -> service.photo(TEACHER_ID, "parent", 1L))
				.hasMessage(SpaceHello.DENIED);
		assertThat(roster.reads).isZero();
		assertThat(roster.photoReads).isZero();
	}

	private static GradeGroup grade(TeacherStudentsView view, long schoolId, String grade) {
		return view.schools().stream()
				.filter(school -> school.schoolId() == schoolId)
				.findFirst()
				.orElseThrow()
				.grades()
				.stream()
				.filter(group -> grade.equals(group.grade()))
				.findFirst()
				.orElseThrow();
	}

	private static List<String> grades(TeacherStudentsView view, long schoolId) {
		return view.schools().stream()
				.filter(school -> school.schoolId() == schoolId)
				.findFirst()
				.orElseThrow()
				.grades()
				.stream()
				.map(GradeGroup::grade)
				.toList();
	}

	private static List<String> names(TeacherStudentsView view) {
		List<String> found = new ArrayList<>();
		for (var school : view.schools()) {
			for (GradeGroup group : school.grades()) {
				for (StudentLine student : group.students()) {
					found.add(student.firstName());
				}
			}
		}
		return found;
	}

	private static EnrolledChild child(
			long id,
			String firstName,
			String lastName,
			int age,
			String curriculum,
			String grade,
			Long schoolId,
			String avatar,
			boolean hasPhoto,
			boolean active) {
		return new EnrolledChild(id, firstName, lastName, age, curriculum, grade, schoolId, avatar, hasPhoto, active);
	}

	private static final class MemoryProfile implements TeacherProfilePort {

		private final Map<Long, TeacherAccount> accounts = new HashMap<>();
		private final Map<Long, List<TeacherSchoolLink>> schools = new HashMap<>();

		void seed(long userId, String email, String displayName) {
			accounts.put(userId, new TeacherAccount(email, displayName));
		}

		void put(long userId, List<TeacherSchoolLink> links) {
			schools.put(userId, links);
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
			throw new AssertionError("le nom du professeur ne change pas");
		}

		@Override
		public List<TeacherSchoolLink> findSchools(long userId) {
			return schools.getOrDefault(userId, List.of());
		}

		@Override
		public void replaceSchools(long userId, List<TeacherSchoolLink> next) {
			throw new AssertionError("le profil ne change pas depuis cette lecture");
		}
	}

	private static final class MemoryRoster implements EnrolledChildPort {

		private final List<EnrolledChild> children = new ArrayList<>();
		private final Map<Long, EnrolledPhoto> photos = new HashMap<>();
		private int reads;
		private int photoReads;

		void add(EnrolledChild child) {
			children.add(child);
		}

		void photo(long id, byte[] bytes, String type) {
			photos.put(id, new EnrolledPhoto(bytes, type));
		}

		List<EnrolledChild> snapshot() {
			return List.copyOf(children);
		}

		@Override
		public List<EnrolledChild> findActiveInSchools(Collection<Long> schoolIds) {
			reads++;
			return List.copyOf(children);
		}

		@Override
		public Optional<EnrolledChild> findActive(long childId) {
			return children.stream().filter(child -> child.id() == childId).findFirst();
		}

		@Override
		public Optional<EnrolledPhoto> findPhoto(long childId) {
			photoReads++;
			return Optional.ofNullable(photos.get(childId));
		}
	}
}

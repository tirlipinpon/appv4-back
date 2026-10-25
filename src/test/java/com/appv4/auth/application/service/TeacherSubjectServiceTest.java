package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
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
	private static final long FRENCH_SCHOOL = 1L;
	private static final long BELGIAN_SCHOOL = 2L;

	private MemoryProfile profiles;
	private MemorySubjects subjects;
	private TeacherSubjectService service;

	@BeforeEach
	void setUp() {
		profiles = new MemoryProfile();
		subjects = new MemorySubjects();
		profiles.seed(TEACHER_ID, "prof@example.com", "Martin");
		profiles.seed(OTHER_ID, "autre@example.com", "Autre");
		profiles.put(TEACHER_ID, List.of(
				new TeacherSchoolLink(FRENCH_SCHOOL, SchoolCurricula.FRANCE, List.of("CE1", "CE2")),
				new TeacherSchoolLink(BELGIAN_SCHOOL, SchoolCurricula.BELGIUM_FR, List.of("3e primaire"))));
		profiles.put(OTHER_ID, List.of(
				new TeacherSchoolLink(FRENCH_SCHOOL, SchoolCurricula.FRANCE, List.of("CE1"))));
		List<CatalogSchool> catalog = List.of(
				new CatalogSchool(FRENCH_SCHOOL, "École française", "France", "Primaire"),
				new CatalogSchool(BELGIAN_SCHOOL, "École belge", "Belgique", "Primaire"));
		service = new TeacherSubjectService(subjects, profiles, () -> catalog);
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
		assertThat(maths.id()).isNotEqualTo(mathsCe2.id());
		assertThat(subjects.all()).hasSize(3);
	}

	@Test
	void sameNameIsAllowedInAnotherSchoolButNotTwiceInTheSameClass() {
		service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "Lecture");

		assertThatThrownBy(() -> service.create(TEACHER_ID, "teacher", FRENCH_SCHOOL, "CE1", "   "))
				.hasMessage(TeacherSubjectService.NAME_REQUIRED);
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
		assertThat(profiles.schoolsOf(TEACHER_ID).get(0).grades()).containsExactly("CE1", "CE2");
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

	private static final class MemorySubjects implements TeacherSubjectPort {

		private final List<StoredSubject> rows = new ArrayList<>();
		private long nextId = 1L;

		List<StoredSubject> all() {
			return List.copyOf(rows);
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
				if (row.userId() == userId) {
					owned.add(row);
				}
			}
			return owned;
		}

		@Override
		public StoredSubject insert(long userId, long schoolId, String grade, String name, String nameKey) {
			StoredSubject saved = new StoredSubject(nextId++, userId, schoolId, grade, name);
			rows.add(saved);
			return saved;
		}

		@Override
		public Optional<StoredSubject> findOwned(long userId, long subjectId) {
			for (StoredSubject row : rows) {
				if (row.id() == subjectId && row.userId() == userId) {
					return Optional.of(row);
				}
			}
			return Optional.empty();
		}

		@Override
		public Optional<StoredSubject> rename(long userId, long subjectId, String name, String nameKey) {
			for (int index = 0; index < rows.size(); index++) {
				StoredSubject row = rows.get(index);
				if (row.id() == subjectId && row.userId() == userId) {
					StoredSubject renamed = new StoredSubject(
							row.id(), row.userId(), row.schoolId(), row.grade(), name);
					rows.set(index, renamed);
					return Optional.of(renamed);
				}
			}
			return Optional.empty();
		}

		@Override
		public boolean delete(long userId, long subjectId) {
			return rows.removeIf(row -> row.userId() == userId && row.id() == subjectId);
		}

		@Override
		public boolean nameTaken(long userId, long schoolId, String grade, String nameKey, Long exceptId) {
			for (StoredSubject row : rows) {
				if (row.userId() != userId || row.schoolId() != schoolId || !grade.equals(row.grade())) {
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

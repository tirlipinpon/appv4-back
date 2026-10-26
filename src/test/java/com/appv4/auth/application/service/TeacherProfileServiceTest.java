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
import com.appv4.auth.application.port.in.TeacherProfileUseCase.TeacherSchoolCommand;
import com.appv4.auth.application.port.out.SchoolCatalogPort.CatalogSchool;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherAccount;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherSchoolLink;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort.AccessHolder;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredDeactivateNotice;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredSubject;
import com.appv4.auth.application.result.TeacherProfileView;

class TeacherProfileServiceTest {

	private static final long TEACHER_ID = 4L;
	private static final long OTHER_ID = 10L;

	private MemoryProfile profiles;
	private MemorySubjects subjects;
	private List<CatalogSchool> catalog;
	private TeacherProfileService service;

	@BeforeEach
	void setUp() {
		profiles = new MemoryProfile();
		subjects = new MemorySubjects();
		profiles.seed(TEACHER_ID, "prof@example.com", "Ancien nom");
		profiles.seed(OTHER_ID, "autre@example.com", "Autre");
		catalog = new ArrayList<>();
		service = new TeacherProfileService(profiles, () -> catalog, subjects);
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
	void uncheckingAGradeDeletesItsSubjectsAndLeavesTheOthers() {
		catalog.add(school(1L, "École française", SchoolCurricula.COUNTRY_FRANCE, "Primaire"));
		catalog.add(school(2L, "École belge", SchoolCurricula.COUNTRY_BELGIUM, "Primaire"));
		service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of("CE1", "CE2")),
				new TeacherSchoolCommand(2L, null, List.of("3e primaire"))));
		subjects.seed(new StoredSubject(1L, TEACHER_ID, 1L, "CE1", "Lecture", true));
		subjects.seed(new StoredSubject(2L, TEACHER_ID, 1L, "CE2", "Mathématiques", true));
		subjects.seed(new StoredSubject(3L, TEACHER_ID, 2L, "3e primaire", "Éveil", true));
		subjects.seed(new StoredSubject(4L, OTHER_ID, 1L, "CE1", "Calcul", true));

		service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of("CE1")),
				new TeacherSchoolCommand(2L, null, List.of("3e primaire"))));

		assertThat(subjects.remaining(TEACHER_ID)).extracting(StoredSubject::id).containsExactly(1L, 3L);
		assertThat(subjects.remaining(OTHER_ID)).extracting(StoredSubject::name).containsExactly("Calcul");

		assertThatThrownBy(() -> service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of("CE1")),
				new TeacherSchoolCommand(9L, null, List.of("CE1")))))
				.hasMessage(TeacherProfileService.SCHOOL_UNKNOWN);
		assertThat(subjects.remaining(TEACHER_ID)).extracting(StoredSubject::id).containsExactly(1L, 3L);

		service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of("CE1"))));
		assertThat(subjects.remaining(TEACHER_ID)).extracting(StoredSubject::id).containsExactly(1L);
		assertThat(subjects.remaining(OTHER_ID)).extracting(StoredSubject::name).containsExactly("Calcul");

		service.update(TEACHER_ID, "teacher", "Ancien nom", List.of(
				new TeacherSchoolCommand(1L, null, List.of("CE1", "CE2"))));
		assertThat(subjects.remaining(TEACHER_ID)).extracting(StoredSubject::name).containsExactly("Lecture");
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
			accounts.put(userId, new TeacherAccount(current.email(), displayName));
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

	private static final class MemorySubjects implements TeacherSubjectPort {

		private final List<StoredSubject> rows = new ArrayList<>();

		void seed(StoredSubject subject) {
			rows.add(subject);
		}

		List<StoredSubject> remaining(long userId) {
			List<StoredSubject> kept = new ArrayList<>();
			for (StoredSubject row : rows) {
				if (row.userId() == userId) {
					kept.add(row);
				}
			}
			return kept;
		}

		@Override
		public List<StoredSubject> findByUser(long userId) {
			return remaining(userId);
		}

		@Override
		public StoredSubject insert(long userId, long schoolId, String grade, String name, String nameKey) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<StoredSubject> findOwned(long userId, long subjectId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<StoredSubject> rename(
				long userId, long subjectId, String name, String nameKey) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hide(long userId, long subjectId) {
			return rows.removeIf(row -> row.userId() == userId && row.id() == subjectId);
		}

		@Override
		public boolean deactivateAccesses(long actorUserId, long subjectId, List<Long> userIds) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean reactivate(long userId, long subjectId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Long> accessUserIds(long subjectId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<AccessHolder> otherAccessHolders(long subjectId, long viewerUserId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<String> otherAccessNames(long subjectId, long viewerUserId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void insertDeactivateNotices(
				long subjectId, long fromUserId, List<Long> toUserIds, List<Long> deactivatedUserIds) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<StoredDeactivateNotice> findOpenDeactivateNoticesTo(long toUserId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean dismissDeactivateNotice(long noticeId, long toUserId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<String> allAccessNames(long subjectId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addAccess(long subjectId, long userId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void replaceAccessWith(long subjectId, long soleUserId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasAccess(long subjectId, long userId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean nameTaken(long schoolId, String grade, String nameKey, Long exceptId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<StoredSubject> findVisible(long subjectId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public StoredTransferRequest insertTransferRequest(long subjectId, long fromUserId, long toUserId, String mode) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasPendingTransfer(long subjectId, long toUserId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<StoredTransferRequest> findPendingTransfersTo(long toUserId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<StoredTransferRequest> findPendingTransferForTarget(long requestId, long toUserId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void markTransferAccepted(long requestId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void markTransferRejected(long requestId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void cancelPendingTransfers(long subjectId, Long exceptRequestId) {
			throw new UnsupportedOperationException();
		}
	}
}

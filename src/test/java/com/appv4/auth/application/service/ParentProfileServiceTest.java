package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.TeacherProfilePort;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherAccount;
import com.appv4.auth.application.port.out.TeacherProfilePort.TeacherSchoolLink;
import com.appv4.auth.application.result.ParentProfileView;

class ParentProfileServiceTest {

	private MemoryProfile profiles;
	private ParentProfileService service;

	@BeforeEach
	void setUp() {
		profiles = new MemoryProfile();
		profiles.seed(4L, "parent@example.com", "Ancien");
		profiles.put(4L, List.of(new TeacherSchoolLink(3L, "BELGIUM_FR", List.of("1re primaire"))));
		service = new ParentProfileService(profiles);
	}

	@Test
	void parentSavesContactWithoutTouchingSchools() {
		ParentProfileView view = service.update(
				4L,
				"parent",
				" Parent Contact ",
				"contact.parent@example.com",
				"+32 470 00 00 00",
				"owl");

		assertThat(view.email()).isEqualTo("parent@example.com");
		assertThat(view.displayName()).isEqualTo("Parent Contact");
		assertThat(view.contactEmail()).isEqualTo("contact.parent@example.com");
		assertThat(view.phone()).isEqualTo("+32 470 00 00 00");
		assertThat(view.avatarCode()).isEqualTo("owl");
		assertThat(view.avatarChoices()).extracting(choice -> choice.label()).contains("Hibou");
		assertThat(profiles.schoolsOf(4L)).hasSize(1);
	}

	@Test
	void emptyContactClearsTheColumnsAndKeepsTheName() {
		service.update(4L, "parent", "Parent Contact", "a@b.c", "0470000001", "fox");

		ParentProfileView cleared = service.update(4L, "parent", "Parent Contact", "", "", "");

		assertThat(cleared.displayName()).isEqualTo("Parent Contact");
		assertThat(cleared.contactEmail()).isNull();
		assertThat(cleared.phone()).isNull();
		assertThat(cleared.avatarCode()).isNull();
		assertThat(profiles.schoolsOf(4L)).hasSize(1);
	}

	@Test
	void refusalsWriteNothing() {
		assertThatThrownBy(() -> service.update(4L, "parent", "   ", "a@b.c", "0470000001", "owl"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AccountContact.NAME_REQUIRED);
		assertThatThrownBy(() -> service.update(4L, "parent", "Nom", "pas-un-mail", "0470000001", "owl"))
				.hasMessage(AccountContact.EMAIL_INVALID);
		assertThatThrownBy(() -> service.update(4L, "parent", "Nom", "a@b.c", "abc", "owl"))
				.hasMessage(AccountContact.PHONE_INVALID);
		assertThatThrownBy(() -> service.update(4L, "parent", "Nom", "a@b.c", "0470000001", "dragon"))
				.hasMessage(AccountContact.AVATAR_UNKNOWN);
		assertThatThrownBy(() -> service.update(4L, "teacher", "Nom", "", "", ""))
				.hasMessage(SpaceHello.DENIED);

		assertThat(profiles.account(4L).displayName()).isEqualTo("Ancien");
		assertThat(profiles.account(4L).contactEmail()).isNull();
		assertThat(profiles.schoolsOf(4L)).hasSize(1);
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
			throw new UnsupportedOperationException();
		}

		@Override
		public void updateContact(
				long userId, String displayName, String contactEmail, String phone, String avatarCode) {
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
			throw new AssertionError("la fiche parent ne touche pas aux écoles");
		}

		@Override
		public List<Long> findUserIdsWithClass(long schoolId, String grade) {
			return List.of();
		}
	}
}
